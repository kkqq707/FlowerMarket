// The local flower seed data uses backend URLs for images also shipped in the app.
// Keep remote uploads unchanged; only these known local assets can be served offline.
function localFlowerImage(value) {
  const match = value.match(/^(?:http:\/\/(?:localhost|127\.0\.0\.1)(?::\d+)?\/|\/?)(?:crmebimage\/public\/flower\/)(product-(?:0[1-9]|1[0-2])\.jpg|logo\.png)(?:[?#].*)?$/i);
  if (!match) return value;
  return match[1].toLowerCase() === 'logo.png'
    ? '/static/images/huajianji/logo.png'
    : `/static/images/flower-products/${match[1].toLowerCase()}`;
}

export function normalizeFlowerImages(value) {
  if (Array.isArray(value)) return value.map(normalizeFlowerImages);
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, normalizeFlowerImages(item)]));
  }
  if (typeof value !== 'string') return value;
  // Product galleries may be returned as JSON-encoded arrays rather than arrays.
  if (value.trim().startsWith('[')) {
    try {
      const parsed = JSON.parse(value);
      if (Array.isArray(parsed)) return JSON.stringify(normalizeFlowerImages(parsed));
    } catch { /* Non-JSON text is returned unchanged. */ }
  }
  return localFlowerImage(value);
}
