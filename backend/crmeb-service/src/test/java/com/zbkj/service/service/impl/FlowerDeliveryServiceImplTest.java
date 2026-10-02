package com.zbkj.service.service.impl;

import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.flower.FlowerDeliveryZone;
import com.zbkj.common.model.flower.FlowerDeliverySlot;
import com.zbkj.common.model.flower.FlowerOrderExt;
import com.zbkj.common.model.user.UserAddress;
import com.zbkj.common.response.flower.FlowerDeliveryQuoteResponse;
import com.zbkj.service.dao.FlowerDeliverySlotDao;
import com.zbkj.service.dao.FlowerDeliveryZoneDao;
import com.zbkj.service.dao.FlowerOrderExtDao;
import com.zbkj.service.service.UserAddressService;
import com.zbkj.service.service.UserService;
import com.zbkj.common.model.user.User;
import com.zbkj.common.model.system.SystemCity;
import com.zbkj.service.service.SystemCityService;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicInteger;

public class FlowerDeliveryServiceImplTest {

    @Test(expected = CrmebException.class)
    public void reserveRejectsFullOrClosedSlot() {
        FlowerDeliveryServiceImpl service = service(
                proxy(FlowerDeliveryZoneDao.class, (o, m, a) -> null),
                proxy(FlowerDeliverySlotDao.class, (o, m, a) -> "reserve".equals(m.getName()) ? 0 : defaultValue(m.getReturnType())),
                proxy(FlowerOrderExtDao.class, (o, m, a) -> null),
                proxy(UserAddressService.class, (o, m, a) -> null));

        service.reserve(8);
    }

    @Test
    public void reserveSucceedsOnlyWhenAtomicUpdateChangesOneRow() {
        FlowerDeliveryServiceImpl service = service(
                proxy(FlowerDeliveryZoneDao.class, (o, m, a) -> null),
                proxy(FlowerDeliverySlotDao.class, (o, m, a) -> "reserve".equals(m.getName()) ? 1 : defaultValue(m.getReturnType())),
                proxy(FlowerOrderExtDao.class, (o, m, a) -> null),
                proxy(UserAddressService.class, (o, m, a) -> null));

        service.reserve(8);
    }

    @Test(expected = CrmebException.class)
    public void quoteRejectsAmountBelowZoneMinimum() {
        UserAddress address = new UserAddress().setId(3).setUid(1).setCityId(440106).setIsDel(false);
        FlowerDeliveryZone zone = new FlowerDeliveryZone().setId(2).setEnabled(true)
                .setMinimumAmount(new BigDecimal("99")).setDeliveryFee(new BigDecimal("12"));
        FlowerDeliveryServiceImpl service = service(
                proxy(FlowerDeliveryZoneDao.class, (o, m, a) -> "findEnabledByAreaCode".equals(m.getName()) ? zone : null),
                proxy(FlowerDeliverySlotDao.class, (o, m, a) -> defaultValue(m.getReturnType())),
                proxy(FlowerOrderExtDao.class, (o, m, a) -> null),
                proxy(UserAddressService.class, (o, m, a) -> "getById".equals(m.getName()) ? address : defaultValue(m.getReturnType())));

        service.quote(3, new BigDecimal("88"));
    }

    @Test
    public void quoteReturnsZoneAndFeeForCoveredAddress() {
        UserAddress address = new UserAddress().setId(3).setUid(1).setCityId(440106).setIsDel(false);
        FlowerDeliveryZone zone = new FlowerDeliveryZone().setId(2).setZoneName("天河区")
                .setEnabled(true).setMinimumAmount(new BigDecimal("99")).setDeliveryFee(new BigDecimal("12"));
        FlowerDeliveryServiceImpl service = service(
                proxy(FlowerDeliveryZoneDao.class, (o, m, a) -> "findEnabledByAreaCode".equals(m.getName()) ? zone : null),
                proxy(FlowerDeliverySlotDao.class, (o, m, a) -> defaultValue(m.getReturnType())),
                proxy(FlowerOrderExtDao.class, (o, m, a) -> null),
                proxy(UserAddressService.class, (o, m, a) -> "getById".equals(m.getName()) ? address : defaultValue(m.getReturnType())));

        FlowerDeliveryQuoteResponse quote = service.quote(3, new BigDecimal("188"));

        Assert.assertEquals(Integer.valueOf(2), quote.getZoneId());
        Assert.assertEquals("天河区", quote.getZoneName());
        Assert.assertEquals(new BigDecimal("12"), quote.getDeliveryFee());
    }

    @Test
    public void quoteUsesAdministrativeAreaCodeInsteadOfCityIdentifier() {
        UserAddress address = new UserAddress().setId(3).setUid(1).setCityId(123).setIsDel(false);
        FlowerDeliveryZone zone = new FlowerDeliveryZone().setId(2).setZoneName("天河区")
                .setEnabled(true).setMinimumAmount(BigDecimal.ZERO).setDeliveryFee(new BigDecimal("12"));
        FlowerDeliveryZoneDao zoneDao = proxy(FlowerDeliveryZoneDao.class, (o, m, a) -> {
            if ("findEnabledByAreaCode".equals(m.getName()) && "440106".equals(a[0])) return zone;
            return null;
        });
        SystemCity city = new SystemCity().setCityId(123).setAreaCode("440106");
        SystemCityService cityService = proxy(SystemCityService.class, (o, m, a) ->
                "getCityByCityId".equals(m.getName()) ? city : defaultValue(m.getReturnType()));
        FlowerDeliveryServiceImpl service = service(zoneDao,
                proxy(FlowerDeliverySlotDao.class, (o, m, a) -> defaultValue(m.getReturnType())),
                proxy(FlowerOrderExtDao.class, (o, m, a) -> null),
                proxy(UserAddressService.class, (o, m, a) -> "getById".equals(m.getName()) ? address : null),
                cityService);

        FlowerDeliveryQuoteResponse quote = service.quote(3, new BigDecimal("188"));

        Assert.assertEquals(Integer.valueOf(2), quote.getZoneId());
    }

    @Test(expected = CrmebException.class)
    public void quoteRejectsAddressOwnedByAnotherUser() {
        UserAddress address = new UserAddress().setId(3).setUid(99).setCityId(440106).setIsDel(false);
        FlowerDeliveryServiceImpl service = service(
                proxy(FlowerDeliveryZoneDao.class, (o, m, a) -> null),
                proxy(FlowerDeliverySlotDao.class, (o, m, a) -> defaultValue(m.getReturnType())),
                proxy(FlowerOrderExtDao.class, (o, m, a) -> null),
                proxy(UserAddressService.class, (o, m, a) -> "getById".equals(m.getName()) ? address : null));

        service.quote(3, new BigDecimal("188"));
    }

    @Test(expected = CrmebException.class)
    public void quoteRejectsAddressOutsideDeliveryZone() {
        UserAddress address = new UserAddress().setId(3).setUid(1).setCityId(440106).setIsDel(false);
        FlowerDeliveryServiceImpl service = service(
                proxy(FlowerDeliveryZoneDao.class, (o, m, a) -> null),
                proxy(FlowerDeliverySlotDao.class, (o, m, a) -> defaultValue(m.getReturnType())),
                proxy(FlowerOrderExtDao.class, (o, m, a) -> null),
                proxy(UserAddressService.class, (o, m, a) -> "getById".equals(m.getName()) ? address : null));

        service.quote(3, new BigDecimal("188"));
    }

    @Test
    public void releaseTwiceDecreasesCapacityOnlyOnce() {
        AtomicInteger marks = new AtomicInteger();
        AtomicInteger releases = new AtomicInteger();
        FlowerOrderExt extension = new FlowerOrderExt().setOrderId("F100").setSlotId(9).setReservationReleased(false);
        FlowerDeliveryServiceImpl service = service(
                proxy(FlowerDeliveryZoneDao.class, (o, m, a) -> null),
                proxy(FlowerDeliverySlotDao.class, (o, m, a) -> {
                    if ("release".equals(m.getName())) {
                        releases.incrementAndGet();
                        return 1;
                    }
                    return defaultValue(m.getReturnType());
                }),
                proxy(FlowerOrderExtDao.class, (o, m, a) -> {
                    if ("selectByOrderId".equals(m.getName())) return extension;
                    if ("markReservationReleased".equals(m.getName())) return marks.getAndIncrement() == 0 ? 1 : 0;
                    return defaultValue(m.getReturnType());
                }),
                proxy(UserAddressService.class, (o, m, a) -> null));

        service.release("F100");
        service.release("F100");

        Assert.assertEquals(1, releases.get());
    }

    @Test(expected = CrmebException.class)
    public void validateSelectionRejectsSlotFromAnotherDeliveryDate() throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        FlowerDeliverySlot slot = new FlowerDeliverySlot().setId(9).setEnabled(true)
                .setDeliveryDate(format.parse("2026-09-16 00:00:00"))
                .setCutoffTime(format.parse("2099-09-15 08:00:00"))
                .setCapacity(10).setBookedCount(2);
        FlowerDeliveryServiceImpl service = service(
                proxy(FlowerDeliveryZoneDao.class, (o, m, a) -> null),
                proxy(FlowerDeliverySlotDao.class, (o, m, a) -> "selectById".equals(m.getName()) ? slot : defaultValue(m.getReturnType())),
                proxy(FlowerOrderExtDao.class, (o, m, a) -> null),
                proxy(UserAddressService.class, (o, m, a) -> null));

        service.validateSelection(2, format.parse("2026-09-17 00:00:00"), 9);
    }

    @Test(expected = CrmebException.class)
    public void validateSelectionRejectsMissingDeliveryZone() throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        FlowerDeliverySlot slot = new FlowerDeliverySlot().setId(9).setEnabled(true)
                .setDeliveryDate(format.parse("2099-09-16 00:00:00"))
                .setCutoffTime(format.parse("2099-09-15 08:00:00"))
                .setCapacity(10).setBookedCount(2);
        FlowerDeliveryServiceImpl service = service(
                proxy(FlowerDeliveryZoneDao.class, (o, m, a) -> null),
                proxy(FlowerDeliverySlotDao.class, (o, m, a) -> "selectById".equals(m.getName()) ? slot : defaultValue(m.getReturnType())),
                proxy(FlowerOrderExtDao.class, (o, m, a) -> null),
                proxy(UserAddressService.class, (o, m, a) -> null));

        service.validateSelection(999, format.parse("2099-09-16 00:00:00"), 9);
    }

    @Test(expected = CrmebException.class)
    public void validateSelectionRejectsExpiredCutoffTime() throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        FlowerDeliveryZone zone = new FlowerDeliveryZone().setId(2).setEnabled(true);
        FlowerDeliverySlot slot = new FlowerDeliverySlot().setId(9).setEnabled(true)
                .setDeliveryDate(format.parse("2099-09-16 00:00:00"))
                .setCutoffTime(format.parse("2020-09-15 08:00:00"))
                .setCapacity(10).setBookedCount(2);
        FlowerDeliveryServiceImpl service = service(
                proxy(FlowerDeliveryZoneDao.class, (o, m, a) -> "selectById".equals(m.getName()) ? zone : null),
                proxy(FlowerDeliverySlotDao.class, (o, m, a) -> "selectById".equals(m.getName()) ? slot : defaultValue(m.getReturnType())),
                proxy(FlowerOrderExtDao.class, (o, m, a) -> null),
                proxy(UserAddressService.class, (o, m, a) -> null));

        service.validateSelection(2, format.parse("2099-09-16 00:00:00"), 9);
    }

    private static FlowerDeliveryServiceImpl service(FlowerDeliveryZoneDao zoneDao,
                                                      FlowerDeliverySlotDao slotDao,
                                                      FlowerOrderExtDao orderExtDao,
                                                      UserAddressService addressService) {
        SystemCityService cityService = proxy(SystemCityService.class, (o, m, a) -> {
            if ("getCityByCityId".equals(m.getName()) && a != null && a.length == 1 && a[0] != null) {
                return new SystemCity().setCityId((Integer) a[0]).setAreaCode(String.valueOf(a[0]));
            }
            return defaultValue(m.getReturnType());
        });
        return service(zoneDao, slotDao, orderExtDao, addressService, cityService);
    }

    private static FlowerDeliveryServiceImpl service(FlowerDeliveryZoneDao zoneDao,
                                                      FlowerDeliverySlotDao slotDao,
                                                      FlowerOrderExtDao orderExtDao,
                                                      UserAddressService addressService,
                                                      SystemCityService cityService) {
        User currentUser = new User().setUid(1);
        UserService userService = proxy(UserService.class, (o, m, a) ->
                "getInfoException".equals(m.getName()) ? currentUser : defaultValue(m.getReturnType()));
        return new FlowerDeliveryServiceImpl(zoneDao, slotDao, orderExtDao, addressService, userService, cityService);
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
