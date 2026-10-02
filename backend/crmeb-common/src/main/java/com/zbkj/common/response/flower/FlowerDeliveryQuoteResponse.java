package com.zbkj.common.response.flower;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class FlowerDeliveryQuoteResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer zoneId;
    private String zoneName;
    private BigDecimal deliveryFee;
    private BigDecimal minimumAmount;
}
