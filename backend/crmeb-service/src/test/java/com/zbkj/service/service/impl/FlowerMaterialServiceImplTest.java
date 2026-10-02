package com.zbkj.service.service.impl;

import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.flower.FlowerMaterial;
import com.zbkj.common.model.flower.FlowerMaterialAllocation;
import com.zbkj.common.model.flower.FlowerMaterialBatch;
import com.zbkj.common.model.flower.FlowerProductExt;
import com.zbkj.common.model.flower.FlowerRecipe;
import com.zbkj.common.model.order.StoreOrderInfo;
import com.zbkj.service.dao.FlowerMaterialAllocationDao;
import com.zbkj.service.dao.FlowerMaterialBatchDao;
import com.zbkj.service.dao.FlowerMaterialDao;
import com.zbkj.service.dao.FlowerProductExtDao;
import com.zbkj.service.dao.FlowerRecipeDao;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class FlowerMaterialServiceImplTest {
    private final List<FlowerMaterialAllocation> saved = new ArrayList<>();
    private final List<Long> reservedBatchIds = new ArrayList<>();
    private int released;
    private int consumed;

    @Test
    public void reservesAcrossBatchesInExpiryOrderAndIgnoresExpiredBatch() {
        FlowerMaterialServiceImpl service = service(Arrays.asList(batch(11L, 3), batch(12L, 10)));
        service.reserve("ORDER-1", Arrays.asList(new StoreOrderInfo().setProductId(900001).setPayNum(2)), new Date());
        Assert.assertEquals(Arrays.asList(11L, 12L), reservedBatchIds);
        Assert.assertEquals(3, saved.get(0).getQuantity().intValue());
        Assert.assertEquals(5, saved.get(1).getQuantity().intValue());
    }

    @Test(expected = CrmebException.class)
    public void enabledRecipeNeverFallsBackWhenMaterialInsufficient() {
        service(Arrays.asList(batch(11L, 2))).reserve("ORDER-2",
                Arrays.asList(new StoreOrderInfo().setProductId(900001).setPayNum(1)), new Date());
    }

    @Test
    public void releaseAndConsumeAreIdempotentForAlreadyChangedAllocations() {
        FlowerMaterialServiceImpl service = service(Arrays.asList(batch(11L, 8)));
        service.reserve("ORDER-3", Arrays.asList(new StoreOrderInfo().setProductId(900001).setPayNum(1)), new Date());
        service.release("ORDER-3");
        service.release("ORDER-3");
        Assert.assertEquals(1, released);
        service.reserve("ORDER-4", Arrays.asList(new StoreOrderInfo().setProductId(900001).setPayNum(1)), new Date());
        service.consume("ORDER-4");
        service.consume("ORDER-4");
        Assert.assertEquals(1, consumed);
    }

    @SuppressWarnings("unchecked")
    private FlowerMaterialServiceImpl service(List<FlowerMaterialBatch> batches) {
        FlowerProductExtDao products = (FlowerProductExtDao) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{FlowerProductExtDao.class}, (p, m, a) -> "selectOne".equals(m.getName())
                        ? new FlowerProductExt().setBatchManaged(true) : zero(m.getReturnType()));
        FlowerRecipeDao recipes = (FlowerRecipeDao) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{FlowerRecipeDao.class}, (p, m, a) -> "selectList".equals(m.getName())
                        ? Arrays.asList(new FlowerRecipe().setMaterialId(1).setQtyPerItem(4)) : zero(m.getReturnType()));
        FlowerMaterialDao materials = (FlowerMaterialDao) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{FlowerMaterialDao.class}, (p, m, a) -> "selectById".equals(m.getName())
                        ? new FlowerMaterial().setId(1).setName("玫瑰").setEnabled(true) : zero(m.getReturnType()));
        FlowerMaterialBatchDao batchDao = (FlowerMaterialBatchDao) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{FlowerMaterialBatchDao.class}, (p, m, a) -> {
                    if ("eligibleForUpdate".equals(m.getName())) return batches;
                    if ("reserve".equals(m.getName())) { reservedBatchIds.add((Long) a[0]); return 1; }
                    if ("release".equals(m.getName())) { released++; return 1; }
                    if ("consume".equals(m.getName())) { consumed++; return 1; }
                    return zero(m.getReturnType());
                });
        FlowerMaterialAllocationDao allocations = (FlowerMaterialAllocationDao) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{FlowerMaterialAllocationDao.class}, (p, m, a) -> {
                    if ("insert".equals(m.getName())) {
                        FlowerMaterialAllocation item = (FlowerMaterialAllocation) a[0];
                        item.setId((long) saved.size() + 1);
                        saved.add(item);
                        return 1;
                    }
                    if ("selectByOrderForUpdate".equals(m.getName())) {
                        List<FlowerMaterialAllocation> result = new ArrayList<>();
                        for (FlowerMaterialAllocation item : saved) if (item.getOrderId().equals(a[0])) result.add(item);
                        return result;
                    }
                    if ("changeStatus".equals(m.getName())) {
                        for (FlowerMaterialAllocation item : saved) if (item.getId().equals(a[0]) && item.getStatus().equals(a[1])) {
                            item.setStatus((String) a[2]); return 1;
                        }
                        return 0;
                    }
                    return zero(m.getReturnType());
                });
        return new FlowerMaterialServiceImpl(materials, batchDao, recipes, allocations, products);
    }

    private FlowerMaterialBatch batch(Long id, int available) {
        return new FlowerMaterialBatch().setId(id).setMaterialId(1).setCurrentQty(available).setReservedQty(0);
    }

    private Object zero(Class<?> type) {
        if (type == int.class) return 0;
        if (type == boolean.class) return false;
        return null;
    }
}
