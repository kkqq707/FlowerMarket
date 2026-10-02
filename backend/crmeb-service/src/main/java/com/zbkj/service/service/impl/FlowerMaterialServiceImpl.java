package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.zbkj.service.service.FlowerMaterialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FlowerMaterialServiceImpl implements FlowerMaterialService {
    private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Shanghai");
    private final FlowerMaterialDao materialDao;
    private final FlowerMaterialBatchDao batchDao;
    private final FlowerRecipeDao recipeDao;
    private final FlowerMaterialAllocationDao allocationDao;
    private final FlowerProductExtDao productExtDao;

    public FlowerMaterialServiceImpl(FlowerMaterialDao materialDao, FlowerMaterialBatchDao batchDao,
                                     FlowerRecipeDao recipeDao, FlowerMaterialAllocationDao allocationDao,
                                     FlowerProductExtDao productExtDao) {
        this.materialDao = materialDao;
        this.batchDao = batchDao;
        this.recipeDao = recipeDao;
        this.allocationDao = allocationDao;
        this.productExtDao = productExtDao;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reserve(String orderNo, List<StoreOrderInfo> items, Date deliveryDate) {
        if (!StringUtils.hasText(orderNo) || deliveryDate == null) throw new CrmebException("花材预约信息不完整");
        if (!allocationDao.selectByOrderForUpdate(orderNo).isEmpty()) return;
        Map<Integer, Integer> required = new LinkedHashMap<>();
        if (items == null) return;
        for (StoreOrderInfo item : items) {
            if (item.getProductId() == null || item.getPayNum() == null || item.getPayNum() <= 0)
                throw new CrmebException("订单商品数量无效");
            FlowerProductExt ext = productExtDao.selectOne(new QueryWrapper<FlowerProductExt>()
                    .eq("product_id", item.getProductId()).last("LIMIT 1"));
            if (ext == null || !Boolean.TRUE.equals(ext.getBatchManaged())) continue;
            List<FlowerRecipe> parts = recipe(item.getProductId());
            if (parts.isEmpty()) throw new CrmebException("花束尚未配置花材配方，暂不能下单");
            for (FlowerRecipe part : parts) {
                if (part.getMaterialId() == null || part.getQtyPerItem() == null || part.getQtyPerItem() <= 0)
                    throw new CrmebException("花束花材配方无效");
                required.merge(part.getMaterialId(), Math.multiplyExact(part.getQtyPerItem(), item.getPayNum()), Math::addExact);
            }
        }
        // 固定花材锁顺序，避免不同商品顺序造成数据库死锁。
        List<Integer> materialIds = new ArrayList<>(required.keySet());
        materialIds.sort(Integer::compareTo);
        for (Integer materialId : materialIds) {
            FlowerMaterial material = materialDao.selectById(materialId);
            if (material == null || !Boolean.TRUE.equals(material.getEnabled()))
                throw new CrmebException("花材已停用，暂不能下单");
            int remaining = required.get(materialId);
            List<FlowerMaterialBatch> eligible = batchDao.eligibleForUpdate(materialId, deliveryDate);
            if (eligible != null) for (FlowerMaterialBatch batch : eligible) {
                int available = batch.getCurrentQty() - batch.getReservedQty();
                int quantity = Math.min(remaining, available);
                if (quantity <= 0) continue;
                if (batchDao.reserve(batch.getId(), quantity) != 1)
                    throw new CrmebException("花材库存已变化，请重新提交订单");
                FlowerMaterialAllocation allocation = new FlowerMaterialAllocation()
                        .setOrderId(orderNo).setMaterialId(materialId).setBatchId(batch.getId())
                        .setQuantity(quantity).setStatus("RESERVED")
                        .setCreateTime(new Date()).setUpdateTime(new Date());
                if (allocationDao.insert(allocation) != 1) throw new CrmebException("保存花材分配失败");
                remaining -= quantity;
                if (remaining == 0) break;
            }
            if (remaining > 0) throw new CrmebException("花材“" + material.getName() + "”在所选日期库存不足，请更换日期或联系花店");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void release(String orderNo) { changeAllocations(orderNo, "RELEASED"); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void consume(String orderNo) { changeAllocations(orderNo, "CONSUMED"); }

    private void changeAllocations(String orderNo, String target) {
        if (!StringUtils.hasText(orderNo)) return;
        List<FlowerMaterialAllocation> allocations = allocationDao.selectByOrderForUpdate(orderNo);
        if (allocations == null) return;
        for (FlowerMaterialAllocation item : allocations) {
            if (!"RESERVED".equals(item.getStatus())) continue;
            if (allocationDao.changeStatus(item.getId(), "RESERVED", target) != 1)
                throw new CrmebException("花材分配状态已变化，请重试");
            int changed = "CONSUMED".equals(target)
                    ? batchDao.consume(item.getBatchId(), item.getQuantity())
                    : batchDao.release(item.getBatchId(), item.getQuantity());
            if (changed != 1) throw new CrmebException("更新花材批次库存失败");
        }
    }

    @Override
    public List<FlowerMaterial> materials() {
        return materialDao.selectList(new QueryWrapper<FlowerMaterial>().orderByAsc("id"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowerMaterial saveMaterial(FlowerMaterial value) {
        if (value == null || !StringUtils.hasText(value.getName()) || !StringUtils.hasText(value.getUnit())
                || value.getFreshDays() == null || value.getFreshDays() <= 0
                || value.getLowStockThreshold() == null || value.getLowStockThreshold() < 0
                || (value.getDisplayDays() != null && value.getDisplayDays() <= 0))
            throw new CrmebException("请填写有效的花材名称、单位和保鲜天数");
        value.setName(value.getName().trim()).setUnit(value.getUnit().trim()).setUpdateTime(new Date());
        if (value.getId() == null) {
            value.setCreateTime(new Date());
            if (materialDao.insert(value) != 1) throw new CrmebException("新增花材失败");
        } else {
            if (materialDao.updateById(value) != 1) throw new CrmebException("花材不存在");
        }
        return value;
    }

    @Override
    public List<FlowerMaterialBatch> batches(Integer materialId) {
        QueryWrapper<FlowerMaterialBatch> query = new QueryWrapper<>();
        if (materialId != null) query.eq("material_id", materialId);
        return batchDao.selectList(query.orderByAsc("expiry_date", "received_date", "id"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowerMaterialBatch receive(FlowerMaterialBatch value) {
        if (value == null || value.getMaterialId() == null || !StringUtils.hasText(value.getBatchNo())
                || value.getReceivedDate() == null || value.getExpiryDate() == null
                || value.getReceivedQty() == null || value.getReceivedQty() <= 0)
            throw new CrmebException("请填写完整的花材批次与入库数量");
        if (day(value.getExpiryDate()).isBefore(day(value.getReceivedDate())))
            throw new CrmebException("批次到期日不能早于入库日");
        if (day(value.getReceivedDate()).isAfter(LocalDate.now(STORE_ZONE)))
            throw new CrmebException("入库日期不能晚于今天");
        if (materialDao.selectById(value.getMaterialId()) == null) throw new CrmebException("花材不存在");
        value.setId(null).setBatchNo(value.getBatchNo().trim()).setCurrentQty(value.getReceivedQty())
                .setReservedQty(0).setCreateTime(new Date()).setUpdateTime(new Date());
        if (batchDao.insert(value) != 1) throw new CrmebException("花材入库失败");
        return value;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjust(Long batchId, Integer currentQty) {
        if (batchId == null || currentQty == null || currentQty < 0
                || batchDao.adjustCurrent(batchId, currentQty) != 1)
            throw new CrmebException("盘点数量不能低于已锁定数量，或批次不存在");
    }

    @Override
    public List<FlowerRecipe> recipe(Integer productId) {
        return recipeDao.selectList(new QueryWrapper<FlowerRecipe>()
                .eq("product_id", productId).orderByAsc("material_id"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceRecipe(Integer productId, List<FlowerRecipe> parts) {
        if (productId == null || parts == null || parts.isEmpty()) throw new CrmebException("花束配方至少包含一种花材");
        FlowerProductExt extension = productExtDao.selectOne(new QueryWrapper<FlowerProductExt>()
                .eq("product_id", productId).last("LIMIT 1"));
        if (extension == null) throw new CrmebException("请先保存花卉商品资料，再配置配方");
        Map<Integer, Integer> normalized = new LinkedHashMap<>();
        for (FlowerRecipe part : parts) {
            if (part.getMaterialId() == null || part.getQtyPerItem() == null || part.getQtyPerItem() <= 0
                    || normalized.putIfAbsent(part.getMaterialId(), part.getQtyPerItem()) != null)
                throw new CrmebException("花束配方花材重复或用量无效");
            FlowerMaterial material = materialDao.selectById(part.getMaterialId());
            if (material == null || !Boolean.TRUE.equals(material.getEnabled())) throw new CrmebException("配方花材不存在或已停用");
        }
        recipeDao.delete(new QueryWrapper<FlowerRecipe>().eq("product_id", productId));
        normalized.forEach((materialId, quantity) -> {
            if (recipeDao.insert(new FlowerRecipe().setProductId(productId).setMaterialId(materialId)
                    .setQtyPerItem(quantity)) != 1) throw new CrmebException("保存花束配方失败");
        });
    }

    @Override
    public Integer suggestedDisplayDays(Integer productId) {
        List<FlowerRecipe> parts = recipe(productId);
        if (parts.isEmpty()) return null;
        Integer shortest = null;
        for (FlowerRecipe part : parts) {
            FlowerMaterial material = materialDao.selectById(part.getMaterialId());
            if (material == null || material.getDisplayDays() == null || !Boolean.TRUE.equals(material.getEnabled())) return null;
            shortest = shortest == null ? material.getDisplayDays() : Math.min(shortest, material.getDisplayDays());
        }
        return shortest;
    }

    private LocalDate day(Date value) {
        return Instant.ofEpochMilli(value.getTime()).atZone(STORE_ZONE).toLocalDate();
    }
}
