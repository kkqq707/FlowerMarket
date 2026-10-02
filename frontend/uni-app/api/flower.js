import request from '@/utils/request.js';

export function getFlowerDeliveryQuote(data) {
  return request.get('flower/delivery/quote', data);
}

export function getFlowerDeliverySlots(data) {
  return request.get('flower/delivery/slots', data);
}

export function mockFlowerPay(orderNo) {
  return request.post(`flower/pay/mock/${orderNo}`, {});
}

export function getFlowerOrderDetail(orderNo) {
  return request.get(`flower/order/${orderNo}`);
}

export function confirmFlowerDelivery(orderNo) {
  return request.post(`flower/order/${orderNo}/confirm`, {});
}

export function getFlowerCareArticles(data) {
  return request.get('flower/care/articles', data);
}

export function getFlowerCareArticle(id) {
  return request.get(`flower/care/articles/${id}`);
}

