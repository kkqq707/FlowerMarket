package com.zbkj.admin.controller;

import com.zbkj.common.response.FlowerDashboardResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.FlowerDashboardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/admin/flower/dashboard")
@Api(tags = "花店后台——经营看板")
public class FlowerDashboardController {
    @Autowired
    private FlowerDashboardService dashboardService;

    @PreAuthorize("hasAuthority('admin:flower:dashboard:view')")
    @ApiOperation(value = "今日经营数据")
    @GetMapping
    public CommonResult<FlowerDashboardResponse> dashboard() {
        return CommonResult.success(dashboardService.today());
    }
}
