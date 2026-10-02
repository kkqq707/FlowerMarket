package com.zbkj.front.controller;

import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.ArticleResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.FlowerCareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/front/flower/care/articles")
public class FlowerCareController {
    @Autowired private FlowerCareService careService;

    @GetMapping
    public CommonResult<CommonPage<ArticleResponse>> list(@RequestParam(required = false) String keyword,
                                                             PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(careService.list(keyword, page)));
    }

    @GetMapping("/{id}")
    public CommonResult<ArticleResponse> detail(@PathVariable Integer id) {
        return CommonResult.success(careService.detail(id));
    }
}
