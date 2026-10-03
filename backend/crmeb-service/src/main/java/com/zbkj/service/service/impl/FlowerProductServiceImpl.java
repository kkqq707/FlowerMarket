package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.flower.FlowerProductExt;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.request.flower.FlowerProductExtRequest;
import com.zbkj.common.request.flower.FlowerProductSearchRequest;
import com.zbkj.common.response.flower.FlowerProductCardResponse;
import com.zbkj.common.response.flower.FlowerProductDetailResponse;
import com.zbkj.service.dao.FlowerProductExtDao;
import com.zbkj.service.service.FlowerProductService;
import com.zbkj.service.service.FlowerMaterialService;
import com.zbkj.service.service.StoreProductService;
import com.zbkj.service.service.SystemConfigService;
import com.zbkj.common.response.StoreProductTabsHeader;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FlowerProductServiceImpl implements FlowerProductService {

    private final FlowerProductExtDao flowerProductExtDao;
    private final StoreProductService storeProductService;

    @Autowired(required = false)
    private FlowerMaterialService flowerMaterialService;

    @Autowired(required = false)
    private SystemConfigService systemConfigService;

    public FlowerProductServiceImpl(FlowerProductExtDao flowerProductExtDao,
                                    StoreProductService storeProductService) {
        this.flowerProductExtDao = flowerProductExtDao;
        this.storeProductService = storeProductService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowerProductExt saveOrUpdate(Integer productId, FlowerProductExtRequest request) {
        getExistingProduct(productId, false);
        if (request == null || !StringUtils.hasText(request.getFlowerMaterials())) {
            throw new CrmebException("主要花材不能为空");
        }

        FlowerProductExt extension = findExtension(productId);
        boolean creating = extension == null;
        if (creating) {
            extension = new FlowerProductExt().setProductId(productId).setCreateTime(new Date());
        }
        Boolean previousBatchManaged = extension.getBatchManaged();
        BeanUtils.copyProperties(request, extension);
        extension.setBatchManaged(request.getBatchManaged() == null
                ? Boolean.TRUE.equals(previousBatchManaged) : request.getBatchManaged());
        if (Boolean.TRUE.equals(extension.getBatchManaged()) && (flowerMaterialService == null
                || flowerMaterialService.recipe(productId).isEmpty())) {
            throw new CrmebException("请先配置花束花材配方，再启用批次管理");
        }
        extension.setFlowerMaterials(request.getFlowerMaterials().trim());
        extension.setOccasion(normalizeTags(request.getOccasion()));
        extension.setRecipient(normalizeTags(request.getRecipient()));
        extension.setColorSeries(normalizeTags(request.getColorSeries()));
        extension.setUpdateTime(new Date());

        if (creating) {
            flowerProductExtDao.insert(extension);
        } else {
            flowerProductExtDao.updateById(extension);
        }
        return extension;
    }

    @Override
    public FlowerProductDetailResponse getAdminDetail(Integer productId) {
        return buildDetail(getExistingProduct(productId, false), findExtension(productId));
    }

    @Override
    public FlowerProductDetailResponse getFrontDetail(Integer productId) {
        StoreProduct product = getExistingProduct(productId, true);
        FlowerProductExt extension = findExtension(productId);
        if (extension == null) {
            throw new CrmebException("鲜花商品信息不存在");
        }
        return buildDetail(product, extension);
    }

    @Override
    public List<FlowerProductCardResponse> search(FlowerProductSearchRequest request) {
        FlowerProductSearchRequest safeRequest = request == null ? new FlowerProductSearchRequest() : request;
        if (safeRequest.getMinPrice() != null && safeRequest.getMaxPrice() != null
                && safeRequest.getMinPrice().compareTo(safeRequest.getMaxPrice()) > 0) {
            throw new CrmebException("最低预算不能高于最高预算");
        }
        PageHelper.startPage(safeRequest.getPage(), safeRequest.getLimit());
        return flowerProductExtDao.searchOnSale(safeRequest);
    }

    @Override
    public List<FlowerProductCardResponse> searchAdmin(FlowerProductSearchRequest request) {
        FlowerProductSearchRequest safeRequest = request == null ? new FlowerProductSearchRequest() : request;
        int warningStock = warningStock();
        PageHelper.startPage(safeRequest.getPage(), safeRequest.getLimit());
        return flowerProductExtDao.searchAdmin(safeRequest, warningStock);
    }

    @Override
    public List<StoreProductTabsHeader> adminTabs(FlowerProductSearchRequest request) {
        FlowerProductSearchRequest criteria = new FlowerProductSearchRequest();
        if (request != null) BeanUtils.copyProperties(request, criteria);
        int warningStock = warningStock();
        String[] labels = {"出售中商品", "仓库中商品", "已售罄商品", "警戒库存", "商品回收站"};
        List<StoreProductTabsHeader> headers = new ArrayList<>();
        for (int type = 1; type <= labels.length; type++) {
            criteria.setType(type);
            headers.add(new StoreProductTabsHeader(flowerProductExtDao.countAdmin(criteria, warningStock), labels[type - 1], type));
        }
        return headers;
    }

    private int warningStock() {
        String value = systemConfigService == null ? null : systemConfigService.getValueByKey("store_stock");
        try {
            return value == null ? 0 : Math.max(0, Integer.parseInt(value.trim()));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    @Override
    public Map<String, List<String>> filterOptions(boolean onSale) {
        List<FlowerProductCardResponse> products = flowerProductExtDao.filterSources(onSale);
        Map<String, List<String>> options = new LinkedHashMap<>();
        options.put("occasion", collectTags(products, FlowerProductCardResponse::getOccasion));
        options.put("recipient", collectTags(products, FlowerProductCardResponse::getRecipient));
        options.put("colorSeries", collectTags(products, FlowerProductCardResponse::getColorSeries));
        return options;
    }

    private List<String> collectTags(List<FlowerProductCardResponse> products,
                                    Function<FlowerProductCardResponse, String> field) {
        return products.stream().map(field).filter(StringUtils::hasText)
                .flatMap(value -> Arrays.stream(value.replace('，', ',').split(",")))
                .map(String::trim).filter(StringUtils::hasText).distinct().collect(Collectors.toList());
    }

    @Override
    public boolean containsFlowerProducts(List<Integer> productIds) {
        return productIds != null && !productIds.isEmpty()
                && flowerProductExtDao.countByProductIds(productIds) > 0;
    }

    private StoreProduct getExistingProduct(Integer productId, boolean requireOnSale) {
        StoreProduct product = productId == null ? null : storeProductService.getById(productId);
        if (product == null || Boolean.TRUE.equals(product.getIsDel()) || Boolean.TRUE.equals(product.getIsRecycle())) {
            throw new CrmebException("商品不存在或已删除");
        }
        if (requireOnSale && !Boolean.TRUE.equals(product.getIsShow())) {
            throw new CrmebException("商品已下架");
        }
        return product;
    }

    private FlowerProductExt findExtension(Integer productId) {
        LambdaQueryWrapper<FlowerProductExt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowerProductExt::getProductId, productId).last("LIMIT 1");
        return flowerProductExtDao.selectOne(wrapper);
    }

    private FlowerProductDetailResponse buildDetail(StoreProduct product, FlowerProductExt extension) {
        FlowerProductDetailResponse response = new FlowerProductDetailResponse();
        response.setId(product.getId());
        response.setImage(product.getImage());
        response.setSliderImage(product.getSliderImage());
        response.setStoreName(product.getStoreName());
        response.setStoreInfo(product.getStoreInfo());
        response.setPrice(product.getPrice());
        response.setOriginalPrice(product.getOtPrice());
        response.setSales((product.getSales() == null ? 0 : product.getSales())
                + (product.getFicti() == null ? 0 : product.getFicti()));
        response.setStock(product.getStock());
        response.setIsShow(product.getIsShow());
        if (extension != null) {
            BeanUtils.copyProperties(extension, response);
            response.setId(product.getId());
            if (flowerMaterialService != null) {
                response.setSuggestedDisplayDays(flowerMaterialService.suggestedDisplayDays(product.getId()));
            }
        }
        return response;
    }

    private String normalizeTags(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return Arrays.stream(value.replace('，', ',').split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining(","));
    }
}
