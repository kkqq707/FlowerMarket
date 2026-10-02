package com.zbkj.admin.controller;

import com.zbkj.common.annotation.LogControllerAnnotation;
import com.zbkj.common.enums.MethodType;
import com.zbkj.common.model.flower.FlowerDeliverySlot;
import com.zbkj.common.model.flower.FlowerDeliveryZone;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.flower.FlowerDeliverySlotRequest;
import com.zbkj.common.request.flower.FlowerDeliveryZoneRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.FlowerDeliveryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/admin/flower/delivery")
@Api(tags = "花店后台——配送区域与预约时段")
public class FlowerDeliveryController {

    @Autowired
    private FlowerDeliveryService deliveryService;

    @PreAuthorize("hasAuthority('admin:flower:delivery:list')")
    @ApiOperation(value = "配送区域列表")
    @GetMapping("/zone/list")
    public CommonResult<CommonPage<FlowerDeliveryZone>> zoneList(@Validated PageParamRequest pageRequest) {
        return CommonResult.success(CommonPage.restPage(deliveryService.zoneList(pageRequest)));
    }

    @PreAuthorize("hasAuthority('admin:flower:delivery:save')")
    @LogControllerAnnotation(intoDB = true, methodType = MethodType.UPDATE, description = "保存花店配送区域")
    @ApiOperation(value = "新增或修改配送区域")
    @PostMapping("/zone")
    public CommonResult<FlowerDeliveryZone> saveZone(@RequestBody @Validated FlowerDeliveryZoneRequest request) {
        return CommonResult.success(deliveryService.saveZone(request));
    }

    @PreAuthorize("hasAuthority('admin:flower:delivery:status')")
    @ApiOperation(value = "启用或停用配送区域")
    @PostMapping("/zone/{id}/enabled")
    public CommonResult<String> zoneEnabled(@PathVariable Integer id, @RequestParam Boolean enabled) {
        deliveryService.setZoneEnabled(id, enabled);
        return CommonResult.success();
    }

    @PreAuthorize("hasAuthority('admin:flower:delivery:list')")
    @ApiOperation(value = "预约配送时段列表")
    @GetMapping("/slot/list")
    public CommonResult<CommonPage<FlowerDeliverySlot>> slotList(@Validated PageParamRequest pageRequest) {
        return CommonResult.success(CommonPage.restPage(deliveryService.slotList(pageRequest)));
    }

    @PreAuthorize("hasAuthority('admin:flower:delivery:save')")
    @LogControllerAnnotation(intoDB = true, methodType = MethodType.UPDATE, description = "保存鲜花配送时段")
    @ApiOperation(value = "新增或修改预约配送时段")
    @PostMapping("/slot")
    public CommonResult<FlowerDeliverySlot> saveSlot(@RequestBody @Validated FlowerDeliverySlotRequest request) {
        return CommonResult.success(deliveryService.saveSlot(request));
    }

    @PreAuthorize("hasAuthority('admin:flower:delivery:status')")
    @ApiOperation(value = "启用或停用预约配送时段")
    @PostMapping("/slot/{id}/enabled")
    public CommonResult<String> slotEnabled(@PathVariable Integer id, @RequestParam Boolean enabled) {
        deliveryService.setSlotEnabled(id, enabled);
        return CommonResult.success();
    }
}
