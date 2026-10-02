package com.zbkj.front.controller;

import com.zbkj.common.response.FlowerOrderDetailResponse;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.FlowerOrderService;
import com.zbkj.service.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/front/flower/order")
@Api(tags = "小程序——送花订单履约")
public class FlowerOrderController {

    @Autowired
    private FlowerOrderService flowerOrderService;

    @Autowired
    private UserService userService;

    @ApiOperation(value = "送花订单履约详情")
    @GetMapping("/{orderNo}")
    public CommonResult<FlowerOrderDetailResponse> detail(@PathVariable String orderNo) {
        return CommonResult.success(flowerOrderService.detail(orderNo, userService.getUserIdException()));
    }

    @ApiOperation(value = "顾客确认送达")
    @PostMapping("/{orderNo}/confirm")
    public CommonResult<String> confirm(@PathVariable String orderNo) {
        if (!flowerOrderService.confirmDelivery(orderNo, userService.getUserIdException())) {
            throw new CrmebException("花店订单不存在或当前状态不适用");
        }
        return CommonResult.success();
    }
}
