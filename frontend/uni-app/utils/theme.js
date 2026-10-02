import { getThemeInfo } from "@/api/api.js";

const FLOWER_THEME_COLOR = "#35402F";
const FLOWER_ACCENT_COLOR = "#ED3C2D";

function parseJson(value, fallback) {
  if (!value) return fallback;
  if (typeof value === "object") return value;
  try {
    return JSON.parse(value);
  } catch (e) {
    return fallback;
  }
}

function pickValue(data, camelKey, snakeKey, fallback) {
  if (data[camelKey] !== undefined && data[camelKey] !== null) return data[camelKey];
  if (data[snakeKey] !== undefined && data[snakeKey] !== null) return data[snakeKey];
  return fallback;
}

function normalizeThemeColor(data = {}) {
  let value = parseJson(data, data || {});
  if (!value || typeof value !== "object") return {};
  if (value.themeData || value.theme_data) {
    value = {
      ...value,
      ...parseJson(value.themeData || value.theme_data, {}),
    };
  }
  if (value.value !== undefined) {
    value = {
      ...value,
      ...parseJson(value.value, {}),
    };
  }
  return {
    ...value,
    theme_color: pickValue(value, "themeColor", "theme_color", value.theme_color),
    gradient_color: pickValue(value, "gradientColor", "gradient_color", value.gradient_color),
    sub_color: pickValue(value, "subColor", "sub_color", value.sub_color),
    light_color: pickValue(value, "lightColor", "light_color", value.light_color),
  };
}

function buildLegacyThemeData(data = {}) {
  const themeColor = FLOWER_THEME_COLOR;
  const gradientColor = FLOWER_THEME_COLOR;
  const subColor = FLOWER_THEME_COLOR;
  const lightColor = hexToRgba(FLOWER_THEME_COLOR, 0.1);
  return {
    theme: "theme1",
    theme_color: themeColor,
    gradient_color: gradientColor,
    sub_color: subColor,
    light_color: lightColor,
    main_color: themeColor,
    price_color: FLOWER_ACCENT_COLOR,
    main_gradient: FLOWER_THEME_COLOR,
    second_gradient: FLOWER_THEME_COLOR,
    coupons_border: `1px solid ${themeColor}`,
    coupons_light_color: lightColor,
  };
}

export function createFlowerTheme() {
  return {
    data: buildLegacyThemeData(),
    style: buildThemeStyle(),
  };
}

function setLegacyThemeData(themeData) {
  const app = typeof getApp === "function" ? getApp() : null;
  uni.setStorageSync("theme", themeData.theme);
  uni.setStorageSync("themeData", themeData);
  uni.setStorageSync("themeColor", themeData.theme_color);
  if (app && app.globalData) {
    app.globalData.theme = themeData.theme;
    app.globalData.themeData = themeData;
    app.globalData.themeColor = themeData.theme_color;
  }
  uni.$emit("themeData", themeData);
}

function buildThemeStyle(data = {}) {
  const themeColor = FLOWER_THEME_COLOR;
  const gradientColor = FLOWER_THEME_COLOR;
  const subColor = FLOWER_THEME_COLOR;
  const lightColor = hexToRgba(FLOWER_THEME_COLOR, 0.1);
  const themeColorRgba = hexToRgba(themeColor, 1);

  return `
      --view-theme: ${themeColorRgba};
      --view-theme-16: ${themeColor};
      --view-priceColor: ${FLOWER_ACCENT_COLOR};
      --view-minorColor: ${subColor};
      --view-minorColorT: ${lightColor};
      --view-bntColor: ${subColor};
      --view-op-ten: ${hexToRgba(themeColor, 0.1)};
      --view-main-start: ${gradientColor};
      --view-main-over: ${themeColor};
      --view-op-point-four: ${hexToRgba(themeColor, 0.04)};
      --view-op-point-eight: ${hexToRgba(themeColor, 0.8)};
      --view-linear: linear-gradient(180deg, ${hexToRgba(
        themeColor,
        0.2,
      )} 0%, rgba(255,255,255,0) 100%);
      --view-gradient: ${gradientColor};
      --view-main-gradient: ${themeColorRgba};
      --view-second-gradient: ${themeColorRgba};
      --view-index-gradient: linear-gradient(270deg, ${themeColorRgba} 0%, #F5F5F5 100%);
      --view-seckill-gradient: linear-gradient(270deg, ${themeColorRgba} 0%, rgba(255,255,255,0.2) 72%, rgba(255,255,255,1) 100%);
      --view-coupons-gradient: ${themeColorRgba};
      --view-bargain-gradient: ${themeColorRgba};
      --view-login-gradient: ${themeColorRgba};
      --view-bragin-border: ${themeColorRgba} transparent transparent;
    `;
}

export function applyH5ThemeStyle(styleText = "") {
  // #ifdef H5
  if (typeof document === "undefined" || !styleText) return;
  styleText
    .split(";")
    .map((item) => item.trim())
    .filter(Boolean)
    .forEach((item) => {
      const index = item.indexOf(":");
      if (index === -1) return;
      document.documentElement.style.setProperty(
        item.slice(0, index).trim(),
        item.slice(index + 1).trim(),
      );
    });
  // #endif
}

/**
 * 处理颜色
 */
export function hexToRgba(hex, alpha) {
  if (!hex) return "";
  let sColor = hex.toLowerCase();
  const reg = /^#([0-9a-fA-f]{3}|[0-9a-fA-f]{6})$/;
  if (sColor && reg.test(sColor)) {
    if (sColor.length === 4) {
      let sColorNew = "#";
      for (let i = 1; i < 4; i += 1) {
        sColorNew += sColor.slice(i, i + 1).concat(sColor.slice(i, i + 1));
      }
      sColor = sColorNew;
    }
    //处理六位的颜色值
    let sColorChange = [];
    for (let i = 1; i < 7; i += 2) {
      sColorChange.push(parseInt("0x" + sColor.slice(i, i + 2)));
    }
    return "rgba(" + sColorChange.join(",") + "," + alpha + ")";
  }
  return sColor;
}

/**
 * 设置主题颜色
 * @param {Object} data 主题数据
 */
export function setThemeColor(data) {
  return applyFlowerTheme();
}

export function applyFlowerTheme() {
  const flowerTheme = createFlowerTheme();
  setLegacyThemeData(flowerTheme.data);
  uni.setStorageSync("viewColor", flowerTheme.style);
  applyH5ThemeStyle(flowerTheme.style);
  uni.$emit("ok", flowerTheme.style);
  return flowerTheme.data;
}

/**
 * 获取并应用主题
 * @param {Number|String} themeId 主题ID
 */
export function applyTheme(themeId) {
  let data = {};
  if (themeId) data.theme_id = themeId;
  return getThemeInfo("theme", data).then((res) => {
    uni.setStorageSync("is_diy", 1);
    uni.$emit("is_diy", 1);
    return setThemeColor(normalizeThemeColor(res.data));
  }).catch((error) => {
    applyFlowerTheme();
    throw error;
  });
}
