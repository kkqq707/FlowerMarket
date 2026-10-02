package com.zbkj.front.controller;

import com.zbkj.common.model.flower.FlowerDeliverySlot;
import com.zbkj.common.response.flower.FlowerDeliveryQuoteResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.FlowerDeliveryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("api/front/flower/delivery")
@Api(tags = "小程序——同城配送")
public class FlowerDeliveryController {

    @Autowired
    private FlowerDeliveryService deliveryService;

    @ApiOperation(value = "按收货地址计算配送范围、起送金额和配送费")
    @GetMapping("/quote")
    public CommonResult<FlowerDeliveryQuoteResponse> quote(@RequestParam Integer addressId,
                                                            @RequestParam BigDecimal goodsAmount) {
        return CommonResult.success(deliveryService.quote(addressId, goodsAmount));
    }

    @ApiOperation(value = "可预约配送时段")
    @GetMapping("/slots")
    public CommonResult<List<FlowerDeliverySlot>> slots(
            @RequestParam Integer zoneId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return CommonResult.success(deliveryService.availableSlots(zoneId, startDate, endDate));
    }
}
