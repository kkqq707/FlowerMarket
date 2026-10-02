package com.zbkj.admin.controller;

import com.zbkj.common.model.flower.FlowerMaterial;
import com.zbkj.common.model.flower.FlowerMaterialBatch;
import com.zbkj.common.model.flower.FlowerRecipe;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.FlowerMaterialService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/admin/flower/material")
@Api(tags = "花店后台——花材批次与配方")
public class FlowerMaterialController {
    @Autowired private FlowerMaterialService service;

    @PreAuthorize("hasAuthority('admin:flower:material:list')")
    @GetMapping("/list")
    public CommonResult<List<FlowerMaterial>> materials() {
        return CommonResult.success(service.materials());
    }

    @PreAuthorize("hasAuthority('admin:flower:material:save')")
    @PostMapping
    public CommonResult<FlowerMaterial> saveMaterial(@RequestBody FlowerMaterial material) {
        return CommonResult.success(service.saveMaterial(material));
    }

    @PreAuthorize("hasAuthority('admin:flower:material:list')")
    @GetMapping("/batches")
    public CommonResult<List<FlowerMaterialBatch>> batches(@RequestParam(required = false) Integer materialId) {
        return CommonResult.success(service.batches(materialId));
    }

    @PreAuthorize("hasAuthority('admin:flower:material:receive')")
    @PostMapping("/batches")
    public CommonResult<FlowerMaterialBatch> receive(@RequestBody FlowerMaterialBatch batch) {
        return CommonResult.success(service.receive(batch));
    }

    @PreAuthorize("hasAuthority('admin:flower:material:adjust')")
    @PostMapping("/batches/{batchId}/adjust")
    public CommonResult<Void> adjust(@PathVariable Long batchId, @RequestParam Integer currentQty) {
        service.adjust(batchId, currentQty);
        return CommonResult.success();
    }

    @PreAuthorize("hasAuthority('admin:flower:product:info')")
    @GetMapping("/recipe/{productId}")
    public CommonResult<List<FlowerRecipe>> recipe(@PathVariable Integer productId) {
        return CommonResult.success(service.recipe(productId));
    }

    @PreAuthorize("hasAuthority('admin:flower:product:save')")
    @PostMapping("/recipe/{productId}")
    public CommonResult<Void> recipe(@PathVariable Integer productId, @RequestBody List<FlowerRecipe> recipe) {
        service.replaceRecipe(productId, recipe);
        return CommonResult.success();
    }
}
