package com.zbkj.admin.controller;

import com.zbkj.common.model.article.Article;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.ArticleRequest;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.vo.ArticleVo;
import com.zbkj.service.service.FlowerCareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/admin/flower/care/articles")
public class FlowerCareController {
    @Autowired private FlowerCareService careService;

    @PreAuthorize("hasAuthority('admin:flower:care:list')")
    @GetMapping
    public CommonResult<CommonPage<ArticleVo>> list(@RequestParam(required = false) String keyword,
                                                      PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(careService.adminList(keyword, page)));
    }

    @PreAuthorize("hasAuthority('admin:flower:care:list')")
    @GetMapping("/{id}")
    public CommonResult<Article> detail(@PathVariable Integer id) {
        return CommonResult.success(careService.adminDetail(id));
    }

    @PreAuthorize("hasAuthority('admin:flower:care:save')")
    @PostMapping
    public CommonResult<Void> save(@RequestBody @Validated ArticleRequest request) {
        careService.save(request);
        return CommonResult.success();
    }

    @PreAuthorize("hasAuthority('admin:flower:care:save')")
    @PostMapping("/{id}")
    public CommonResult<Void> update(@PathVariable Integer id, @RequestBody @Validated ArticleRequest request) {
        careService.update(id, request);
        return CommonResult.success();
    }
}
