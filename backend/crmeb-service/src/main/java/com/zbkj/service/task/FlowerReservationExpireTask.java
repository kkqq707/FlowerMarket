package com.zbkj.service.task;

import com.zbkj.common.model.flower.FlowerOrderExt;
import com.zbkj.common.enums.ExpiredReservationCancellationResult;
import com.zbkj.service.dao.FlowerOrderExtDao;
import com.zbkj.service.service.FlowerDeliveryService;
import com.zbkj.service.service.FlowerMaterialService;
import com.zbkj.service.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;

@Component
public class FlowerReservationExpireTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowerReservationExpireTask.class);

    private final FlowerOrderExtDao orderExtDao;
    private final OrderService orderService;
    private final FlowerDeliveryService deliveryService;
    private final Clock clock;
    private final BiConsumer<String, Exception> failureHandler;

    @Autowired(required = false)
    private FlowerMaterialService flowerMaterialService;

    @Autowired
    public FlowerReservationExpireTask(FlowerOrderExtDao orderExtDao, OrderService orderService,
                                       FlowerDeliveryService deliveryService) {
        this(orderExtDao, orderService, deliveryService, Clock.systemDefaultZone(),
                (orderNo, error) -> LOGGER.error("释放过期送花预约失败，orderNo={}", orderNo, error));
    }

    public FlowerReservationExpireTask(FlowerOrderExtDao orderExtDao, OrderService orderService,
                                       FlowerDeliveryService deliveryService, Clock clock) {
        this(orderExtDao, orderService, deliveryService, clock,
                (orderNo, error) -> LOGGER.error("释放过期送花预约失败，orderNo={}", orderNo, error));
    }

    public FlowerReservationExpireTask(FlowerOrderExtDao orderExtDao, OrderService orderService,
                                       FlowerDeliveryService deliveryService, Clock clock,
                                       BiConsumer<String, Exception> failureHandler) {
        this.orderExtDao = orderExtDao;
        this.orderService = orderService;
        this.deliveryService = deliveryService;
        this.clock = clock;
        this.failureHandler = failureHandler;
    }

    @Scheduled(fixedDelay = 60_000L)
    public void releaseExpiredReservations() {
        List<FlowerOrderExt> expired = orderExtDao.findExpiredReservations(Date.from(clock.instant()));
        if (expired == null) expired = Collections.emptyList();
        for (FlowerOrderExt extension : expired) {
            String orderNo = extension.getOrderId();
            try {
                ExpiredReservationCancellationResult result = orderService
                        .classifyExpiredReservationCancellation(orderNo);
                if (result != null && result.shouldReleaseReservation()) {
                    if (flowerMaterialService != null) flowerMaterialService.release(orderNo);
                    deliveryService.release(orderNo);
                }
            } catch (Exception e) {
                failureHandler.accept(orderNo, e);
            }
        }
    }
}
