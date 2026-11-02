import { reactive } from 'vue';

export function reportToday() {
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone:'Asia/Shanghai', year:'numeric', month:'2-digit', day:'2-digit' }).formatToParts(new Date());
  return ['year','month','day'].map(type => parts.find(part => part.type === type).value).join('-');
}

export function reportDateRange(days = 7, endDate = reportToday()) {
  const start = new Date(`${endDate}T00:00:00Z`);
  start.setUTCDate(start.getUTCDate() - days + 1);
  return { startDate:start.toISOString().slice(0,10), endDate };
}

function validDate(value) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value || '')) return false;
  const date = new Date(`${value}T00:00:00Z`);
  return Number.isFinite(date.getTime()) && date.toISOString().slice(0,10) === value;
}

export function validateReportRange(range, today = reportToday()) {
  if (!validDate(range?.startDate) || !validDate(range?.endDate)) return '请选择有效的开始和结束日期';
  if (range.startDate > range.endDate) return '开始日期不能晚于结束日期';
  if (range.endDate > today) return '结束日期不能晚于今天';
  if ((Date.parse(range.endDate) - Date.parse(range.startDate)) / 86400000 >= 31) return '最多查询 31 天，请缩小日期范围';
  return '';
}

// Each panel owns its request sequence; late responses cannot replace a newer selection.
export function createDashboardReport(fetcher, initialParams) {
  const state = reactive({ params:{...initialParams}, data:[], loading:false, error:'' });
  let sequence = 0;
  async function run(params = state.params) {
    const request = ++sequence;
    state.params = {...params};
    state.loading = true;
    state.error = '';
    try {
      const rows = await fetcher({...state.params});
      if (request !== sequence) return false;
      state.data = Array.isArray(rows) ? rows : [];
      return true;
    } catch {
      if (request === sequence) {
        state.data = [];
        state.error = '数据加载失败，请重试';
      }
      return false;
    } finally {
      if (request === sequence) state.loading = false;
    }
  }
  return {state, run};
}
