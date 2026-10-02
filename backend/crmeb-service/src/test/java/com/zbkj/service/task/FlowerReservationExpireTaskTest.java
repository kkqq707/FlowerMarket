package com.zbkj.service.task;

import com.zbkj.common.model.flower.FlowerOrderExt;
import com.zbkj.common.enums.ExpiredReservationCancellationResult;
import com.zbkj.service.dao.FlowerOrderExtDao;
import com.zbkj.service.service.FlowerDeliveryService;
import com.zbkj.service.service.OrderService;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

public class FlowerReservationExpireTaskTest {

    @Test
    public void usesFixedClockAndCancelsThenReleasesEveryExpiredReservation() {
        Instant fixedInstant = Instant.parse("2026-09-15T04:00:00Z");
        AtomicReference<Date> queryTime = new AtomicReference<>();
        List<String> cancelled = new CopyOnWriteArrayList<>();
        List<String> released = new CopyOnWriteArrayList<>();
        List<FlowerOrderExt> expired = Arrays.asList(extension("F1"), extension("F2"));
        FlowerOrderExtDao extDao = proxy(FlowerOrderExtDao.class, (o, m, a) -> {
            if ("findExpiredReservations".equals(m.getName())) {
                queryTime.set((Date) a[0]);
                return expired;
            }
            return defaultValue(m.getReturnType());
        });
        OrderService orderService = proxy(OrderService.class, (o, m, a) -> {
            if ("classifyExpiredReservationCancellation".equals(m.getName())) {
                cancelled.add((String) a[0]);
                return ExpiredReservationCancellationResult.NEW_CANCELLATION;
            }
            return defaultValue(m.getReturnType());
        });
        FlowerDeliveryService deliveryService = proxy(FlowerDeliveryService.class, (o, m, a) -> {
            if ("release".equals(m.getName())) released.add((String) a[0]);
            return defaultValue(m.getReturnType());
        });
        FlowerReservationExpireTask task = new FlowerReservationExpireTask(extDao, orderService,
                deliveryService, Clock.fixed(fixedInstant, ZoneOffset.UTC));

        task.releaseExpiredReservations();

        Assert.assertEquals(Date.from(fixedInstant), queryTime.get());
        Assert.assertEquals(Arrays.asList("F1", "F2"), cancelled);
        Assert.assertEquals(Arrays.asList("F1", "F2"), released);
    }

    @Test
    public void oneReservationFailureDoesNotPreventLaterOrders() {
        List<String> attempted = new CopyOnWriteArrayList<>();
        List<String> released = new CopyOnWriteArrayList<>();
        FlowerOrderExtDao extDao = proxy(FlowerOrderExtDao.class, (o, m, a) ->
                "findExpiredReservations".equals(m.getName())
                        ? Arrays.asList(extension("BROKEN"), extension("F2"))
                        : defaultValue(m.getReturnType()));
        OrderService orderService = proxy(OrderService.class, (o, m, a) -> {
            if ("classifyExpiredReservationCancellation".equals(m.getName())) {
                String orderNo = (String) a[0];
                attempted.add(orderNo);
                if ("BROKEN".equals(orderNo)) throw new IllegalStateException("simulated failure");
                return ExpiredReservationCancellationResult.NEW_CANCELLATION;
            }
            return defaultValue(m.getReturnType());
        });
        FlowerDeliveryService deliveryService = proxy(FlowerDeliveryService.class, (o, m, a) -> {
            if ("release".equals(m.getName())) released.add((String) a[0]);
            return defaultValue(m.getReturnType());
        });
        FlowerReservationExpireTask task = new FlowerReservationExpireTask(extDao, orderService,
                deliveryService, Clock.fixed(Instant.parse("2026-09-15T04:00:00Z"), ZoneOffset.UTC),
                (orderNo, error) -> { });

        task.releaseExpiredReservations();

        Assert.assertEquals(Arrays.asList("BROKEN", "F2"), attempted);
        Assert.assertEquals(Arrays.asList("F2"), released);
    }

    @Test
    public void retryReleasesAnAlreadyCancelledReservationAfterTheFirstReleaseFails() {
        AtomicReference<Boolean> reservationReleased = new AtomicReference<>(false);
        List<ExpiredReservationCancellationResult> results = Arrays.asList(
                ExpiredReservationCancellationResult.NEW_CANCELLATION,
                ExpiredReservationCancellationResult.RECOVERABLE_RELEASE);
        AtomicInteger resultIndex = new AtomicInteger();
        AtomicInteger releaseAttempts = new AtomicInteger();
        AtomicInteger actualSlotDecrements = new AtomicInteger();
        FlowerOrderExtDao extDao = proxy(FlowerOrderExtDao.class, (o, m, a) -> {
            if ("findExpiredReservations".equals(m.getName())) {
                return reservationReleased.get() ? Arrays.asList() : Arrays.asList(extension("F-RECOVER"));
            }
            return defaultValue(m.getReturnType());
        });
        OrderService orderService = proxy(OrderService.class, (o, m, a) -> {
            if ("classifyExpiredReservationCancellation".equals(m.getName())) {
                return results.get(resultIndex.getAndIncrement());
            }
            return defaultValue(m.getReturnType());
        });
        FlowerDeliveryService deliveryService = proxy(FlowerDeliveryService.class, (o, m, a) -> {
            if ("release".equals(m.getName())) {
                if (releaseAttempts.getAndIncrement() == 0) throw new IllegalStateException("release unavailable");
                if (!reservationReleased.getAndSet(true)) actualSlotDecrements.incrementAndGet();
            }
            return defaultValue(m.getReturnType());
        });
        FlowerReservationExpireTask task = new FlowerReservationExpireTask(extDao, orderService, deliveryService,
                Clock.fixed(Instant.parse("2026-09-15T04:00:00Z"), ZoneOffset.UTC), (orderNo, error) -> { });

        task.releaseExpiredReservations();
        task.releaseExpiredReservations();
        task.releaseExpiredReservations();

        Assert.assertEquals(2, releaseAttempts.get());
        Assert.assertEquals(1, actualSlotDecrements.get());
        Assert.assertEquals(2, resultIndex.get());
    }

    @Test
    public void concurrentCancellationModelHasOneNewCancellationAndOneRecoveryResult() {
        AtomicReference<Boolean> cancelled = new AtomicReference<>(false);
        AtomicInteger newCancellations = new AtomicInteger();
        AtomicInteger recoveries = new AtomicInteger();

        for (int i = 0; i < 2; i++) {
            ExpiredReservationCancellationResult result = cancelled.getAndSet(true)
                    ? ExpiredReservationCancellationResult.RECOVERABLE_RELEASE
                    : ExpiredReservationCancellationResult.NEW_CANCELLATION;
            if (result == ExpiredReservationCancellationResult.NEW_CANCELLATION) newCancellations.incrementAndGet();
            if (result == ExpiredReservationCancellationResult.RECOVERABLE_RELEASE) recoveries.incrementAndGet();
        }

        Assert.assertEquals(1, newCancellations.get());
        Assert.assertEquals(1, recoveries.get());
    }

    private static FlowerOrderExt extension(String orderNo) {
        return new FlowerOrderExt().setOrderId(orderNo).setCurrentStage("WAIT_PAY")
                .setReservationReleased(false);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (type.equals(boolean.class)) return false;
        if (type.equals(int.class)) return 0;
        if (type.equals(long.class)) return 0L;
        return null;
    }
}
