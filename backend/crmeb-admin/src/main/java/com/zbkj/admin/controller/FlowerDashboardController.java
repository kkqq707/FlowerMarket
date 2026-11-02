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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

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

    @PreAuthorize("hasAuthority('admin:flower:dashboard:view')")
    @ApiOperation(value = "指定日期范围的销售趋势（最多31天）")
    @GetMapping("/sales-trend")
    public CommonResult<List<FlowerDashboardResponse.TrendItem>> salesTrend(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        return CommonResult.success(dashboardService.salesTrend(startDate, endDate));
    }

    @PreAuthorize("hasAuthority('admin:flower:dashboard:view')")
    @ApiOperation(value = "指定日期范围的热销花礼（按销量或销售额）")
    @GetMapping("/hot-products")
    public CommonResult<List<FlowerDashboardResponse.RankingItem>> hotProducts(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "sortBy", defaultValue = "quantity") String sortBy) {
        return CommonResult.success(dashboardService.hotProducts(startDate, endDate, sortBy));
    }
}
