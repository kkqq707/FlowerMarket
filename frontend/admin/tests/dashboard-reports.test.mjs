import assert from 'node:assert/strict';
import test from 'node:test';

const helpers = () => import('../src/views/flower/dashboard/report-query.js');
const deferred = () => { let resolve, reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no; }); return { promise, resolve, reject }; };

test('preset date ranges include today and cross month/year boundaries', async () => {
  const { reportDateRange } = await helpers();
  assert.deepEqual(reportDateRange(7, '2026-11-02'), { startDate:'2026-10-27', endDate:'2026-11-02' });
  assert.deepEqual(reportDateRange(30, '2027-01-02'), { startDate:'2026-12-04', endDate:'2027-01-02' });
});

test('custom dates require valid calendar dates, ordered inclusive range of up to 31 days, and no future dates', async () => {
  const { validateReportRange } = await helpers();
  const validate = (startDate, endDate) => validateReportRange({startDate,endDate}, '2026-11-02');
  assert.equal(validate('2026-10-03','2026-11-02'), '');
  assert.equal(validate('2026-11-02','2026-11-02'), '');
  assert.ok(validate('2026-10-02','2026-11-02'));
  assert.ok(validate('2026-11-02','2026-10-31'));
  assert.ok(validate('2026-02-30','2026-03-01'));
  assert.ok(validate('2026-11-02','2026-11-03'));
  assert.ok(validate('', ''));
});

test('independent reports send their own dates and sort without changing the other report', async () => {
  const { createDashboardReport } = await helpers();
  const calls=[];
  const sales=createDashboardReport(async params => {calls.push({...params});return [{amount:436}];}, {startDate:'2026-10-27',endDate:'2026-11-02'});
  const hot=createDashboardReport(async params => {calls.push({...params});return [{productId:1}];}, {startDate:'2026-10-27',endDate:'2026-11-02',sortBy:'quantity'});
  await sales.run({startDate:'2026-11-01',endDate:'2026-11-01'});
  await hot.run({...hot.state.params,sortBy:'amount'});
  assert.deepEqual(calls,[{startDate:'2026-11-01',endDate:'2026-11-01'},{startDate:'2026-10-27',endDate:'2026-11-02',sortBy:'amount'}]);
  assert.equal(sales.state.data[0].amount,436);
  assert.equal(hot.state.data[0].productId,1);
  assert.equal(sales.state.params.startDate,'2026-11-01');
});

test('a slow previous filter response cannot overwrite the latest results or loading state', async () => {
  const { createDashboardReport } = await helpers();
  const first=deferred(), second=deferred();let n=0;
  const report=createDashboardReport(() => (++n===1?first.promise:second.promise), {});
  const old=report.run({startDate:'2026-10-27'});
  const latest=report.run({startDate:'2026-11-01'});
  second.resolve([{amount:846}]);await latest;
  first.resolve([{amount:436}]);await old;
  assert.equal(report.state.data[0].amount,846);
  assert.equal(report.state.params.startDate,'2026-11-01');
  assert.equal(report.state.loading,false);
});

test('failed requests clear stale results, show an error, and can retry the same filters', async () => {
  const { createDashboardReport } = await helpers();let n=0;
  const report=createDashboardReport(async () => {if(++n===1)throw new Error('network');return [{quantity:2}];}, {sortBy:'amount'});
  report.state.data=[{quantity:100}];
  assert.equal(await report.run(),false);
  assert.deepEqual(Array.from(report.state.data),[]);
  assert.ok(report.state.error);
  assert.equal(report.state.loading,false);
  await report.run();
  assert.equal(report.state.error,'');
  assert.equal(report.state.data[0].quantity,2);
});
