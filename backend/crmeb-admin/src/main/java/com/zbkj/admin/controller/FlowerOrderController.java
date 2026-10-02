package com.zbkj.admin.controller;

import com.zbkj.common.annotation.LogControllerAnnotation;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.enums.MethodType;
import com.zbkj.common.request.FlowerOrderTransitRequest;
import com.zbkj.common.request.flower.FlowerOrderAdminSearchRequest;
import com.zbkj.common.response.FlowerOrderAdminListResponse;
import com.zbkj.common.response.FlowerOrderDetailResponse;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.utils.SecurityUtil;
import com.zbkj.service.service.FlowerOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/admin/flower/order")
@Api(tags = "花店后台——订单履约")
public class FlowerOrderController {

    @Autowired
    private FlowerOrderService flowerOrderService;

    @PreAuthorize("hasAuthority('admin:flower:order:list')")
    @ApiOperation(value = "花店订单工作台列表")
    @GetMapping("/list")
    public CommonResult<CommonPage<FlowerOrderAdminListResponse>> list(@Validated FlowerOrderAdminSearchRequest request) {
        return CommonResult.success(CommonPage.restPage(flowerOrderService.adminList(request)));
    }

    @PreAuthorize("hasAuthority('admin:flower:order:info')")
    @ApiOperation(value = "花店订单履约详情")
    @GetMapping("/{orderNo}")
    public CommonResult<FlowerOrderDetailResponse> detail(@PathVariable String orderNo) {
        return CommonResult.success(flowerOrderService.adminDetail(orderNo));
    }

    @PreAuthorize("#request != null and #request.targetStage != null and ((#request.targetStage.name() == 'WAIT_PRODUCTION' and hasAuthority('admin:flower:order:accept'))"
            + " or ((#request.targetStage.name() == 'PRODUCING' or #request.targetStage.name() == 'WAIT_DELIVERY')"
            + " and hasAuthority('admin:flower:order:produce'))"
            + " or (#request.targetStage.name() == 'DELIVERING' and hasAuthority('admin:flower:order:deliver'))"
            + " or (#request.targetStage.name() == 'COMPLETED' and hasAuthority('admin:flower:order:complete')))")
    @LogControllerAnnotation(intoDB = true, methodType = MethodType.UPDATE, description = "更新花店订单履约阶段")
    @ApiOperation(value = "更新花店订单履约阶段")
    @PostMapping("/{orderNo}/transition")
    public CommonResult<String> transition(@PathVariable String orderNo,
                                           @RequestBody @Validated FlowerOrderTransitRequest request) {
        Integer operatorId = SecurityUtil.getLoginUserVo().getUser().getId();
        if (!flowerOrderService.transition(orderNo, request, operatorId)) {
            throw new CrmebException("花店订单不存在或当前状态不适用");
        }
        return CommonResult.success();
    }
}
