package com.zbkj.common.interceptor;

import org.junit.Assert;
import org.junit.Test;

public class SwaggerInterceptorTest {

    @Test
    public void malformedBasicAuthorizationFailsClosed() throws Exception {
        SwaggerInterceptor interceptor = new SwaggerInterceptor("admin", "secret", true);

        Assert.assertFalse(interceptor.httpBasicAuth("Basic not-base64!"));
        Assert.assertFalse(interceptor.httpBasicAuth("Bearer dG9rZW4="));
        Assert.assertFalse(interceptor.httpBasicAuth("Basic dXNlcm9ubHk="));
    }

    @Test
    public void validBasicAuthorizationSupportsColonInPassword() throws Exception {
        SwaggerInterceptor interceptor = new SwaggerInterceptor("admin", "sec:ret", true);

        Assert.assertTrue(interceptor.httpBasicAuth("Basic YWRtaW46c2VjOnJldA=="));
    }
}
