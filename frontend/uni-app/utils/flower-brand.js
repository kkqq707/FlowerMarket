export const FLOWER_BRAND = Object.freeze({
  name: '花间集',
  logo: '/static/images/huajianji/logo.png',
  wordmark: '/static/images/huajianji/wordmark.png',
});

export function goBackFromFlowerLogin(navigation) {
  if (navigation.pageCount() > 1) navigation.back();
  else navigation.home();
}
