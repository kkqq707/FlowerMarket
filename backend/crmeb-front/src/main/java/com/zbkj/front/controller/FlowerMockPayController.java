package com.zbkj.front.controller;

import com.zbkj.common.response.OrderPayResultResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.FlowerMockPayService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/front/flower/pay")
@Api(tags = "小程序——花店开发演示支付")
public class FlowerMockPayController {

    @Autowired
    private FlowerMockPayService mockPayService;

    @ApiOperation(value = "模拟微信支付（仅开发演示环境可用）")
    @PostMapping("/mock/{orderNo}")
    public CommonResult<OrderPayResultResponse> mockPay(@PathVariable String orderNo) {
        return CommonResult.success(mockPayService.pay(orderNo));
    }
}
