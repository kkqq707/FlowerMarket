package com.zbkj.common.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class FlowerOrderAdminListResponse {
    private String orderNo;
    private String currentStage;
    private String recipientName;
    private String recipientPhone;
    private String deliveryAddress;
    private Date deliveryDate;
    private Integer slotId;
    private BigDecimal payPrice;
    private Integer totalNum;
    private Boolean paid;
    private Integer refundStatus;
    private Date createTime;
    private String orderState;
    private Date payTime;
    private Date refundApplyTime;
    private Date refundCompletedTime;
    private BigDecimal refundPrice;
    private BigDecimal totalPrice;
    private String productName;
    private String productImage;
    private String refundReason;
}
