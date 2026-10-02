package com.zbkj.front.controller;

import com.zbkj.common.constants.SysConfigConstants;
import com.zbkj.common.response.flower.FlowerStoreConfigResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.SystemConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/front/flower/store")
@Api(tags = "小程序——花店公开资料")
public class FlowerStoreConfigController {

    private final SystemConfigService systemConfigService;

    public FlowerStoreConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @ApiOperation(value = "获取花店名称、联系方式与营业资料")
    @GetMapping("/config")
    public CommonResult<FlowerStoreConfigResponse> getConfig() {
        FlowerStoreConfigResponse response = new FlowerStoreConfigResponse();
        response.setStoreName(valueOrDefault(SysConfigConstants.CONFIG_FLOWER_STORE_NAME, "花间集"));
        response.setStorePhone(valueOrDefault(SysConfigConstants.CONFIG_FLOWER_STORE_PHONE, "暂未设置"));
        response.setStoreAddress(valueOrDefault(SysConfigConstants.CONFIG_FLOWER_STORE_ADDRESS, "暂未设置"));
        response.setBusinessHours(valueOrDefault(SysConfigConstants.CONFIG_FLOWER_BUSINESS_HOURS, "09:00-21:00"));
        response.setBrandSlogan(valueOrDefault(SysConfigConstants.CONFIG_FLOWER_BRAND_SLOGAN, "让每一束花，都有故事"));
        return CommonResult.success(response);
    }

    private String valueOrDefault(String key, String defaultValue) {
        String value = systemConfigService.getValueByKey(key);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }
}
