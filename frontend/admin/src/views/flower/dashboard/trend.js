function storeDate() {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(new Date());
  const part = type => parts.find(item => item.type === type).value;
  return `${part('year')}-${part('month')}-${part('day')}`;
}

const nonNegative = value => Number.isFinite(Number(value)) ? Math.max(0, Number(value)) : 0;

const DAY_MS = 24 * 60 * 60 * 1000;

function calendarDate(value) {
  const date = new Date(`${value}T00:00:00Z`);
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)
      || !Number.isFinite(date.getTime()) || date.toISOString().slice(0, 10) !== value) {
    throw new RangeError('Expected a valid ISO calendar date');
  }
  return date;
}

export function buildDashboardTrend(records, endDate = storeDate(), startDate) {
  const end = calendarDate(endDate);
  const start = startDate === undefined ? new Date(end.getTime() - 6 * DAY_MS) : calendarDate(startDate);
  const length = (end.getTime() - start.getTime()) / DAY_MS + 1;
  if (length < 1 || length > 31) throw new RangeError('Sales trend range must contain 1 to 31 days');
  const source = Array.isArray(records) ? records : [];
  return Array.from({ length }, (_, index) => {
    const key = new Date(start.getTime() + index * DAY_MS).toISOString().slice(0, 10);
    const label = key.slice(5);
    const record = source.find(item => item?.label === key) || source.find(item => item?.label === label);
    return { date: key, label, amount: nonNegative(record?.amount), orderCount: nonNegative(record?.orderCount) };
  });
}
