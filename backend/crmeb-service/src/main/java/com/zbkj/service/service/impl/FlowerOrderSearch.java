package com.zbkj.service.service.impl;

import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.request.flower.FlowerOrderAdminSearchRequest;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

/** Validates the public filter vocabulary before creating a database query. */
public final class FlowerOrderSearch {
    private FlowerOrderSearch() {}
    public static FlowerOrderAdminSearchRequest normalize(FlowerOrderAdminSearchRequest value) {
        FlowerOrderAdminSearchRequest q = value == null ? new FlowerOrderAdminSearchRequest() : value;
        q.setStage(text(q.getStage())); q.setKeyword(text(q.getKeyword()));
        q.setRefundState(text(q.getRefundState()));
        q.setTimeType(text(q.getTimeType()).isEmpty() ? "created" : text(q.getTimeType()));
        q.setStartDate(text(q.getStartDate())); q.setEndDate(text(q.getEndDate()));
        if (!Arrays.asList("created","paid","refund","refunded").contains(q.getTimeType())) throw new CrmebException("请选择有效的时间口径");
        if (!Arrays.asList("","WAIT_PAY","WAIT_ACCEPT","WAIT_PRODUCTION","PRODUCING","PRODUCTION","WAIT_DELIVERY","DELIVERING","DELIVERY","COMPLETED","CANCELLED","CLOSED","AFTER_SALE").contains(q.getStage())) throw new CrmebException("订单状态无效");
        if (!Arrays.asList("","none","pending","partial","done").contains(q.getRefundState())) throw new CrmebException("退款状态无效");
        if (q.getKeyword().length()>100) throw new CrmebException("搜索内容不能超过100字");
        if (!q.getStartDate().isEmpty() || !q.getEndDate().isEmpty()) {
            try {
                if (!q.getStartDate().matches("\\d{4}-\\d{2}-\\d{2}") || !q.getEndDate().matches("\\d{4}-\\d{2}-\\d{2}")) throw new IllegalArgumentException();
                LocalDate start=LocalDate.parse(q.getStartDate()),end=LocalDate.parse(q.getEndDate());
                if (end.isBefore(start) || ChronoUnit.DAYS.between(start,end)>365) throw new IllegalArgumentException();
            } catch (Exception error) { throw new CrmebException("请选择有效的起止日期，范围最多366天"); }
        }
        return q;
    }
    private static String text(String v) { return v == null ? "" : v.trim(); }
}
