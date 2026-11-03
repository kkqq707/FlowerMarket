package com.zbkj.common.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class FlowerOrderSummaryResponse {
    private Long orderCount;
    private Long paidCount;
    private BigDecimal paidAmount;
    private BigDecimal pendingRefundAmount;
    private BigDecimal refundedAmount;
    private Map<String, Long> stageCounts;
}
