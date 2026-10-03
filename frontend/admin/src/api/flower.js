import request from '@/utils/request';

export function getFlowerProduct(productId) {
  return request({ url: `/admin/flower/product/${productId}`, method: 'get' });
}

export function saveFlowerProduct(productId, data) {
  return request({ url: `/admin/flower/product/${productId}`, method: 'post', data });
}

export function getFlowerProductList(params) {
  return request({ url: '/admin/flower/product/list', method: 'get', params });
}
export function getFlowerFilterOptions() {
  return request({ url: '/admin/flower/product/filters', method: 'get' });
}
export function getFlowerProductTabs(params) {
  return request({ url: '/admin/flower/product/tabs', method: 'get', params });
}
export function exportFlowerProducts(params) {
  return request({ url: '/admin/export/excel/flower/product', method: 'get', params });
}

export const getDeliveryZones = (params) => request({ url: '/admin/flower/delivery/zone/list', method: 'get', params });
export const saveDeliveryZone = (data) => request({ url: '/admin/flower/delivery/zone', method: 'post', data });
export const setDeliveryZoneEnabled = (id, enabled) =>
  request({ url: `/admin/flower/delivery/zone/${id}/enabled`, method: 'post', params: { enabled } });
export const getDeliverySlots = (params) => request({ url: '/admin/flower/delivery/slot/list', method: 'get', params });
export const saveDeliverySlot = (data) => request({ url: '/admin/flower/delivery/slot', method: 'post', data });
export const setDeliverySlotEnabled = (id, enabled) =>
  request({ url: `/admin/flower/delivery/slot/${id}/enabled`, method: 'post', params: { enabled } });
export const getFlowerDashboard = () => request({ url: '/admin/flower/dashboard', method: 'get' });
export const getFlowerOrders = (params) => request({ url: '/admin/flower/order/list', method: 'get', params });
export const getFlowerOrderDetail = (orderNo) => request({ url: `/admin/flower/order/${orderNo}`, method: 'get' });
export const transitionFlowerOrder = (orderNo, data) =>
  request({ url: `/admin/flower/order/${orderNo}/transition`, method: 'post', data });

export const getFlowerMaterials = () => request({ url: '/admin/flower/material/list', method: 'get' });
export const saveFlowerMaterial = (data) => request({ url: '/admin/flower/material', method: 'post', data });
export const getFlowerBatches = (params) => request({ url: '/admin/flower/material/batches', method: 'get', params });
export const receiveFlowerBatch = (data) => request({ url: '/admin/flower/material/batches', method: 'post', data });
export const adjustFlowerBatch = (batchId, currentQty) => request({ url: `/admin/flower/material/batches/${batchId}/adjust`, method: 'post', params: { currentQty } });
export const getFlowerRecipe = (productId) => request({ url: `/admin/flower/material/recipe/${productId}`, method: 'get' });
export const saveFlowerRecipe = (productId, data) => request({ url: `/admin/flower/material/recipe/${productId}`, method: 'post', data });

export const getFlowerCareArticles = (params) => request({ url: '/admin/flower/care/articles', method: 'get', params });
export const getFlowerCareArticle = (id) => request({ url: `/admin/flower/care/articles/${id}`, method: 'get' });
export const saveFlowerCareArticle = (data) => request({ url: '/admin/flower/care/articles', method: 'post', data });
export const updateFlowerCareArticle = (id, data) => request({ url: `/admin/flower/care/articles/${id}`, method: 'post', data });
