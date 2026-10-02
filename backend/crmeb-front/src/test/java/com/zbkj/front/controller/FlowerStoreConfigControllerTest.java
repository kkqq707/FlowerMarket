package com.zbkj.front.controller;

import com.zbkj.common.response.flower.FlowerStoreConfigResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.SystemConfigService;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class FlowerStoreConfigControllerTest {

    @Test
    public void returnsConfiguredValuesAndSafeDefaults() {
        Map<String, String> values = new HashMap<>();
        values.put("flower_store_name", "测试花店");
        values.put("flower_store_phone", "010-12345678");
        values.put("flower_business_hours", "10:00-20:00");
        SystemConfigService configService = (SystemConfigService) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{SystemConfigService.class},
                (proxy, method, args) -> "getValueByKey".equals(method.getName()) ? values.get(args[0]) : null
        );

        FlowerStoreConfigController controller = new FlowerStoreConfigController(configService);
        CommonResult<FlowerStoreConfigResponse> result = controller.getConfig();

        assertEquals("测试花店", result.getData().getStoreName());
        assertEquals("010-12345678", result.getData().getStorePhone());
        assertEquals("暂未设置", result.getData().getStoreAddress());
        assertEquals("10:00-20:00", result.getData().getBusinessHours());
        assertEquals("让每一束花，都有故事", result.getData().getBrandSlogan());
    }
}
