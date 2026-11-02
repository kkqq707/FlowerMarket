import assert from 'node:assert/strict';
import test from 'node:test';

async function helpers() {
  return import('../src/views/flower/dashboard/trend.js');
}

test('shows a full seven-day timeline when sales only occurred on the last day', async () => {
  const { buildDashboardTrend } = await helpers();
  const records = [{ label: '11-02', amount: '5020.00', orderCount: 12 }];
  const days = buildDashboardTrend(records, '2026-11-02');
  assert.deepEqual(days.map(day => day.label), ['10-27','10-28','10-29','10-30','10-31','11-01','11-02']);
  assert.deepEqual(days.map(day => day.amount), [0,0,0,0,0,0,5020]);
  assert.deepEqual(days.map(day => day.orderCount), [0,0,0,0,0,0,12]);
  assert.equal(records.length, 1);
});

test('sorts dates across the year boundary and excludes data outside the visible week', async () => {
  const { buildDashboardTrend } = await helpers();
  const days = buildDashboardTrend([
    { label:'01-02', amount:200, orderCount:2 },
    { label:'12-31', amount:150, orderCount:1 },
    { label:'12-20', amount:999, orderCount:9 },
  ], '2027-01-02');
  assert.equal(days[0].date, '2026-12-27');
  assert.equal(days[6].date, '2027-01-02');
  assert.equal(days[4].amount, 150);
  assert.equal(days.reduce((total, day) => total + day.amount, 0), 350);
});

test('a one-day selection includes exactly that date and its full values', async () => {
  const { buildDashboardTrend } = await helpers();
  assert.deepEqual(buildDashboardTrend([{ label:'2026-11-02', amount:'5020.12', orderCount:12 }], '2026-11-02', '2026-11-02'), [
    { date:'2026-11-02', label:'11-02', amount:5020.12, orderCount:12 },
  ]);
});

test('a thirty-day selection fills missing dates including month transitions', async () => {
  const { buildDashboardTrend } = await helpers();
  const days = buildDashboardTrend([{ label:'2026-10-31', amount:89.99, orderCount:1 }], '2026-11-02', '2026-10-04');
  assert.equal(days.length, 30);
  assert.equal(days[0].date, '2026-10-04');
  assert.equal(days[29].date, '2026-11-02');
  assert.deepEqual(days[27], { date:'2026-10-31', label:'10-31', amount:89.99, orderCount:1 });
  assert.equal(days.filter(day => day.amount === 0).length, 29);
});

test('a thirty-one-day selection includes both calendar boundaries', async () => {
  const { buildDashboardTrend } = await helpers();
  const days = buildDashboardTrend([], '2026-01-31', '2026-01-01');
  assert.equal(days.length, 31);
  assert.equal(days[0].date, '2026-01-01');
  assert.equal(days[30].date, '2026-01-31');
});

test('an explicit range crosses the year boundary without using another year’s ISO data', async () => {
  const { buildDashboardTrend } = await helpers();
  const days = buildDashboardTrend([
    { label:'2025-12-31', amount:999, orderCount:9 },
    { label:'2026-12-31', amount:150, orderCount:1 },
    { label:'01-02', amount:200, orderCount:2 },
  ], '2027-01-03', '2026-12-30');
  assert.deepEqual(days.map(day => day.date), ['2026-12-30','2026-12-31','2027-01-01','2027-01-02','2027-01-03']);
  assert.deepEqual(days.map(day => day.amount), [0,150,0,200,0]);
});

test('exact ISO dates take priority over legacy month-day labels without mutating input', async () => {
  const { buildDashboardTrend } = await helpers();
  const records = Object.freeze([
    Object.freeze({ label:'11-02', amount:1, orderCount:1 }),
    Object.freeze({ label:'2026-11-02', amount:'5020.12', orderCount:12 }),
  ]);
  const days = buildDashboardTrend(records, '2026-11-02', '2026-11-02');
  assert.equal(days[0].amount, 5020.12);
  assert.equal(days[0].orderCount, 12);
  assert.equal(records[1].amount, '5020.12');
});

test('a leap-day range advances by real calendar dates', async () => {
  const { buildDashboardTrend } = await helpers();
  const days = buildDashboardTrend([], '2028-03-01', '2028-02-28');
  assert.deepEqual(days.map(day => day.label), ['02-28','02-29','03-01']);
});

test('invalid, reversed and oversized explicit ranges cannot silently show another interval', async () => {
  const { buildDashboardTrend } = await helpers();
  for (const [start, end] of [['2026-02-30','2026-03-01'], ['2026-11-02','2026-11-01'], ['2026-01-01','2026-02-01']]) {
    assert.throws(() => buildDashboardTrend([], end, start), RangeError);
  }
});

test('empty or invalid data produces real zero values', async () => {
  const { buildDashboardTrend } = await helpers();
  const days = buildDashboardTrend(null, '2026-11-02');
  assert.equal(days.length, 7);
  assert.ok(days.every(day => day.amount === 0 && day.orderCount === 0));
  const invalid = buildDashboardTrend([{ label:'11-02', amount:NaN, orderCount:-1 }], '2026-11-02', '2026-11-02');
  assert.deepEqual(invalid[0], { date:'2026-11-02', label:'11-02', amount:0, orderCount:0 });
});
