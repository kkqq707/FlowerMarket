const FLOWER_IMAGE_FALLBACK =
  'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 400"%3E%3Crect width="400" height="400" fill="%23F7F4EC"/%3E%3Cpath d="M200 104c-32 0-58 26-58 58 0 18 8 34 21 45-24 7-41 29-41 56 0 33 27 60 60 60 7 0 13-1 18-3v38h8v-38c6 2 12 3 18 3 33 0 60-27 60-60 0-27-18-49-42-56 14-11 22-27 22-45 0-32-26-58-58-58z" fill="%2335402F" opacity=".16"/%3E%3Cpath d="M200 159v159" stroke="%2335402F" stroke-width="6" stroke-linecap="round" opacity=".4"/%3E%3C/svg%3E'.replace(/"/g, '%22').replace(/ /g, '%20');

function firstText(...values) {
  const value = values.find((item) => typeof item === 'string' && item.trim());
  return value ? value.trim() : '';
}

function toNonNegativeNumber(value) {
  const number = Number(value);
  return Number.isFinite(number) && number > 0 ? number : 0;
}

function formatFlowerPrice(value) {
  return splitFlowerPrice(value).text;
}

function splitFlowerPrice(value) {
  const [integer, fraction] = toNonNegativeNumber(value).toFixed(2).split('.');
  const decimal = `.${fraction}`;

  return {
    symbol: '¥',
    integer,
    decimal,
    text: `¥${integer}${decimal}`,
  };
}

function formatFlowerSales(value) {
  return `已售 ${Math.floor(toNonNegativeNumber(value))} 件`;
}

function flowerDetailMetrics(product = {}, selectedSku = {}) {
  return {
    stock: Math.floor(toNonNegativeNumber(selectedSku.stock ?? product.stock)),
    sales: Math.floor(toNonNegativeNumber(product.sales)) + Math.floor(toNonNegativeNumber(product.ficti)),
  };
}

function resolveFlowerImage(value) {
  if (Array.isArray(value)) return resolveFlowerImage(value[0]);
  return typeof value === 'string' && value.trim() ? value.trim() : FLOWER_IMAGE_FALLBACK;
}

function buildFlowerProductView(product = {}) {
  const title = firstText(product.store_name, product.storeName, product.name, product.title) || '花礼';
  const priceParts = splitFlowerPrice(product.price ?? product.ot_price);

  return {
    id: product.id || product.product_id || '',
    title,
    englishTitle: firstText(product.store_name_en, product.englishName, product.subtitle),
    materials: firstText(product.flower_materials, product.flowerMaterials, product.materials),
    priceParts,
    priceText: priceParts.text,
    salesText: formatFlowerSales(product.sales ?? product.sales_num),
    image: {
      src: resolveFlowerImage(product.image || product.cover_image || product.slider_image),
      alt: title,
    },
  };
}

export {
  FLOWER_IMAGE_FALLBACK,
  buildFlowerProductView,
  formatFlowerPrice,
  formatFlowerSales,
  flowerDetailMetrics,
  resolveFlowerImage,
  splitFlowerPrice,
};

export default {
  FLOWER_IMAGE_FALLBACK,
  buildFlowerProductView,
  formatFlowerPrice,
  formatFlowerSales,
  flowerDetailMetrics,
  resolveFlowerImage,
  splitFlowerPrice,
};
