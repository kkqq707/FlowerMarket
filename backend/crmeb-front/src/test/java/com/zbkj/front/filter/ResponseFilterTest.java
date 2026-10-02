package com.zbkj.front.filter;

import com.zbkj.common.config.CrmebConfig;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ResponseFilterTest {

    @Test
    public void writesMiniProgramJsonAsUtf8WhenJvmDefaultCharsetIsGbk() throws Exception {
        String payload = "{\"name\":\"晨露 · 白绿洋桔梗\",\"menu\":\"地址管理\"}";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/front/flower/product/list");
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseFilter filter = new ResponseFilter();
        CrmebConfig config = new CrmebConfig();
        config.setIgnored(Collections.emptyList());
        filter.crmebConfig = config;

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            servletResponse.setContentType("application/json");
            servletResponse.getOutputStream().write(payload.getBytes(StandardCharsets.UTF_8));
        });

        assertEquals("UTF-8", response.getCharacterEncoding());
        assertArrayEquals(payload.getBytes(StandardCharsets.UTF_8), response.getContentAsByteArray());
    }
}
