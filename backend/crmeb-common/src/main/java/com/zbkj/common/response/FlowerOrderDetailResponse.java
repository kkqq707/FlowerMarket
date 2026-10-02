package com.zbkj.common.response;

import com.zbkj.common.model.flower.FlowerOrderProgress;
import com.zbkj.common.model.order.StoreOrderInfo;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.Date;
import java.math.BigDecimal;
import java.util.List;

@Data
@ApiModel(value = "FlowerOrderDetailResponse", description = "送花订单履约详情")
public class FlowerOrderDetailResponse {
    private String orderNo;
    private String senderName;
    private String senderPhone;
    private String recipientName;
    private String recipientPhone;
    private String deliveryAddress;
    private Date deliveryDate;
    private Integer slotId;
    private Date slotStartTime;
    private Date slotEndTime;
    private String greetingCard;
    private Boolean anonymous;
    private Boolean contactRecipient;
    private String substitutionPolicy;
    private String deliveryRemark;
    private String currentStage;
    private String courierName;
    private String courierPhone;
    private String finishedProductImage;
    private String deliveryProofImage;
    private Date completedTime;
    private BigDecimal payPrice;
    private Integer totalNum;
    private List<StoreOrderInfo> products;
    private List<FlowerOrderProgress> progressList;
}
