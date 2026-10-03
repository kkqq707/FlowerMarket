// +----------------------------------------------------------------------
// | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
// +----------------------------------------------------------------------
// | Copyright (c) 2016~2026 https://www.crmeb.com All rights reserved.
// +----------------------------------------------------------------------
// | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
// +----------------------------------------------------------------------
// | Author: CRMEB Team <admin@crmeb.com>
// +----------------------------------------------------------------------


import request from "@/utils/request.js";
import flowerView from '@/utils/flower-product-view.js';

// The flower endpoints extend the existing catalog; cart and SKU APIs remain unchanged.
export function getFlowerProducts(data = {}) {
	return request.get('flower/product/list', data, { noAuth: true });
}
export function getFlowerDetail(id) {
	return request.get('flower/product/' + id, {}, { noAuth: true });
}
export function getFlowerFilterOptions() {
	return request.get('flower/product/filters', {}, { noAuth: true });
}
export function flowerFiltersFromResponse(response) {
	const data = response?.data || {};
	return flowerFilters.map(group => group.key === 'budget' ? group : {
		...group, values: ['全部', ...new Set((Array.isArray(data[group.key]) ? data[group.key] : []).filter(Boolean))]
	});
}
export function getFlowerStoreConfig() {
	return request.get('flower/store/config', {}, { noAuth: true });
}
export function flowerProductsFromResponse(response) {
	const data = response && response.data;
	const list = Array.isArray(data) ? data : ((data && data.list) || []);
	return list.map(item => ({ ...item, store_name: item.store_name || item.storeName || item.name,
		flower_materials: item.flower_materials || item.flowerMaterials }));
}
// Explicit local presentation fixtures: never sent to a cart or order endpoint.
export const flowerDemoProducts = [
	{ id: 'demo-01', store_name: '晨露 · 白绿洋桔梗', subtitle: 'MORNING DEW', price: 188, flowerMaterials: '白色洋桔梗 · 季节叶材', occasion: '日常', recipient: '朋友,自己', colorSeries: '白绿', flowerLanguage: '把清晨的温柔，轻轻带回家。' },
	{ id: 'demo-02', store_name: '暖阳 · 向日葵花束', subtitle: 'HELLO SUNSHINE', price: 168, flowerMaterials: '向日葵 · 尤加利 · 季节配花', occasion: '日常', recipient: '朋友,长辈', colorSeries: '暖黄', flowerLanguage: '愿每一个普通日子，都朝着阳光生长。' },
	{ id: 'demo-03', store_name: '香颂 · 香槟玫瑰', subtitle: 'SOFT CHAMPAGNE', price: 228, flowerMaterials: '香槟玫瑰 · 尤加利', occasion: '日常', recipient: '恋人,朋友', colorSeries: '暖黄', flowerLanguage: '安静的喜欢，也值得被认真表达。' },
	{ id: 'demo-04', store_name: '怦然 · 粉色玫瑰', subtitle: 'HEART FLUTTER', price: 268, flowerMaterials: '粉玫瑰 · 季节配叶', occasion: '生日', recipient: '恋人', colorSeries: '粉色', flowerLanguage: '为怦然心动的你，留住今天的甜。' },
	{ id: 'demo-05', store_name: '欢颜 · 缤纷混搭', subtitle: 'JOY IN BLOOM', price: 298, flowerMaterials: '洋桔梗 · 玫瑰 · 季节混合花材', occasion: '生日', recipient: '朋友,家人', colorSeries: '多彩', flowerLanguage: '把所有明亮的颜色，都送给你的新一岁。' },
	{ id: 'demo-06', store_name: '奶油诗 · 郁金香', subtitle: 'CREAM POEM', price: 238, flowerMaterials: '奶油白郁金香 · 季节叶材', occasion: '生日', recipient: '恋人,朋友', colorSeries: '白绿', flowerLanguage: '柔软而坚定，像一句只说给你的诗。' },
	{ id: 'demo-07', store_name: '挚爱 · 红玫瑰', subtitle: 'TRUE LOVE', price: 398, flowerMaterials: '红玫瑰 · 季节配叶', occasion: '纪念日', recipient: '恋人', colorSeries: '红色', flowerLanguage: '把未说尽的爱，交给盛放的玫瑰。' },
	{ id: 'demo-08', store_name: '蝶梦 · 紫色蝴蝶兰', subtitle: 'PURPLE REVERIE', price: 328, flowerMaterials: '紫色蝴蝶兰 · 绿叶', occasion: '纪念日', recipient: '恋人,长辈', colorSeries: '紫色', flowerLanguage: '优雅长久，珍藏每一次温柔相伴。' },
	{ id: 'demo-09', store_name: '月光 · 白色百合', subtitle: 'MOONLIGHT LILY', price: 358, flowerMaterials: '白百合 · 季节叶材', occasion: '纪念日', recipient: '恋人,家人', colorSeries: '白绿', flowerLanguage: '纯净的心意，在月光里静静绽放。' },
	{ id: 'demo-10', store_name: '橙意 · 暖色花束', subtitle: 'WARM WISHES', price: 288, flowerMaterials: '橙色玫瑰 · 季节混合花材', occasion: '祝贺', recipient: '朋友,家人', colorSeries: '橙色', flowerLanguage: '以明亮花色，祝愿新的旅程热烈展开。' },
	{ id: 'demo-11', store_name: '晴空 · 蓝白花篮', subtitle: 'CLEAR SKY', price: 468, flowerMaterials: '蓝色飞燕草 · 白玫瑰 · 绣球', occasion: '祝贺', recipient: '朋友,长辈', colorSeries: '蓝白', flowerLanguage: '晴朗从容，愿好消息如约而至。' },
	{ id: 'demo-12', store_name: '盛放 · 开业桌花', subtitle: 'A GRAND OPENING', price: 598, flowerMaterials: '红掌 · 红玫瑰 · 季节叶材', occasion: '祝贺', recipient: '朋友,长辈', colorSeries: '红色', flowerLanguage: '花开正盛，祝事业启程、前路长红。' },
].map((item, index) => ({
	...item,
	image: `/static/images/flower-products/product-${String(index + 1).padStart(2, '0')}.jpg`,
	flower_materials: item.flowerMaterials,
	sales: 0,
	demo: true,
}));

export function flowerCartRecommendationsFromResponse(response, limit = 6) {
	const realFlowers = flowerProductsFromResponse(response);
	return realFlowers.slice(0, limit);
}

export const flowerFilters = [
	{ key: 'occasion', label: '场景', values: ['全部', '日常', '生日', '纪念日', '祝贺', '探望'] },
	{ key: 'recipient', label: '送给', values: ['全部', '恋人', '朋友', '家人', '长辈', '自己'] },
	{ key: 'colorSeries', label: '色系', values: ['全部', '白绿', '粉色', '红色', '暖黄', '紫色', '橙色', '蓝白', '多彩'] },
	{ key: 'budget', label: '预算', values: ['全部', '200元以内', '200–300元', '300–500元', '500元以上'] },
];
export const flowerBudgets = [{}, { minPrice: 0, maxPrice: 199.99 }, { minPrice: 200, maxPrice: 299.99 }, { minPrice: 300, maxPrice: 500 }, { minPrice: 500.01 }];
export function filterFlowerDemo(query = {}) {
	return flowerDemoProducts.filter(item => (!query.keyword || item.store_name.includes(query.keyword) || item.flowerMaterials.includes(query.keyword)) &&
		['occasion', 'recipient', 'colorSeries'].every(key => !query[key] || item[key].split(',').includes(query[key])) &&
		(query.minPrice === undefined || item.price >= query.minPrice) && (query.maxPrice === undefined || item.price <= query.maxPrice));
}

/**
 * 获取产品详情
 * @param int id
 * 
 */
export function getProductDetail(id, type) {
	const query = type === undefined || type === null || type === '' ? '' : '?type=' + type;
	return request.get('product/detail/' + id + query, {}, {
		noAuth: true
	});
}

/**
 * 产品分享二维码 推广员
 * @param int id
 */
// #ifndef MP
export function getProductCode(id) {
	return request.get('product/code/' + id, {});
}
// #endif
// #ifdef MP
export function getProductCode(id) {
	return request.get('product/code/' + id, {
		user_type: 'routine'
	});
}
// #endif

/**
 * 添加收藏
 * @param int id
 * @param string category product=普通产品,product_seckill=秒杀产品
 */
export function collectAdd(id, category) {
	return request.post('collect/add', {
		id: id,
		'category': category === undefined ? 'product' : category
	});
}

/**
 * 取消收藏产品
 * @param int id
 */
export function collectDel(proId) {
	return request.post(`collect/cancel/${proId}`);
}


/**
 * 删除收藏产品
 * @param string id
 */
export function collectDelete(ids) {
	return request.post(`collect/delete`,ids);
}

/**
 * 购车添加
 * 
 */
export function postCartAdd(data) {
	return request.post('cart/save', data, {});
}

/**
 * 获取分类列表
 * 
 */
export function getCategoryList() {
	return request.get('category', {}, {
		noAuth: true
	});
}

/**
 * 获取产品列表
 * @param object data
 */
export function getProductslist(data) {
	return request.get('products', data, {
		noAuth: true
	});
}

/**
 * 商品列表(个别分类模型使用)
 * @param object data
 */
export function productList(data) {
	return request.get('product/list', data, {
		noAuth: true
	});
}

/**
 * 获取推荐产品
 * 
 */
export function getProductHot(page, limit) {
	return request.get("product/hot", {
		page: page === undefined ? 1 : page,
		limit: limit === undefined ? 4 : limit
	}, {
		noAuth: true
	});
}
/**
 * 批量收藏
 * 
 * @param object id  产品编号 join(',') 切割成字符串
 * @param string category 
 */
export function collectAll(id, category) {
	return request.post('collect/all', {
		id: id,
		category: category === undefined ? 'product' : category
	});
}

/**
 * 首页产品的轮播图和产品信息
 * @param int type 
 * 
 */
export function getGroomList(type, data) {
	return request.get('index/product/' + type, data, {
		noAuth: true
	});
}

/**
 * 获取收藏列表
 * @param object data
 */
export function getCollectUserList(data) {
	return request.get('collect/user', data)
}

/**
 * 获取产品评论
 * @param int id
 * @param object data
 * 
 */
export function getReplyList(id, data) {
	return request.get('reply/list/' + id, data,{
		noAuth: true
	})
}

/**
 * 产品评价数量和好评度
 * @param int id
 */
export function getReplyConfig(id) {
	return request.get('reply/config/' + id,{},{
		noAuth: true
	});
}

/**
 * 获取搜索关键字获取
 * 
 */
export function getSearchKeyword() {
	return request.get('search/keyword', {}, {
		noAuth: true
	});
}

/**
 * 门店列表
 * @returns {*}
 */
export function storeListApi(data) {
	return request.post("store/list", data, {}, 1);
}


/**
 * 优品推荐
 * @param object data
 */
export function getProductGood() {
	return request.get('product/good',{},{
		noAuth: true
	});
}

/**
 * 详情页产品评论
 * @param int id
 * @param object data
 * 
 */
export function getReplyProduct(id) {
	return request.get('reply/product/' + id,{}, {
		noAuth: true
	})
}

/**
 * 获取商品的sku
 * @returns {*}
 */
export function getAttr(id) {
  return request.get("product/sku/detail/" + id);
}
/**
 * 购车添加、减少、修改，主题组件暂按标准版接口保留
 */
export function postCartNum(data) {
	return request.post('v2/set_cart_num', data);
}
/**
 * 根据商品id集合查询对应商品
 * @param {string} ids
 */
export function productByidsApi(ids) {
	return request.get(`product/byids/${ids}`,{}, {
		noAuth: true
	});
}
