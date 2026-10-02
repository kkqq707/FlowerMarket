import Layout from '@/layout';

const flowerRouter = {
  path: '/flower',
  component: Layout,
  redirect: '/flower/dashboard',
  name: 'FlowerShop',
  alwaysShow: true,
  meta: { title: '花店经营', icon: 'dashboard' },
  children: [
    { path: 'dashboard', component: () => import('@/views/flower/dashboard/index.vue'), name: 'FlowerDashboard', meta: { title: '经营看板', icon: 'dashboard', isAffix: true } },
    { path: 'orders', component: () => import('@/views/flower/order/index.vue'), name: 'FlowerOrders', meta: { title: '花店订单' } },
    { path: 'delivery', component: () => import('@/views/flower/delivery/index.vue'), name: 'FlowerDelivery', meta: { title: '配送管理' } },
    { path: 'material', component: () => import('@/views/flower/material/index.vue'), name: 'FlowerMaterial', meta: { title: '花材库存' } },
    { path: 'care', component: () => import('@/views/flower/care/index.vue'), name: 'FlowerCare', meta: { title: '养花指南' } },
    { path: 'orders/:orderNo', component: () => import('@/views/flower/order/detail.vue'), name: 'FlowerOrderDetail', hidden: true, meta: { title: '订单详情', activeMenu: '/flower/orders' } },
  ],
};

export default flowerRouter;
