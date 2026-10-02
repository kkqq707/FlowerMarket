package com.zbkj.service.service.impl;

import cn.hutool.core.date.DateUtil;
import com.zbkj.common.enums.FlowerOrderStageEnum;
import com.zbkj.common.enums.FlowerSubstitutionPolicyEnum;
import com.zbkj.common.constants.Constants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.flower.FlowerOrderExt;
import com.zbkj.common.model.flower.FlowerOrderProgress;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.model.user.UserAddress;
import com.zbkj.common.request.flower.FlowerOrderRequest;
import com.zbkj.common.request.FlowerOrderTransitRequest;
import com.zbkj.common.request.StoreOrderSendRequest;
import com.zbkj.common.response.FlowerOrderDetailResponse;
import com.zbkj.common.response.FlowerOrderAdminListResponse;
import com.zbkj.common.request.flower.FlowerOrderAdminSearchRequest;
import com.zbkj.service.service.StoreOrderInfoService;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.response.flower.FlowerDeliveryQuoteResponse;
import com.zbkj.service.dao.FlowerOrderExtDao;
import com.zbkj.service.dao.FlowerOrderProgressDao;
import com.zbkj.service.dao.FlowerDeliverySlotDao;
import com.zbkj.service.dao.FlowerProductExtDao;
import com.zbkj.common.model.flower.FlowerDeliverySlot;
import com.zbkj.common.model.flower.FlowerProductExt;
import com.zbkj.common.model.order.StoreOrderInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.service.service.FlowerDeliveryService;
import com.zbkj.service.service.FlowerMaterialService;
import com.zbkj.service.service.FlowerOrderService;
import com.zbkj.service.service.OrderService;
import com.zbkj.service.service.StoreOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class FlowerOrderServiceImpl implements FlowerOrderService {

    private final FlowerOrderExtDao orderExtDao;
    private final FlowerOrderProgressDao progressDao;
    private final FlowerDeliveryService deliveryService;
    private final StoreOrderService storeOrderService;
    private final OrderService orderService;

    @Autowired(required = false)
    private StoreOrderInfoService storeOrderInfoService;

    @Autowired(required = false)
    private FlowerDeliverySlotDao flowerDeliverySlotDao;

    @Autowired(required = false)
    private FlowerProductExtDao flowerProductExtDao;

    @Autowired(required = false)
    private FlowerMaterialService flowerMaterialService;

    @Autowired
    public FlowerOrderServiceImpl(FlowerOrderExtDao orderExtDao,
                                  FlowerOrderProgressDao progressDao,
                                  FlowerDeliveryService deliveryService,
                                  StoreOrderService storeOrderService,
                                  @org.springframework.context.annotation.Lazy OrderService orderService) {
        this.orderExtDao = orderExtDao;
        this.progressDao = progressDao;
        this.deliveryService = deliveryService;
        this.storeOrderService = storeOrderService;
        this.orderService = orderService;
    }

    public FlowerOrderServiceImpl(FlowerOrderExtDao orderExtDao,
                                  FlowerOrderProgressDao progressDao,
                                  FlowerDeliveryService deliveryService,
                                  StoreOrderService storeOrderService) {
        this(orderExtDao, progressDao, deliveryService, storeOrderService, null);
    }

    public FlowerOrderServiceImpl(FlowerOrderExtDao orderExtDao,
                                  FlowerOrderProgressDao progressDao,
                                  FlowerDeliveryService deliveryService) {
        this(orderExtDao, progressDao, deliveryService, null, null);
    }

    @Override
    public void requireFlowerRequest(boolean containsFlowerProducts, FlowerOrderRequest request) {
        if (containsFlowerProducts && request == null) {
            throw new CrmebException("鲜花商品必须填写送花信息");
        }
    }

    @Override
    public FlowerDeliveryQuoteResponse validateForCreate(FlowerOrderRequest request, UserAddress address,
                                                          Integer currentUid, Integer shippingType, BigDecimal goodsAmount) {
        if (request == null) {
            throw new CrmebException("请填写送花信息");
        }
        requireContact(request.getSenderName(), request.getSenderPhone(), "订花人");
        if (request.getDeliveryDate() == null) {
            throw new CrmebException("请选择送花日期");
        }
        ZoneId storeZone = ZoneId.of("Asia/Shanghai");
        if (request.getDeliveryDate().toInstant().atZone(storeZone).toLocalDate()
                .isBefore(LocalDate.now(storeZone))) {
            throw new CrmebException("送花或取花日期不能早于今天");
        }
        if (Integer.valueOf(2).equals(shippingType)) {
            return null;
        }
        requireContact(request.getRecipientName(), request.getRecipientPhone(), "收花人");
        if (address == null || Boolean.TRUE.equals(address.getIsDel())
                || currentUid == null || !currentUid.equals(address.getUid())) {
            throw new CrmebException("收花地址不存在");
        }
        if (request.getDeliveryZoneId() == null || request.getSlotId() == null) {
            throw new CrmebException("请选择配送区域和配送时段");
        }
        if (request.getSubstitutionPolicy() == null) {
            request.setSubstitutionPolicy(FlowerSubstitutionPolicyEnum.CONTACT_FIRST);
        }
        FlowerDeliveryQuoteResponse quote = deliveryService.quote(address.getId(), goodsAmount);
        if (!request.getDeliveryZoneId().equals(quote.getZoneId())) {
            throw new CrmebException("选择的配送区域与收花地址不匹配");
        }
        deliveryService.validateSelection(request.getDeliveryZoneId(), request.getDeliveryDate(), request.getSlotId());
        return quote;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowerOrderExt createExtension(StoreOrder order, FlowerOrderRequest request, UserAddress address) {
        if (order == null || !StringUtils.hasText(order.getOrderId()) || request == null) {
            throw new CrmebException("送花订单资料不完整");
        }
        Date now = new Date();
        FlowerOrderExt extension = new FlowerOrderExt()
                .setOrderId(order.getOrderId())
                .setSenderName(trim(request.getSenderName()))
                .setSenderPhone(trim(request.getSenderPhone()))
                .setRecipientName(trim(request.getRecipientName()))
                .setRecipientPhone(trim(request.getRecipientPhone()))
                .setDeliveryAddressSnapshot(address == null ? "" : addressSnapshot(address))
                .setDeliveryZoneId(request.getDeliveryZoneId())
                .setDeliveryDate(request.getDeliveryDate())
                .setSlotId(request.getSlotId())
                .setGreetingCard(trim(request.getGreetingCard()))
                .setAnonymous(Boolean.TRUE.equals(request.getAnonymous()))
                .setContactRecipient(!Boolean.FALSE.equals(request.getContactRecipient()))
                .setSubstitutionPolicy((request.getSubstitutionPolicy() == null
                        ? FlowerSubstitutionPolicyEnum.CONTACT_FIRST : request.getSubstitutionPolicy()).name())
                .setCurrentStage(FlowerOrderStageEnum.WAIT_PAY.name())
                .setReservationExpireTime(DateUtil.offsetMinute(now, 15))
                .setReservationReleased(false)
                .setCreateTime(now)
                .setUpdateTime(now);
        if (orderExtDao.insert(extension) != 1) {
            throw new CrmebException("保存送花订单信息失败");
        }
        appendProgress(order.getOrderId(), FlowerOrderStageEnum.WAIT_PAY, null, null, "订单创建，等待付款");
        return extension;
    }

    @Override
    public void appendProgress(String orderNo, FlowerOrderStageEnum stage, Integer operatorId,
                               String proofImage, String remark) {
        FlowerOrderProgress progress = new FlowerOrderProgress()
                .setOrderId(orderNo).setStage(stage.name()).setOperatorId(operatorId)
                .setProofImage(trim(proofImage)).setRemark(trim(remark)).setCreateTime(new Date());
        if (progressDao.insert(progress) != 1) {
            throw new CrmebException("保存花店订单进度失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onPaySuccess(String orderNo) {
        if (orderExtDao.markPaidStage(orderNo) == 1) {
            appendProgress(orderNo, FlowerOrderStageEnum.WAIT_ACCEPT, null, null,
                    "支付成功，等待花店接单");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean transition(String orderNo, FlowerOrderTransitRequest request, Integer operatorId) {
        if (!StringUtils.hasText(orderNo) || request == null || request.getTargetStage() == null) {
            throw new CrmebException("订单号和目标阶段不能为空");
        }
        FlowerOrderExt extension = orderExtDao.selectByOrderIdForUpdate(orderNo);
        if (extension == null) {
            return false;
        }
        FlowerOrderStageEnum targetStage = request.getTargetStage();
        FlowerOrderStageEnum currentStage = parseStage(extension.getCurrentStage());
        if (currentStage.isSameStage(targetStage)) {
            return true;
        }
        if (!currentStage.canTransitTo(targetStage)) {
            throw new CrmebException("订单阶段不允许跳转");
        }
        if (operatorId != null && (targetStage == FlowerOrderStageEnum.WAIT_DELIVERY
                || targetStage == FlowerOrderStageEnum.COMPLETED)
                && !StringUtils.hasText(request.getProofImage())) {
            throw new CrmebException("请上传制作或送达凭证");
        }

        StoreOrder order = requireStoreOrder(orderNo);
        requireActiveOrder(order);
        if (targetStage == FlowerOrderStageEnum.WAIT_PRODUCTION && !Boolean.TRUE.equals(order.getPaid())) {
            throw new CrmebException("未支付订单不能接单");
        }
        if (targetStage == FlowerOrderStageEnum.DELIVERING) {
            shipOrder(orderNo, request);
        }
        if (targetStage == FlowerOrderStageEnum.PRODUCING && flowerMaterialService != null) {
            flowerMaterialService.consume(orderNo);
        }
        if (orderExtDao.updateStage(orderNo, targetStage.name(), nullableTrim(request.getCourierName()),
                nullableTrim(request.getCourierPhone()), nullableTrim(request.getRemark())) != 1) {
            throw new CrmebException("更新花店订单阶段失败");
        }
        appendProgress(orderNo, targetStage, operatorId, request.getProofImage(), request.getRemark());
        if (targetStage == FlowerOrderStageEnum.COMPLETED) {
            markAwaitingReview(order);
        }
        return true;
    }

    @Override
    public FlowerOrderDetailResponse detail(String orderNo, Integer currentUid) {
        FlowerOrderExt extension = orderExtDao.selectByOrderId(orderNo);
        StoreOrder order = requireOwnedOrder(orderNo, currentUid, "无权查看该订单");
        return buildDetail(extension, order);
    }

    @Override
    public List<FlowerOrderAdminListResponse> adminList(FlowerOrderAdminSearchRequest request) {
        FlowerOrderAdminSearchRequest safe = request == null ? new FlowerOrderAdminSearchRequest() : request;
        PageHelper.startPage(safe.getPage(), safe.getLimit());
        return orderExtDao.selectAdminList(trim(safe.getStage()), trim(safe.getKeyword()));
    }

    @Override
    public FlowerOrderDetailResponse adminDetail(String orderNo) {
        return buildDetail(orderExtDao.selectByOrderId(orderNo), requireStoreOrder(orderNo));
    }

    private FlowerOrderDetailResponse buildDetail(FlowerOrderExt extension, StoreOrder order) {
        if (extension == null) {
            throw new CrmebException("送花订单不存在");
        }
        List<FlowerOrderProgress> progressList = progressDao.selectByOrderId(order.getOrderId());
        if (progressList == null) progressList = Collections.emptyList();
        FlowerOrderDetailResponse response = new FlowerOrderDetailResponse();
        response.setOrderNo(order.getOrderId());
        response.setSenderName(extension.getSenderName());
        response.setSenderPhone(extension.getSenderPhone());
        response.setRecipientName(extension.getRecipientName());
        response.setRecipientPhone(extension.getRecipientPhone());
        response.setDeliveryAddress(extension.getDeliveryAddressSnapshot());
        response.setDeliveryDate(extension.getDeliveryDate());
        response.setSlotId(extension.getSlotId());
        if (flowerDeliverySlotDao != null && extension.getSlotId() != null) {
            FlowerDeliverySlot slot = flowerDeliverySlotDao.selectById(extension.getSlotId());
            if (slot != null) {
                response.setSlotStartTime(slot.getStartTime());
                response.setSlotEndTime(slot.getEndTime());
            }
        }
        response.setGreetingCard(extension.getGreetingCard());
        response.setAnonymous(extension.getAnonymous());
        response.setContactRecipient(extension.getContactRecipient());
        response.setSubstitutionPolicy(extension.getSubstitutionPolicy());
        response.setDeliveryRemark(extension.getDeliveryRemark());
        response.setCurrentStage(extension.getCurrentStage());
        response.setCourierName(extension.getCourierName());
        response.setCourierPhone(extension.getCourierPhone());
        response.setPayPrice(order.getPayPrice());
        response.setTotalNum(order.getTotalNum());
        List<StoreOrderInfo> products = storeOrderInfoService == null ? Collections.emptyList()
                : storeOrderInfoService.getListByOrderNo(order.getOrderId());
        if (products == null) products = Collections.emptyList();
        if (flowerProductExtDao != null) {
            for (StoreOrderInfo product : products) {
                FlowerProductExt productExt = flowerProductExtDao.selectOne(new LambdaQueryWrapper<FlowerProductExt>()
                        .eq(FlowerProductExt::getProductId, product.getProductId()).last("LIMIT 1"));
                if (productExt != null) {
                    product.setFlowerMaterials(productExt.getFlowerMaterials());
                    product.setStemCount(productExt.getStemCount());
                    product.setPackaging(productExt.getPackaging());
                }
            }
        }
        response.setProducts(products);
        response.setProgressList(progressList);
        for (FlowerOrderProgress progress : progressList) {
            if (FlowerOrderStageEnum.WAIT_DELIVERY.name().equals(progress.getStage())
                    && StringUtils.hasText(progress.getProofImage())) {
                response.setFinishedProductImage(progress.getProofImage());
            }
            if (FlowerOrderStageEnum.COMPLETED.name().equals(progress.getStage())
                    && StringUtils.hasText(progress.getProofImage())) {
                response.setDeliveryProofImage(progress.getProofImage());
            }
            if (FlowerOrderStageEnum.COMPLETED.name().equals(progress.getStage())) {
                response.setCompletedTime(progress.getCreateTime());
            }
        }
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmDelivery(String orderNo, Integer currentUid) {
        requireOwnedOrder(orderNo, currentUid, "无权操作该订单");
        FlowerOrderTransitRequest request = new FlowerOrderTransitRequest();
        request.setTargetStage(FlowerOrderStageEnum.COMPLETED);
        request.setRemark("顾客确认送达");
        return transition(orderNo, request, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void validateCancellation(String orderNo) {
        FlowerOrderExt extension = orderExtDao.selectByOrderIdForUpdate(orderNo);
        if (extension == null) {
            return;
        }
        requireActiveOrder(requireStoreOrder(orderNo));
        FlowerOrderStageEnum stage = parseStage(extension.getCurrentStage());
        if (stage.ordinal() >= FlowerOrderStageEnum.PRODUCING.ordinal()) {
            throw new CrmebException("订单已开始制作，请联系花店取消");
        }
    }

    private StoreOrder requireStoreOrder(String orderNo) {
        StoreOrder order = storeOrderService == null ? null : storeOrderService.getByOderId(orderNo);
        if (order == null) throw new CrmebException("原订单不存在");
        return order;
    }

    private StoreOrder requireOwnedOrder(String orderNo, Integer currentUid, String message) {
        StoreOrder order = requireStoreOrder(orderNo);
        if (currentUid == null || !currentUid.equals(order.getUid())) {
            throw new CrmebException(message);
        }
        return order;
    }

    private FlowerOrderStageEnum parseStage(String stage) {
        try {
            return FlowerOrderStageEnum.valueOf(stage);
        } catch (Exception e) {
            throw new CrmebException("花店订单阶段无效");
        }
    }

    private void shipOrder(String orderNo, FlowerOrderTransitRequest request) {
        if (!StringUtils.hasText(request.getCourierName()) || !StringUtils.hasText(request.getCourierPhone())) {
            throw new CrmebException("请填写配送人员和联系电话");
        }
        StoreOrderSendRequest sendRequest = new StoreOrderSendRequest();
        sendRequest.setOrderNo(orderNo);
        sendRequest.setDeliveryType("send");
        sendRequest.setDeliveryName(request.getCourierName().trim());
        sendRequest.setDeliveryTel(request.getCourierPhone().trim());
        storeOrderService.send(sendRequest);
    }

    private void markAwaitingReview(StoreOrder order) {
        if (orderService != null) {
            if (!Boolean.TRUE.equals(orderService.completeFlowerOrder(order.getOrderId()))) {
                throw new CrmebException("花店订单不存在或当前状态不适用");
            }
            return;
        }
        if (Integer.valueOf(Constants.ORDER_STATUS_INT_BARGAIN).equals(order.getStatus())) return;
        order.setStatus(Constants.ORDER_STATUS_INT_BARGAIN);
        order.setUpdateTime(new Date());
        if (!storeOrderService.updateById(order)) {
            throw new CrmebException("同步原订单待评价状态失败");
        }
    }

    private void requireActiveOrder(StoreOrder order) {
        if (Boolean.TRUE.equals(order.getIsDel()) || Boolean.TRUE.equals(order.getIsSystemDel())) {
            throw new CrmebException("订单已取消");
        }
    }

    private String nullableTrim(String value) {
        return value == null ? null : value.trim();
    }

    private void requireContact(String name, String phone, String label) {
        if (!StringUtils.hasText(name) || !StringUtils.hasText(phone)) {
            throw new CrmebException("请填写" + label + "姓名和手机号");
        }
        if (!phone.trim().matches("^1\\d{10}$")) {
            throw new CrmebException(label + "手机号格式不正确");
        }
    }

    private String addressSnapshot(UserAddress address) {
        return trim(address.getProvince()) + trim(address.getCity()) + trim(address.getDistrict()) + trim(address.getDetail());
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
