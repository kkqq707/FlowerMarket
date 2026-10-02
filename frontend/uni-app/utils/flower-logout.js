import { getLogout } from '@/api/user.js';

export function confirmFlowerLogout(appStore) {
	uni.showModal({
		title: '退出登录',
		content: '确定退出当前账号吗？',
		success: async ({ confirm }) => {
			if (!confirm) return;
			uni.showLoading({ title: '退出中...' });
			try {
				await getLogout();
			} catch (_) {
				// 即使服务端会话已失效，也要清除本机登录状态。
			} finally {
				appStore.LOGOUT();
				uni.hideLoading();
				uni.reLaunch({ url: '/pages/index/index' });
			}
		}
	});
}
