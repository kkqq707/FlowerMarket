package com.zbkj.service.service.impl;

import com.zbkj.common.constants.Constants;
import com.zbkj.common.constants.SysConfigConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.flower.FlowerOrderExt;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.model.user.User;
import com.zbkj.common.response.OrderPayResultResponse;
import com.zbkj.service.dao.FlowerOrderExtDao;
import com.zbkj.service.service.*;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Date;

@Service
public class FlowerMockPayServiceImpl implements FlowerMockPayService {

    private static final String MOCK_PAY_TYPE = Constants.PAY_TYPE_MOCK;

    private final SystemConfigService configService;
    private final Environment environment;
    private final StoreOrderService orderService;
    private final UserService userService;
    private final FlowerOrderExtDao orderExtDao;
    private final OrderPayService orderPayService;

    public FlowerMockPayServiceImpl(SystemConfigService configService,
                                    Environment environment,
                                    StoreOrderService orderService,
                                    UserService userService,
                                    FlowerOrderExtDao orderExtDao,
                                    OrderPayService orderPayService) {
        this.configService = configService;
        this.environment = environment;
        this.orderService = orderService;
        this.userService = userService;
        this.orderExtDao = orderExtDao;
        this.orderPayService = orderPayService;
    }

    @Override
    public OrderPayResultResponse pay(String orderNo) {
        ensureAvailable();
        if (!StringUtils.hasText(orderNo)) {
            throw new CrmebException("订单号不能为空");
        }
        User currentUser = userService.getInfoException();
        StoreOrder order = orderService.getByOderId(orderNo);
        if (order == null) throw new CrmebException("订单不存在");
        if (!order.getUid().equals(currentUser.getUid())) throw new CrmebException("无权操作该订单");
        if (Boolean.TRUE.equals(order.getIsDel()) || Boolean.TRUE.equals(order.getIsSystemDel())) {
            throw new CrmebException("订单已取消");
        }
        if (order.getRefundStatus() != null && order.getRefundStatus() != 0) {
            throw new CrmebException("退款订单不能支付");
        }
        if (Boolean.TRUE.equals(order.getPaid())) {
            return success(orderNo);
        }
        FlowerOrderExt extension = orderExtDao.selectByOrderId(orderNo);
        if (extension == null) throw new CrmebException("该订单不是花店演示订单");
        if (Boolean.TRUE.equals(extension.getReservationReleased())
                || extension.getReservationExpireTime() == null
                || !extension.getReservationExpireTime().after(new Date())) {
            throw new CrmebException("订单预约已过期，请重新下单");
        }
        boolean changed = orderPayService.markPaidAndEnqueueSuccess(orderNo, MOCK_PAY_TYPE, "MOCK-" + orderNo);
        if (!changed) {
            StoreOrder freshOrder = orderService.getByOderId(orderNo);
            if (freshOrder == null || !Boolean.TRUE.equals(freshOrder.getPaid())) {
                throw new CrmebException("订单状态已变化，请刷新后重试");
            }
        }
        return success(orderNo);
    }

    private void ensureAvailable() {
        boolean production = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile));
        if (production) {
            throw new CrmebException("生产环境禁止模拟支付");
        }
        String value = configService.getValueByKey(SysConfigConstants.CONFIG_FLOWER_MOCK_PAY_ENABLED);
        if (!("1".equals(value) || "'1'".equals(value) || "true".equalsIgnoreCase(value))) {
            throw new CrmebException("模拟支付未开启");
        }
    }

    private OrderPayResultResponse success(String orderNo) {
        return new OrderPayResultResponse().setStatus(true).setOrderNo(orderNo).setPayType(MOCK_PAY_TYPE);
    }
}
