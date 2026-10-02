package com.zbkj.admin.controller;

import com.zbkj.common.annotation.LogControllerAnnotation;
import com.zbkj.common.enums.MethodType;
import com.zbkj.common.model.flower.FlowerProductExt;
import com.zbkj.common.request.flower.FlowerProductExtRequest;
import com.zbkj.common.response.flower.FlowerProductDetailResponse;
import com.zbkj.common.response.flower.FlowerProductCardResponse;
import com.zbkj.common.request.flower.FlowerProductSearchRequest;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.FlowerProductService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/admin/flower/product")
@Api(tags = "花店后台——鲜花商品资料")
public class FlowerProductController {

    @Autowired
    private FlowerProductService flowerProductService;

    @PreAuthorize("hasAuthority('admin:flower:product:info')")
    @ApiOperation(value = "查看鲜花商品资料")
    @GetMapping("/{productId}")
    public CommonResult<FlowerProductDetailResponse> detail(@PathVariable Integer productId) {
        return CommonResult.success(flowerProductService.getAdminDetail(productId));
    }

    @PreAuthorize("hasAuthority('admin:flower:product:save')")
    @LogControllerAnnotation(intoDB = true, methodType = MethodType.ADD, description = "保存鲜花商品资料")
    @ApiOperation(value = "新增或修改鲜花商品资料")
    @PostMapping("/{productId}")
    public CommonResult<FlowerProductExt> save(@PathVariable Integer productId,
                                                @RequestBody @Validated FlowerProductExtRequest request) {
        return CommonResult.success(flowerProductService.saveOrUpdate(productId, request));
    }

    @PreAuthorize("hasAuthority('admin:product:list')")
    @ApiOperation(value = "鲜花商品管理列表")
    @GetMapping("/list")
    public CommonResult<CommonPage<FlowerProductCardResponse>> list(@Validated FlowerProductSearchRequest request) {
        return CommonResult.success(CommonPage.restPage(flowerProductService.searchAdmin(request)));
    }
}
