package com.zbkj.service.service.impl;

import com.github.pagehelper.PageHelper;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.flower.FlowerDeliverySlot;
import com.zbkj.common.model.flower.FlowerDeliveryZone;
import com.zbkj.common.model.flower.FlowerOrderExt;
import com.zbkj.common.model.user.UserAddress;
import com.zbkj.common.model.user.User;
import com.zbkj.common.model.system.SystemCity;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.flower.FlowerDeliverySlotRequest;
import com.zbkj.common.request.flower.FlowerDeliveryZoneRequest;
import com.zbkj.common.response.flower.FlowerDeliveryQuoteResponse;
import com.zbkj.service.dao.FlowerDeliverySlotDao;
import com.zbkj.service.dao.FlowerDeliveryZoneDao;
import com.zbkj.service.dao.FlowerOrderExtDao;
import com.zbkj.service.service.FlowerDeliveryService;
import com.zbkj.service.service.UserAddressService;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.SystemCityService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;

@Service
public class FlowerDeliveryServiceImpl implements FlowerDeliveryService {

    private final FlowerDeliveryZoneDao zoneDao;
    private final FlowerDeliverySlotDao slotDao;
    private final FlowerOrderExtDao orderExtDao;
    private final UserAddressService addressService;
    private final UserService userService;
    private final SystemCityService systemCityService;

    public FlowerDeliveryServiceImpl(FlowerDeliveryZoneDao zoneDao,
                                     FlowerDeliverySlotDao slotDao,
                                     FlowerOrderExtDao orderExtDao,
                                     UserAddressService addressService,
                                     UserService userService,
                                     SystemCityService systemCityService) {
        this.zoneDao = zoneDao;
        this.slotDao = slotDao;
        this.orderExtDao = orderExtDao;
        this.addressService = addressService;
        this.userService = userService;
        this.systemCityService = systemCityService;
    }

    @Override
    public FlowerDeliveryQuoteResponse quote(Integer addressId, BigDecimal goodsAmount) {
        UserAddress address = addressId == null ? null : addressService.getById(addressId);
        User currentUser = userService.getInfoException();
        if (address == null || Boolean.TRUE.equals(address.getIsDel()) || currentUser == null
                || !currentUser.getUid().equals(address.getUid())) {
            throw new CrmebException("配送地址不存在");
        }
        SystemCity city = address.getCityId() == null ? null : systemCityService.getCityByCityId(address.getCityId());
        String areaCode = city == null ? null : city.getAreaCode();
        FlowerDeliveryZone zone = StringUtils.hasText(areaCode) ? zoneDao.findEnabledByAreaCode(areaCode) : null;
        if (zone == null) {
            throw new CrmebException("该地址暂不在本店配送范围内");
        }
        BigDecimal safeAmount = goodsAmount == null ? BigDecimal.ZERO : goodsAmount;
        BigDecimal minimum = zone.getMinimumAmount() == null ? BigDecimal.ZERO : zone.getMinimumAmount();
        if (safeAmount.compareTo(minimum) < 0) {
            throw new CrmebException("商品金额未达到该区域起送金额");
        }
        FlowerDeliveryQuoteResponse response = new FlowerDeliveryQuoteResponse();
        response.setZoneId(zone.getId());
        response.setZoneName(zone.getZoneName());
        response.setDeliveryFee(zone.getDeliveryFee() == null ? BigDecimal.ZERO : zone.getDeliveryFee());
        response.setMinimumAmount(minimum);
        return response;
    }

    @Override
    public List<FlowerDeliverySlot> availableSlots(Integer zoneId, Date startDate, Date endDate) {
        FlowerDeliveryZone zone = zoneId == null ? null : zoneDao.selectById(zoneId);
        if (zone == null || !Boolean.TRUE.equals(zone.getEnabled())) {
            throw new CrmebException("配送区域不存在或已停用");
        }
        if (startDate == null || endDate == null || startDate.after(endDate)) {
            throw new CrmebException("配送日期范围不正确");
        }
        return slotDao.findAvailable(startDate, endDate);
    }

    @Override
    public FlowerDeliverySlot validateSelection(Integer zoneId, Date deliveryDate, Integer slotId) {
        FlowerDeliveryZone zone = zoneId == null ? null : zoneDao.selectById(zoneId);
        if (zone == null || !Boolean.TRUE.equals(zone.getEnabled())) {
            throw new CrmebException("配送区域不存在或已停用");
        }
        FlowerDeliverySlot slot = slotId == null ? null : slotDao.selectById(slotId);
        if (slot == null || !Boolean.TRUE.equals(slot.getEnabled()) || slot.getCutoffTime() == null
                || !slot.getCutoffTime().after(new Date()) || slot.getCapacity() == null
                || slot.getBookedCount() == null || slot.getBookedCount() >= slot.getCapacity()) {
            throw new CrmebException("该配送时段已满或已停止预约");
        }
        if (deliveryDate == null || slot.getDeliveryDate() == null
                || !new SimpleDateFormat("yyyy-MM-dd").format(deliveryDate)
                .equals(new SimpleDateFormat("yyyy-MM-dd").format(slot.getDeliveryDate()))) {
            throw new CrmebException("配送日期与预约时段不一致");
        }
        return slot;
    }

    @Override
    public void reserve(Integer slotId) {
        if (slotId == null || slotDao.reserve(slotId) != 1) {
            throw new CrmebException("该配送时段已满或已停止预约");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void release(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return;
        }
        FlowerOrderExt extension = orderExtDao.selectByOrderId(orderNo);
        if (extension == null || extension.getSlotId() == null) {
            return;
        }
        if (orderExtDao.markReservationReleased(orderNo) == 1) {
            slotDao.release(extension.getSlotId());
        }
    }

    @Override
    public List<FlowerDeliveryZone> zoneList(PageParamRequest pageRequest) {
        PageParamRequest page = pageRequest == null ? new PageParamRequest() : pageRequest;
        PageHelper.startPage(page.getPage(), page.getLimit());
        return zoneDao.selectList(null);
    }

    @Override
    public List<FlowerDeliverySlot> slotList(PageParamRequest pageRequest) {
        PageParamRequest page = pageRequest == null ? new PageParamRequest() : pageRequest;
        PageHelper.startPage(page.getPage(), page.getLimit());
        return slotDao.selectList(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowerDeliveryZone saveZone(FlowerDeliveryZoneRequest request) {
        if (request == null || !StringUtils.hasText(request.getZoneName()) || !StringUtils.hasText(request.getAreaCodes())) {
            throw new CrmebException("配送区域资料不完整");
        }
        FlowerDeliveryZone zone = request.getId() == null ? new FlowerDeliveryZone() : zoneDao.selectById(request.getId());
        if (zone == null) throw new CrmebException("配送区域不存在");
        boolean creating = zone.getId() == null;
        BeanUtils.copyProperties(request, zone);
        zone.setZoneName(request.getZoneName().trim());
        zone.setAreaCodes(normalizeCodes(request.getAreaCodes()));
        zone.setEnabled(request.getEnabled() == null || request.getEnabled());
        zone.setUpdateTime(new Date());
        if (creating) {
            zone.setCreateTime(new Date());
            zoneDao.insert(zone);
        } else {
            zoneDao.updateById(zone);
        }
        return zone;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowerDeliverySlot saveSlot(FlowerDeliverySlotRequest request) {
        if (request == null || request.getDeliveryDate() == null || request.getStartTime() == null
                || request.getEndTime() == null || request.getCutoffTime() == null || request.getCapacity() == null) {
            throw new CrmebException("配送时段资料不完整");
        }
        if (!request.getStartTime().before(request.getEndTime())) {
            throw new CrmebException("结束时间必须晚于开始时间");
        }
        FlowerDeliverySlot slot = request.getId() == null ? new FlowerDeliverySlot() : slotDao.selectById(request.getId());
        if (slot == null) throw new CrmebException("配送时段不存在");
        if (slot.getBookedCount() != null && request.getCapacity() < slot.getBookedCount()) {
            throw new CrmebException("容量不能小于当前已预约数量");
        }
        boolean creating = slot.getId() == null;
        BeanUtils.copyProperties(request, slot);
        slot.setEnabled(request.getEnabled() == null || request.getEnabled());
        slot.setUpdateTime(new Date());
        if (creating) {
            slot.setBookedCount(0).setVersion(0).setCreateTime(new Date());
            slotDao.insert(slot);
        } else {
            if (slotDao.updateAdminFields(slot) != 1) {
                throw new CrmebException("容量不能小于当前已预约数量，请刷新后重试");
            }
        }
        return slot;
    }

    @Override
    public void setZoneEnabled(Integer id, Boolean enabled) {
        FlowerDeliveryZone zone = zoneDao.selectById(id);
        if (zone == null) throw new CrmebException("配送区域不存在");
        zone.setEnabled(Boolean.TRUE.equals(enabled)).setUpdateTime(new Date());
        zoneDao.updateById(zone);
    }

    @Override
    public void setSlotEnabled(Integer id, Boolean enabled) {
        if (slotDao.updateEnabled(id, Boolean.TRUE.equals(enabled)) != 1) {
            throw new CrmebException("配送时段不存在");
        }
    }

    private String normalizeCodes(String value) {
        return Arrays.stream(value.replace('，', ',').split(","))
                .map(String::trim).filter(StringUtils::hasText).distinct()
                .collect(Collectors.joining(","));
    }
}
