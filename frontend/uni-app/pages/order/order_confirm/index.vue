<template>
	<view :data-theme="theme" :style="colorStyle">
		<!-- #ifdef MP -->
		<view class="mp-nav-bar" :style="{ height: mpNavHeight + 'rpx' }">
			<view class="mp-nav-bar__content">
				<view class="mp-nav-bar__back iconfont icon-fanhui2" @tap="goBack"></view>
				<view class="mp-nav-bar__title">{{ navTitle }}</view>
				<view class="mp-nav-bar__placeholder"></view>
			</view>
		</view>
		<view class="mp-nav-placeholder" :style="{ height: mpNavHeight + 'rpx' }"></view>
		<!-- #endif -->

		<view class='order-submission'>
			<view class="allAddress">
				<view v-if="store_self_mention" class="delivery-modes">
					<button class="delivery-mode" :class="{ 'is-active': shippingType === 0 }" @tap="addressType(0)">配送到家</button>
					<button class="delivery-mode" :class="{ 'is-active': shippingType === 1 }" @tap="addressType(1)">到店自提</button>
				</view>
				<view class='address acea-row row-between-wrapper' @tap='onAddress' v-if='shippingType == 0'>
					<view class='addressCon' v-if="addressInfo.realName">
						<view class='name'>{{addressInfo.realName}}
							<text class='phone'>{{addressInfo.phone}}</text>
						</view>
						<view class="acea-row">
							<text class='default font_color' v-if="addressInfo.isDefault">[默认]</text>
							<text
								class="line2">{{addressInfo.province}}{{addressInfo.city}}{{addressInfo.district}}{{addressInfo.detail}}</text>
						</view>
					</view>
					<view class='addressCon' v-else>
						<view class='setaddress'>设置收货地址</view>
					</view>
					<view class='iconfont icon-jiantou'></view>
				</view>
				<view class='address acea-row row-between-wrapper' v-else @tap="showStoreList">
					<block v-if="storeList.length>0">
						<view class='addressCon'>
							<view class='name'>{{system_store.name}}
								<text class='phone'>{{system_store.phone}}</text>
							</view>
							<view class="line1"> {{system_store.address}}{{", " + system_store.detailedAddress}}
							</view>
						</view>
						<view class='iconfont icon-jiantou'></view>
					</block>
					<block v-else>
						<view>暂无门店信息</view>
					</block>
				</view>
			</view>
			<view class="pad30">
				<orderGoods :cartInfo="cartInfo" :orderProNum="orderProNum" checkout></orderGoods>
				<view class="flower-checkout">
					<view class="flower-checkout__heading">
						<text class="flower-checkout__title">送花信息</text>
					</view>

					<view class="flower-checkout__section">
						<text class="flower-checkout__section-title">订花人</text>
						<view class="flower-checkout__field-grid">
							<view class="flower-checkout__field">
								<text class="flower-checkout__label">姓名</text>
								<input v-model="flowerForm.senderName" maxlength="20" placeholder="请输入姓名" />
							</view>
							<view class="flower-checkout__field">
								<text class="flower-checkout__label">手机号</text>
								<input v-model="flowerForm.senderPhone" type="number" maxlength="11" placeholder="请输入手机号" />
							</view>
						</view>
					</view>

					<view v-if="shippingType === 0" class="flower-checkout__section">
						<text class="flower-checkout__section-title">收花人</text>
						<view class="flower-checkout__field-grid">
							<view class="flower-checkout__field">
								<text class="flower-checkout__label">姓名</text>
								<input v-model="flowerForm.recipientName" maxlength="20" placeholder="请输入姓名" />
							</view>
							<view class="flower-checkout__field">
								<text class="flower-checkout__label">手机号</text>
								<input v-model="flowerForm.recipientPhone" type="number" maxlength="11" placeholder="请输入手机号" />
							</view>
						</view>
					</view>

					<view class="flower-checkout__section">
						<view class="flower-checkout__section-head">
							<text class="flower-checkout__section-title">{{ shippingType === 0 ? '配送日期与时段' : '到店取花日期' }}</text>
							<text v-if="deliveryQuote && shippingType === 0" class="flower-checkout__zone">{{ deliveryQuote.zoneName }}</text>
						</view>
						<flower-delivery-slot-picker
							v-if="shippingType === 0"
							v-model="flowerForm.slotId"
							:slots="deliverySlots"
							@change="onFlowerSlotChange"
						/>
						<picker v-else mode="date" :value="flowerForm.deliveryDate" :start="today" @change="onPickupDateChange">
							<view class="flower-checkout__date-picker">{{ flowerForm.deliveryDate || '请选择取花日期' }}</view>
						</picker>
						<text v-if="slotLoading" class="flower-checkout__hint">正在加载可预约时段…</text>
					</view>

					<view class="flower-checkout__section">
						<view class="flower-checkout__section-head">
							<text class="flower-checkout__section-title">贺卡留言</text>
							<text class="flower-checkout__counter">{{ flowerForm.greetingCard.length }}/200</text>
						</view>
						<textarea v-model="flowerForm.greetingCard" maxlength="200" placeholder="写下想对收花人说的话" />
						<view class="flower-checkout__switches">
							<view class="flower-checkout__switch-row">
								<view><text class="flower-checkout__switch-title">匿名送花</text><text class="flower-checkout__switch-copy">不向收花人展示订花人姓名</text></view>
								<switch :checked="flowerForm.anonymous" color="#35402F" @change="flowerForm.anonymous = $event.detail.value" />
							</view>
							<view v-if="shippingType === 0" class="flower-checkout__switch-row">
								<view><text class="flower-checkout__switch-title">配送前联系收花人</text><text class="flower-checkout__switch-copy">便于确认收花时间</text></view>
								<switch :checked="flowerForm.contactRecipient" color="#35402F" @change="flowerForm.contactRecipient = $event.detail.value" />
							</view>
						</view>
					</view>

					<view class="flower-checkout__section">
						<text class="flower-checkout__section-title">花材缺货时</text>
						<button
							v-for="option in FLOWER_SUBSTITUTION_OPTIONS"
							:key="option.value"
							class="flower-checkout__option"
							:class="{ 'is-active': flowerForm.substitutionPolicy === option.value }"
							@click="flowerForm.substitutionPolicy = option.value"
						>
							<view><text class="flower-checkout__option-title">{{ option.label }}</text><text class="flower-checkout__option-copy">{{ option.description }}</text></view>
							<text class="flower-checkout__option-dot"></text>
						</button>
					</view>
				</view>
				<view class='wrapper checkout-extras borRadius14'>
					<view class='item acea-row row-between-wrapper' v-if='shippingType==0'>
						<view>快递费用</view>
						<view class='discount' v-if='parseFloat(orderInfoVo.freightFee) > 0'>
							+￥{{orderInfoVo.freightFee}}
						</view>
						<view class='discount' v-else>免运费</view>
					</view>
					<view v-else>
						<view class="item acea-row row-between-wrapper">
							<view>联系人</view>
							<view class="discount textR">
								<input type="text" placeholder="请填写您的联系姓名" placeholder-style="color:#ccc;"
									placeholder-class="placeholder" @blur='realName' maxlength="20"></input>
							</view>
						</view>
						<view class="item acea-row row-between-wrapper">
							<view>联系电话</view>
							<view class="discount textR">
								<input type="number" placeholder="请填写您的联系电话" placeholder-style="color:#ccc;"
									placeholder-class="placeholder" @blur='phone' maxlength="11"></input>
							</view>
						</view>
					</view>
					<!-- <view class='item acea-row row-between-wrapper' wx:else>
					  <view>自提门店</view>
					  <view class='discount'>{{system_store.name}}</view>
					</view> -->
					<view class='item' v-if="textareaStatus">
						<view class="flex justify-between">
							<view>备注信息</view>
							<view>
								<text style="color:#666;">{{markNum ? markNum : 0}}</text>/<text>150</text>
							</view>
						</view>
						<textarea placeholder-class='placeholder' @input='bindHideKeyboard'
							:maxlength="150" value="" name="mark" placeholder='请添加备注（150字以内）'></textarea>
					</view>
				</view>
				<view class='moneyList checkout-summary borRadius14'>
					<view class='item acea-row row-between-wrapper'>
						<view>商品总价：</view>
						<view class='money'>￥{{orderInfoVo.proTotalFee || 0}}</view>
					</view>
					<view class='item acea-row row-between-wrapper' v-if="orderInfoVo.couponFee > 0">
						<view>优惠抵扣：</view>
						<view class='money'>-￥{{orderInfoVo.couponFee}}</view>
					</view>
					<view class='item acea-row row-between-wrapper' v-if="orderInfoVo.freightFee > 0">
						<view>运费：</view>
						<view class='money'>+￥{{orderInfoVo.freightFee}}</view>
					</view>
				</view>
				<view class="checkout-footer-spacer"></view>
			</view>
			<view class='footer checkout-footer acea-row row-between-wrapper'>
				<view class="flower-checkout-total">
					<view>合计: <text class='price_color'>￥{{orderInfoVo.payFee || 0}}</text></view>
					<text v-if="shippingType === 0" class="flower-checkout-total__hint">含配送费，提交后保留时段15分钟</text>
				</view>
				<view class='settlement' style='z-index:100' @tap="SubOrder">提交订单</view>
			</view>
		</view>
		<view class="alipaysubmit" v-html="formContent"></view>
	</view>
</template>
<script setup>
	import { ref, reactive, computed, watch, getCurrentInstance } from 'vue';
	import { onLoad, onShow } from '@dcloudio/uni-app';
	import {
		getCouponsOrderPrice,
		createFlowerOrder,
		postOrderComputed,
		orderPay,
		wechatQueryPayResult,
		loadPreOrderApi
	} from '@/api/order.js';
	import { getFlowerDeliveryQuote, getFlowerDeliverySlots } from '@/api/flower.js';
	import {
		getAddressDetail,
		getAddressDefault
	} from '@/api/user.js';
	import {
		openPaySubscribe,
		openOrderSubscribe
	} from '@/utils/SubscribeMessage.js';
	import {
		storeListApi
	} from '@/api/store.js';
	import {
		CACHE_LONGITUDE,
		CACHE_LATITUDE
	} from '@/config/cache.js';
	import couponListWindow from '@/components/couponListWindow/index.vue';
	import orderGoods from '@/components/orderGoods/index.vue';
	import FlowerDeliverySlotPicker from '@/components/flower-delivery-slot-picker/index.vue';
	import {
		FLOWER_SUBSTITUTION_OPTIONS,
		buildFlowerOrderPayload,
		clearUnavailableFlowerSlot,
		validateFlowerCheckout,
	} from '@/utils/flower-checkout.js';
	import {
		toLogin
	} from '@/libs/login.js';
	import {
		Debounce
	} from '@/utils/validate.js'
	import util from '@/utils/util.js';
	import { useAppStore } from "@/store/app.js";
	import { storeToRefs } from 'pinia';
import { useColor } from '@/composables/useColor.js';
	let app = getApp();

	const { proxy } = getCurrentInstance();
	const appStore = useAppStore();
	const { isLogin, systemPlatform } = storeToRefs(appStore);

	// data
	const navTitle = ref('提交订单');
	// #ifdef MP
	const mpWindowInfo = uni.getWindowInfo();
	const mpWindowWidth = Number(mpWindowInfo.windowWidth || 375);
	const cachedNavHeight = Number(app.globalData.navHeight);
	const mpNavHeight =
		cachedNavHeight > 0
			? cachedNavHeight
			: Number(mpWindowInfo.statusBarHeight || 0) * (750 / mpWindowWidth) + 91;
	// #endif
	const orderShow = ref('orderShow'); //下单页面使用优惠券组件不展示tab切换页
	const textareaStatus = ref(true);
	//支付方式
	const cartArr = ref([{
			"name": "微信支付",
			"icon": "icon-weixin2",
			value: 'weixin',
			title: '微信快捷支付',
			payStatus: 1,
		},
		{
			"name": "余额支付",
			"icon": "icon-yuezhifu",
			value: 'yue',
			title: '可用余额:',
			payStatus: 1,
		},
		// #ifndef MP
		{
			"name": "支付宝支付",
			"icon": "icon-zhifubao",
			value: 'alipay',
			title: '支付宝快捷支付',
			payStatus: 1,
		}
		// #endif
	]);
	const payType = ref('weixin'); //支付方式
	const openType = ref(1); //优惠券打开方式 1=使用
	const active = ref(0); //支付方式切换
	const coupon = reactive({
		coupon: false,
		list: [],
		statusTile: '立即使用'
	}); //优惠券组件
	const addressInfo = ref({}); //地址信息
	const addressId = ref(0); //地址id
	const couponId = ref(0); //优惠券id
	const cartId = ref(''); //购物车id
	const mark = ref(''); //备注信息
	const couponTitle = ref('请选择'); //优惠券
	const coupon_price = ref(0); //优惠券抵扣金额
	const formIds = ref([]); //收集formid
	const status = ref(0);
	const is_address = ref(false);
	const toPay = ref(false); //修复进入支付时页面隐藏从新刷新页面
	const shippingType = ref(0);
	const system_store = ref({});
	const storePostage = ref(0);
	const contacts = ref('');
	const contactsTel = ref('');
	const mydata = ref({});
	const storeList = ref([]);
	const store_self_mention = ref(false);
	const cartInfo = ref([]);
	const animated = ref(false);
	const totalPrice = ref(0);
	const orderKey = ref("");
	// const usableCoupon = ref({});
	const offlinePostage = ref("");
	const payChannel = ref('');
	const news = ref(true);
	const again = ref(false);
	const addAgain = ref(false);
	const bargain = ref(false); //是否是砍价
	const combination = ref(false); //是否是拼团
	const secKill = ref(false); //是否是秒杀
	const orderInfoVo = ref({});
	const addressList = ref([]); //地址列表数据
	const orderProNum = ref(0);
	const preOrderNo = ref(''); //预下单订单号
	const theme = ref(app.globalData.theme);
	const { colorStyle } = useColor();
	const formContent = ref('');
	const addressChangeId = ref(0);
	const orderNo = ref(''); //下单订单号
	const today = new Date().toISOString().slice(0, 10);
	const deliveryQuote = ref(null);
	const deliverySlots = ref([]);
	const slotLoading = ref(false);
	const flowerForm = reactive({
		senderName: '', senderPhone: '', recipientName: '', recipientPhone: '',
		deliveryZoneId: null, deliveryDate: '', slotId: null, greetingCard: '',
		anonymous: false, contactRecipient: true, substitutionPolicy: 'CONTACT_FIRST',
	});

	// computed
	const markNum = computed(() => {
		if (mark.value) {
			return mark.value.length
		}
	});
	const canUseDiscount = computed(() => {
		const info = orderInfoVo.value;
		return info.isVideo === false && !info.bargainId && !info.combinationId && !info.seckillId;
	});

	watch(isLogin, (newV, oldV) => {
		if (newV) {
			getloadPreOrder();
		}
	}, { deep: true });

	onLoad((options) => {
		// #ifdef H5
		payChannel.value = proxy.$wechat.isWeixin() ? 'public' : 'weixinh5';
		// #endif
		// #ifdef MP
		payChannel.value = 'routine';
		// #endif
		// #ifdef APP-PLUS
		payChannel.value = systemPlatform.value === 'ios' ? 'weixinAppIos' : 'weixinAppAndroid';
		// #endif
		// if (!options.cartId) return util.Tips({
		// 	title: '请选择要购买的商品'
		// }, {
		// 	tab: 3,
		// 	url: 1
		// });
		preOrderNo.value = options.preOrderNo || 0;
		addressChangeId.value = options.addressId || 0;
		is_address.value = options.is_address ? true : false;
		if (isLogin.value) {
			getloadPreOrder();
		} else {
			toLogin();
		}
	});
	/**
	 * 生命周期函数--监听页面显示
	 */
	onShow(() => {
		// wx.getLaunchOptionsSync 
		textareaStatus.value = true;

		uni.$on("handClick", res => {
			if (res) {
				system_store.value = res.address
			}
			// 清除监听
			uni.$off('handClick');
		})
	});

	// #ifdef MP
	function goBack() {
		if (getCurrentPages().length > 1) {
			uni.navigateBack();
			return;
		}

		uni.switchTab({
			url: '/pages/index/index'
		});
	}
	// #endif

	// 订单详情
	function getloadPreOrder() {
		loadPreOrderApi(preOrderNo.value).then(res => {
			let info = res.data.orderInfoVo;
			orderInfoVo.value = info;
			appStore.PRODUCT_TYPE(info.isVideo ? 'video' : 'normal');
			if (info.addressId && addressChangeId.value === 0) {
				addressId.value = info.addressId;
			} else {
				addressId.value = addressChangeId.value;
				if (info.addressId != addressChangeId.value) computedPrice();
			}
			cartInfo.value = info.orderDetailList;
			orderProNum.value = info.orderProNum;
			cartArr.value[1].title = '可用余额:' + info.userBalance;
			cartArr.value[1].payStatus = parseInt(res.data.yuePayStatus) === 1 ? 1 : 2;
			cartArr.value[0].payStatus = parseInt(res.data.payWeixinOpen) === 1 ? 1 : 0;
			getaddressInfo();
			// #ifdef H5
			if (proxy.$wechat.isWeixin()) cartArr.value.pop();
			// #endif
			store_self_mention.value = res.data.storeSelfMention == '1' && info.isVideo === false;
		}).catch(err => {
			uni.navigateTo({
				url: '/pages/users/order_list/index'
			});
		})
	}
	/**
	 * 获取门店列表数据
	 */
	function getList() {
		let longitude = uni.getStorageSync("user_longitude") ? uni.getStorageSync("user_longitude") : ''; //经度
		let latitude = uni.getStorageSync("user_latitude") ? uni.getStorageSync("user_latitude") : ''; //纬度
		let data = {
			latitude: latitude, //纬度
			longitude: longitude, //经度
			page: 1,
			limit: 10
		}
		storeListApi(data).then(res => {
			let list = res.data.list || [];
			storeList.value = list;
			system_store.value = list[0];
		}).catch(err => {
			return util.Tips({
				title: err
			});
		})
	}
	/*
	 * 跳转门店列表
	 */
	function showStoreList() {
		if (storeList.value.length > 0) {
			uni.navigateTo({
				url: '/pages/goods/goods_details_store/index'
			})
		}
	}
	// 计算订单价格
	function computedPrice() {
		postOrderComputed({
			addressId: addressId.value,
			useIntegral: false, // 开源版不支持积分抵扣，后端要求该字段非空
			couponId: couponId.value,
			shippingType: parseInt(shippingType.value) + 1,
			preOrderNo: preOrderNo.value,
			flower: buildFlowerOrderPayload(flowerForm, { shippingType: shippingType.value + 1 })
		}).then(res => {
			let data = res.data;
			orderInfoVo.value.couponFee = data.couponFee;
			orderInfoVo.value.freightFee = data.freightFee;
			orderInfoVo.value.payFee = data.payFee;
			orderInfoVo.value.proTotalFee = data.proTotalFee;
		}).catch(err => {
			return util.Tips({
				title: err
			});
		});
	}
	//选择地址还是门店自提
	function addressType(e) {
		let index = e;
		shippingType.value = parseInt(index);
		computedPrice();
		//调起获取定位信息
		if (index == 1) {
			flowerForm.slotId = null;
			if (!flowerForm.deliveryDate) flowerForm.deliveryDate = today;
			getList();
			util.$L.getLocation();
		} else {
			refreshFlowerDelivery();
		}
	}
	function bindPickerChange(e) {
		let value = e.detail.value;
		shippingType.value = value;
		computedPrice();
	}
	function ChangCouponsClone() {
		coupon.coupon = false;
	}
	/**
	 * 处理点击优惠券后的事件
	 * 
	 */
	function ChangCoupons(e) {
		// usableCoupon.value = e
		// coupon.coupon = false
		let index = e,
			list = coupon.list,
			title = '请选择',
			id = 0;
		for (let i = 0, len = list.length; i < len; i++) {
			if (i != index) {
				list[i].use_title = '';
				list[i].isUse = 0;
			}
		}
		if (list[index].isUse) {
			//不使用优惠券
			list[index].use_title = '';
			list[index].isUse = 0;
		} else {
			//使用优惠券
			list[index].use_title = '不使用';
			list[index].isUse = 1;
			title = list[index].name;
			id = list[index].id;
		}
		couponTitle.value = title;
		couponId.value = id;
		coupon.coupon = false;
		coupon.list = list;
		computedPrice();
	}
	function bindHideKeyboard(e) {
		mark.value = e.detail.value;
	}
	/**
	 * 获取当前金额可用优惠券
	 * 
	 */
	function getCouponList() {
		getCouponsOrderPrice(preOrderNo.value).then(res => {
			coupon.list = res.data;
			openType.value = 1;
		});
	}
	/*
	 * 获取默认收货地址或者获取某条地址信息
	 */
	function getaddressInfo() {
		if (addressId.value) {

			getAddressDetail(addressId.value).then(res => {
				if (res.data) {
					res.data.isDefault = res.data.isDefault;
					addressInfo.value = res.data || {};
					if (!flowerForm.senderName) flowerForm.senderName = res.data.realName || '';
					if (!flowerForm.senderPhone) flowerForm.senderPhone = res.data.phone || '';
					if (!flowerForm.recipientName) flowerForm.recipientName = res.data.realName || '';
					if (!flowerForm.recipientPhone) flowerForm.recipientPhone = res.data.phone || '';
					refreshFlowerDelivery();
				}
			})
		}
	}
	function payItem(e) {
		let activeVal = e;
		active.value = activeVal;
		animated.value = true;
		payType.value = cartArr.value[activeVal].value;
		// #ifdef H5
		if (payType.value == 'alipay' && proxy.$wechat.isWeixin() == true) {
			payChannel.value = 'public';
		} else if (payType.value == 'alipay' && proxy.$wechat.isWeixin() == false) {
			payChannel.value = 'weixinh5';
		}
		// #endif
		// #ifdef APP-PLUS
		if (payType.value == 'alipay') {
			payChannel.value = 'appAliPay';
		}
		// #endif
		//computedPrice();
		setTimeout(function() {
			car();
		}, 500);
	}
	function couponTap() {
		coupon.coupon = true;
		if (!coupon.list.length) getCouponList();
	}
	function car() {
		animated.value = false;
	}
	function onAddress() {

		uni.redirectTo({
			url: '/pages/users/user_address_list/index?preOrderNo=' + preOrderNo.value
		});
	}
	function realName(e) {
		contacts.value = e.detail.value;
	}
	function phone(e) {
		contactsTel.value = e.detail.value;
	}
	function payment(data) {
		// #ifdef MP
		uni.checkBeforeAddOrder({
		success(res) {
			const traceId = res.data.traceId;
			data.traceId = traceId;
			onCreate(data);
		},
		fail(res) {
		}
	});
		// #endif
		// #ifndef MP
		onCreate(data);
		// #endif
	}
	function onCreate(data) {
		createFlowerOrder(data).then(res => {
			orderNo.value = res.data.orderNo;
			uni.navigateTo({
				url: `/pages/order/order_payment/index?orderNo=${orderNo.value}&payPrice=${orderInfoVo.value.payFee}`
			})
		}).catch(err => {
			uni.hideLoading();
			if (String(err).includes('时段')) {
				flowerForm.slotId = null;
				flowerForm.deliveryDate = '';
				refreshFlowerDelivery();
			}
			return util.Tips({
				title: err
			}, '/pages/users/order_list/index');
		});
	}
	function SubOrder(e) {
		let data = {};
		if (!addressId.value && !shippingType.value) return util.Tips({
			title: '请选择收货地址'
		});
		const flowerErrors = validateFlowerCheckout(flowerForm, { shippingType: shippingType.value + 1 });
		const firstFlowerError = Object.values(flowerErrors)[0];
		if (firstFlowerError) return util.Tips({ title: firstFlowerError });
		if (shippingType.value == 1) {
			if (contacts.value == "" || contactsTel.value == "") {
				return util.Tips({
					title: '请填写联系人及联系人电话'
				});
			}
			if (!/^1(3|4|5|7|8|9|6)\d{9}$/.test(contactsTel.value)) {
				return util.Tips({
					title: '请填写正确的手机号'
				});
			}
			if (!/^[\u4e00-\u9fa5\w]{2,16}$/.test(contacts.value)) {
				return util.Tips({
					title: '请填写您的真实姓名'
				});
			}
			if (storeList.value.length == 0) return util.Tips({
				title: '暂无门店,请选择其他方式'
			});
		}
		data = {
			realName: contacts.value,
			phone: contactsTel.value,
			addressId: addressId.value,
			couponId: couponId.value,
			useIntegral: false, // 开源版不支持积分抵扣，后端要求该字段非空
			preOrderNo: preOrderNo.value,
			mark: mark.value,
			storeId: system_store.value.id || 0,
			shippingType: util.$h.Add(shippingType.value, 1),
			flower: buildFlowerOrderPayload(flowerForm, { shippingType: shippingType.value + 1 }),
		};
		// #ifdef MP
		openPaySubscribe().then(() => {
			payment(data);
		});
		// #endif
		// #ifndef MP
		payment(data);
		// #endif
	}

	function dateOffset(days) {
		const date = new Date();
		date.setDate(date.getDate() + days);
		return date.toISOString().slice(0, 10);
	}

	async function refreshFlowerDelivery() {
		if (shippingType.value !== 0 || !addressId.value || !Number(orderInfoVo.value.proTotalFee || 0)) return;
		slotLoading.value = true;
		try {
			const quoteResponse = await getFlowerDeliveryQuote({
				addressId: addressId.value,
				goodsAmount: orderInfoVo.value.proTotalFee,
			});
			deliveryQuote.value = quoteResponse.data;
			flowerForm.deliveryZoneId = quoteResponse.data.zoneId;
			const slotResponse = await getFlowerDeliverySlots({
				zoneId: quoteResponse.data.zoneId,
				startDate: dateOffset(0),
				endDate: dateOffset(6),
			});
			deliverySlots.value = slotResponse.data || [];
			const selection = clearUnavailableFlowerSlot({
				deliveryDate: flowerForm.deliveryDate,
				slotId: flowerForm.slotId,
			}, deliverySlots.value);
			flowerForm.deliveryDate = selection.deliveryDate;
			flowerForm.slotId = selection.slotId;
			computedPrice();
		} catch (error) {
			deliveryQuote.value = null;
			deliverySlots.value = [];
			flowerForm.deliveryZoneId = null;
			flowerForm.deliveryDate = '';
			flowerForm.slotId = null;
			util.Tips({ title: error });
		} finally {
			slotLoading.value = false;
		}
	}

	function onFlowerSlotChange(selection) {
		flowerForm.deliveryDate = selection.deliveryDate;
		flowerForm.slotId = selection.slotId;
	}

	function onPickupDateChange(event) {
		flowerForm.deliveryDate = event.detail.value;
	}
</script>

<style lang="scss" scoped>
	@import '@/static/css/flower-tokens.scss';
	.order-submission { min-height: 100vh; background: $flower-background; }

	.flower-checkout {
		margin: 18rpx 0;
		padding: 0 28rpx;
		border-radius: 16rpx;
		background: $flower-paper;
		color: $flower-text;
	}
	.flower-checkout__heading { padding: 28rpx 0 20rpx; border-bottom: 1rpx solid rgba($flower-primary, .12); }
	.flower-checkout__title { display: block; font-family: $flower-serif-cn; font-size: 34rpx; font-weight: 600; }
	.flower-checkout__section { padding: 28rpx 0; border-bottom: 1rpx solid rgba($flower-primary, .12); }
	.flower-checkout__section:last-child { border-bottom: 0; }
	.flower-checkout__section-title { display: block; font-family: $flower-serif-cn; font-size: 29rpx; font-weight: 500; }
	.flower-checkout__section-head { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; margin-bottom: 18rpx; }
	.flower-checkout__zone, .flower-checkout__counter, .flower-checkout__hint { font-size: 22rpx; color: rgba($flower-text, .56); }
	.flower-checkout__field-grid { display: flex; gap: 16rpx; margin-top: 18rpx; }
	.flower-checkout__field { box-sizing: border-box; flex: 1; min-width: 0; padding: 16rpx 18rpx; border: 1rpx solid rgba($flower-primary, .12); border-radius: 10rpx; background: $flower-background; }
	.flower-checkout__label { display: block; font-size: 22rpx; color: rgba($flower-text, .62); }
	.flower-checkout__field input { display: block; width: 100%; height: 56rpx; margin-top: 5rpx; padding: 0; font-size: 25rpx; text-align: left; }
	.flower-checkout textarea { box-sizing: border-box; width: 100%; height: 148rpx; margin-top: 0; padding: 18rpx; border-radius: 10rpx; font-size: 25rpx; line-height: 1.6; background: $flower-background; }
	.flower-checkout__switches { margin-top: 16rpx; }
	.flower-checkout__switch-row { display: flex; align-items: center; justify-content: space-between; gap: 20rpx; padding: 18rpx 0; border-bottom: 1rpx solid rgba($flower-primary, .08); }
	.flower-checkout__switch-row:last-child { border-bottom: 0; }
	.flower-checkout__switch-title, .flower-checkout__switch-copy, .flower-checkout__option-title, .flower-checkout__option-copy { display: block; }
	.flower-checkout__switch-title, .flower-checkout__option-title { font-size: 26rpx; }
	.flower-checkout__switch-copy, .flower-checkout__option-copy { margin-top: 6rpx; font-size: 21rpx; line-height: 1.45; color: rgba($flower-text, .52); }
	.flower-checkout__option { display: flex; align-items: center; justify-content: space-between; gap: 20rpx; width: 100%; margin: 14rpx 0 0; padding: 18rpx 20rpx; text-align: left; color: $flower-text; background: $flower-paper; border: 1rpx solid rgba($flower-primary, .16); border-radius: 10rpx; }
	.flower-checkout__option::after { border: 0; }
	.flower-checkout__option.is-active { background: rgba($flower-primary, .07); border-color: $flower-primary; }
	.flower-checkout__option-dot { width: 16rpx; height: 16rpx; flex: 0 0 16rpx; border: 2rpx solid rgba($flower-primary, .36); border-radius: 50%; }
	.flower-checkout__option.is-active .flower-checkout__option-dot { background: $flower-primary; border-color: $flower-primary; box-shadow: 0 0 0 5rpx rgba($flower-primary, .1); }
	.flower-checkout__date-picker { padding: 22rpx; border-radius: 10rpx; font-size: 26rpx; background: $flower-background; }
	.flower-checkout__hint { display: block; margin-top: 16rpx; }
	.flower-checkout-total__hint { display: block; margin-top: 4rpx; font-size: 19rpx; color: #8a8a82; }
	/* #ifdef MP */
	.mp-nav-bar {
		position: fixed;
		top: 0;
		right: 0;
		left: 0;
		z-index: 998;
		box-sizing: border-box;
		@include main_bg_color(theme);
	}

	.mp-nav-bar__content {
		display: flex;
		align-items: center;
		position: absolute;
		right: 0;
		bottom: 16rpx;
		left: 0;
		height: 60rpx;
		padding: 0 14rpx;
		box-sizing: border-box;
	}

	.mp-nav-bar__back,
	.mp-nav-bar__placeholder {
		width: 72rpx;
		flex: 0 0 72rpx;
	}

	.mp-nav-bar__back {
		display: flex;
		align-items: center;
		justify-content: center;
		height: 60rpx;
		border: 1px solid rgba(0, 0, 0, 0.07);
		border-radius: 30rpx;
		background: rgba(255, 255, 255, 0.6);
		color: #000;
		font-size: 36rpx;
	}

	.mp-nav-bar__title {
		flex: 1;
		height: 60rpx;
		overflow: hidden;
		color: #fff;
		font-size: 34rpx;
		font-weight: 500;
		line-height: 60rpx;
		text-align: center;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.mp-nav-placeholder {
		width: 100%;
	}
	/* #endif */

	.font_color {
		@include main_color(theme);
	}

	.price_color {
		@include price_color(theme);
	}

	.line2 {
		min-width: 0;
		flex: 1;
	}

	.textR {
		text-align: right;
	}

	.order-submission .line {
		width: 100%;
		height: 3rpx;
	}

	.order-submission .line image {
		width: 100%;
		height: 100%;
		display: block;
	}

	.order-submission .address {
		box-sizing: border-box;
		min-height: 104rpx;
		padding: 24rpx 28rpx;
		border-radius: 16rpx;
		background: $flower-paper;
	}

	.order-submission .address .addressCon {
		flex: 1;
		min-width: 0;
		font-size: 25rpx;
		color: rgba($flower-text, .7);
	}

	.order-submission .address .addressCon .name {
		margin-bottom: 8rpx;
		color: $flower-text;
		font-size: 29rpx;
		font-weight: 600;
	}

	.order-submission .address .addressCon .name .phone {
		margin-left: 20rpx;
		font-size: 25rpx;
		font-weight: 400;
	}

	.order-submission .address .addressCon .default {
		margin-right: 12rpx;
	}

	.order-submission .address .addressCon .setaddress {
		color: $flower-text;
		font-size: 28rpx;
	}

	.order-submission .address .iconfont {
		margin-left: 16rpx;
		color: rgba($flower-text, .55);
		font-size: 32rpx;
	}

	.order-submission .allAddress {
		box-sizing: border-box;
		width: 100%;
		padding: 18rpx 30rpx 0;
		background: $flower-background;
	}

	.order-submission .delivery-modes {
		display: flex;
		gap: 14rpx;
		margin-bottom: 16rpx;
	}

	.order-submission .delivery-mode {
		flex: 1;
		height: 72rpx;
		margin: 0;
		padding: 0;
		border: 1rpx solid rgba($flower-primary, .18);
		border-radius: 10rpx;
		background: $flower-paper;
		color: $flower-text;
		font-size: 26rpx;
		line-height: 70rpx;
	}

	.order-submission .delivery-mode::after {
		border: 0;
	}

	.order-submission .delivery-mode.is-active {
		border-color: $flower-primary;
		background: $flower-primary;
		color: $flower-paper;
	}

	.order-submission .allAddress .address {
		width: 100%;
		margin: 0;
	}

	.order-submission .wrapper .item .discount .placeholder {
		color: #ccc;
	}

	.order-submission .wrapper {
		margin-top: 18rpx;
		overflow: hidden;
		border-radius: 16rpx;
		background: $flower-paper;
	}

	.order-submission .wrapper .item {
		padding: 25rpx 28rpx;
		border-bottom: 1rpx solid rgba($flower-primary, .1);
		color: $flower-text;
		font-size: 27rpx;
	}
	.order-submission .wrapper .item:last-child { border-bottom: 0; }

	.order-submission .wrapper .item .discount {
		font-size: 26rpx;
		color: rgba($flower-text, .75);
	}

	.order-submission .wrapper .item .discount .iconfont {
		color: #515151;
		font-size: 30rpx;
		margin-left: 15rpx;
	}

	.order-submission .wrapper .item .discount .num {
		font-size: 32rpx;
		margin-right: 20rpx;
	}

	.order-submission .wrapper .item .shipping {
		font-size: 30rpx;
		color: #999;
		position: relative;
		padding-right: 58rpx;
	}

	.order-submission .wrapper .item .shipping .iconfont {
		font-size: 35rpx;
		color: #707070;
		position: absolute;
		right: 0;
		top: 50%;
		transform: translateY(-50%);
		margin-left: 30rpx;
	}

	.order-submission .wrapper .item textarea {
		width: 100% !important;
		height: 128rpx;
		margin-top: 18rpx;
		padding: 18rpx;
		box-sizing: border-box;
		border-radius: 10rpx;
		background: $flower-background;
		font-weight: 400;
		font-size: 25rpx;
	}

	.order-submission .wrapper .item .placeholder {
		color: #ccc;
	}

	.order-submission .wrapper .item .list {
		margin-top: 35rpx;
	}

	.order-submission .wrapper .item .list .payItem {
		border: 1px solid #eee;
		border-radius: 14rpx;
		height: 86rpx;
		width: 100%;
		box-sizing: border-box;
		margin-top: 20rpx;
		font-size: 28rpx;
		color: #282828;
	}

	.order-submission .wrapper .item .list .payItem.on {
		// border-color: #fc5445;
		@include coupons_border_color(theme);
		color: $theme-color;
	}

	.order-submission .wrapper .item .list .payItem .name {
		width: 50%;
		text-align: center;
		border-right: 1px solid #eee;
	}

	.order-submission .wrapper .item .list .payItem .name .iconfont {
		width: 44rpx;
		height: 44rpx;
		border-radius: 50%;
		text-align: center;
		line-height: 44rpx;
		background-color: #fe960f;
		color: #fff;
		font-size: 30rpx;
		margin-right: 15rpx;
	}

	.order-submission .wrapper .item .list .payItem .name .iconfont.icon-weixin2 {
		background-color: #41b035;
	}

	.order-submission .wrapper .item .list .payItem .name .iconfont.icon-zhifubao {
		background-color: #00AAEA;
	}

	.order-submission .wrapper .item .list .payItem .tip {
		width: 49%;
		text-align: center;
		font-size: 26rpx;
		color: #aaa;
	}

	.order-submission .moneyList {
		margin-top: 18rpx;
		margin-bottom: 0;
		padding: 26rpx 28rpx;
		border-radius: 16rpx;
		background: $flower-paper;
	}

	.order-submission .moneyList .item {
		font-size: 26rpx;
		color: $flower-text;
	}

	.order-submission .moneyList .item~.item {
		margin-top: 16rpx;
	}

	.order-submission .moneyList .item .money {
		color: rgba($flower-text, .78);
	}

	.checkout-footer-spacer { height: calc(130rpx + env(safe-area-inset-bottom)); }

	.order-submission .footer {
		width: 100%;
		height: calc(112rpx + env(safe-area-inset-bottom));
		padding: 0 30rpx env(safe-area-inset-bottom);
		border-top: 1rpx solid rgba($flower-primary, .12);
		background: $flower-paper;
		font-size: 27rpx;
		color: $flower-text;
		box-sizing: border-box;
		position: fixed;
		bottom: 0;
		left: 0;
		z-index: 30;
	}
	.flower-checkout-total { flex: 1; min-width: 0; }
	.flower-checkout-total .price_color { font-size: 34rpx; font-weight: 600; }

	.order-submission .footer .settlement {
		flex: none;
		font-size: 28rpx;
		color: #fff;
		width: 218rpx;
		height: 76rpx;
		@include main_bg_color(theme);
		border-radius: 50rpx;
		text-align: center;
		line-height: 76rpx;
	}

	.footer .transparent {
		opacity: 0
	}
</style>
