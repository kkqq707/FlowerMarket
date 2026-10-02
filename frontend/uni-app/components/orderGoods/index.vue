<template>
	<view class="orderGoods borRadius14" :class="{ 'orderGoods--checkout': checkout }">
		<view class='total'>共{{ orderProNum?orderProNum:totalNmu}}件商品</view>
		<view class='goodWrapper pad30'>
			<view class='item acea-row row-between-wrapper' v-for="(item,index) in cartInfo" :key="index"
				@click="jumpCon(item.productId)">
				<view class='pictrue'>
					<image :src='item.image'></image>
				</view>
				<view class='text'>
					<view class='acea-row row-between-wrapper'>
						<view class='name line1'>{{item.productName ? item.productName : item.storeName}}</view>
						<view class='num'>x {{item.payNum ? item.payNum : item.cartNum}}</view>
					</view>
					<view class='attr line1' v-if="item.sku">{{item.sku}}</view>
					<view class='money'>￥{{item.price}}</view>
					<view class='evaluate' v-if='item.isReply==0 && evaluate==2' @click.stop="evaluateTap(item)">评价
					</view>
					<view class='evaluate' v-else-if="item.isReply==1">已评价</view>
				</view>
			</view>
		</view>
	</view>

</template>

<script setup>
	import { ref, watch } from 'vue'

	const props = defineProps({
		checkout: { type: Boolean, default: false },
		evaluate: {
			type: Number,
			default: 0,
		},
		cartInfo: {
			type: Array,
			default: function() {
				return [];
			}
		},
		orderId: {
			type: String,
			default: '',
		},
		ids: {
			type: Number,
			default: 0,
		},
		jump: {
			type: Boolean,
			default: false,
		},
		orderProNum: {
			type: Number,
			default: function() {
				return 0;
			}
		},
		productType: {
			type: Number,
			default: function() {
				return 0;
			}
		}
	})

	const totalNmu = ref('')

	watch(() => props.cartInfo, (nVal, oVal) => {
		let num = 0
		nVal.forEach((item, index) => {
			num += item.cartNum
		})
		totalNmu.value = num
	})

	function evaluateTap(item) {
		uni.navigateTo({
			url: "/pages/goods/goods_comment_con/index?unique=" + item.attrId + "&orderId=" + props.orderId + '&id=' + props.ids
		})
	}
	function jumpCon(id) {
		let type = props.productType==0?'normal':'video'
		if (props.jump) {
			uni.navigateTo({
				url: `/pages/goods/goods_details/index?id=${id}&type=${type}`
			})
		}
	}
</script>

<style scoped lang="scss">
	.orderGoods {
		background-color: #fff;
		margin-top: 15rpx;
	}
	.money{
		@include price_color(theme);
	}
	.orderGoods .total {
		width: 100%;
		height: 86rpx;
		padding: 0 24rpx;
		border-bottom: 2rpx solid #f0f0f0;
		font-size: 30rpx;
		color: #282828;
		line-height: 86rpx;
		box-sizing: border-box;
	}
	.orderGoods--checkout { margin-top: 0; overflow: hidden; border-radius: 16rpx; }
	.orderGoods--checkout .total { height: auto; padding: 24rpx 28rpx; border-bottom: 1rpx solid #eae9df; color: #2d312c; font-size: 27rpx; line-height: 1.4; }
	.orderGoods--checkout .goodWrapper { padding: 0 28rpx; }
	.orderGoods--checkout .goodWrapper .item { height: auto; min-height: 156rpx; padding: 18rpx 0; gap: 20rpx; border-bottom: 1rpx solid #eae9df; }
	.orderGoods--checkout .goodWrapper .item:last-child { border-bottom: 0; }
	.orderGoods--checkout .goodWrapper .item .pictrue { width: 124rpx; height: 124rpx; flex: none; }
	.orderGoods--checkout .goodWrapper .item .text { width: auto; min-width: 0; flex: 1; }
	.orderGoods--checkout .goodWrapper .item .text .name { width: auto; min-width: 0; flex: 1; }
	.orderGoods--checkout .goodWrapper .item .text .num { flex: none; margin-left: 12rpx; }
	.orderGoods--checkout .goodWrapper .item .text .money { margin-top: 14rpx; font-size: 28rpx; }

	.pictrue image {
		background: #f4f4f4;
	}
</style>
