package com.zbkj.front.controller;

import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.flower.FlowerProductSearchRequest;
import com.zbkj.common.response.flower.FlowerProductCardResponse;
import com.zbkj.common.response.flower.FlowerProductDetailResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.FlowerProductService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/front/flower/product")
@Api(tags = "小程序——鲜花商品")
public class FlowerProductController {

    @Autowired
    private FlowerProductService flowerProductService;

    @ApiOperation(value = "鲜花商品列表与条件筛选")
    @GetMapping("/list")
    public CommonResult<CommonPage<FlowerProductCardResponse>> list(
            @Validated FlowerProductSearchRequest request) {
        return CommonResult.success(CommonPage.restPage(flowerProductService.search(request)));
    }

    @ApiOperation(value = "鲜花商品详情")
    @GetMapping("/{productId}")
    public CommonResult<FlowerProductDetailResponse> detail(@PathVariable Integer productId) {
        return CommonResult.success(flowerProductService.getFrontDetail(productId));
    }
}
