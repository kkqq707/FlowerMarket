package com.zbkj.service.service.impl;

import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.flower.FlowerRefundOutbox;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.service.dao.FlowerRefundOutboxDao;
import com.zbkj.service.service.FlowerRefundOutboxService;
import com.zbkj.service.service.StoreOrderService;
import com.zbkj.service.service.StoreOrderTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.List;

@Service
public class FlowerRefundOutboxServiceImpl implements FlowerRefundOutboxService {
    private static final Logger logger = LoggerFactory.getLogger(FlowerRefundOutboxServiceImpl.class);

    @Resource
    private FlowerRefundOutboxDao dao;
    @Autowired
    private StoreOrderService storeOrderService;
    @Autowired
    private StoreOrderTaskService storeOrderTaskService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public void dispatchPending(int limit) {
        List<Integer> ids = dao.findPendingIds(limit);
        for (Integer id : ids) {
            try {
                transactionTemplate.execute(status -> {
                    FlowerRefundOutbox task = dao.selectForUpdate(id);
                    if (task == null || Integer.valueOf(1).equals(task.getStatus())) {
                        return Boolean.TRUE;
                    }
                    StoreOrder order = storeOrderService.getById(task.getOrderId());
                    if (order == null) {
                        throw new CrmebException("退款后置任务对应订单不存在");
                    }
                    if (!Boolean.TRUE.equals(storeOrderTaskService.refundOrder(order))) {
                        throw new CrmebException("退款后置任务执行失败");
                    }
                    if (dao.markDone(id) != 1) {
                        throw new CrmebException("退款后置任务状态更新失败");
                    }
                    return Boolean.TRUE;
                });
            } catch (Exception exception) {
                logger.error("花店模拟退款后置任务执行失败，taskId={}", id, exception);
            }
        }
    }
}
