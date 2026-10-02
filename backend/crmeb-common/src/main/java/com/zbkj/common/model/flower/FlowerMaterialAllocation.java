package com.zbkj.common.model.flower;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("eb_flower_material_allocation")
public class FlowerMaterialAllocation {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String orderId;
    private Integer materialId;
    private Long batchId;
    private Integer quantity;
    private String status;
    private Date createTime;
    private Date updateTime;
}
