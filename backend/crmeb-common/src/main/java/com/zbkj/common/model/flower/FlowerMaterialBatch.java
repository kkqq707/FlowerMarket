package com.zbkj.common.model.flower;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("eb_flower_material_batch")
public class FlowerMaterialBatch {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Integer materialId;
    private String batchNo;
    private Date receivedDate;
    private Date expiryDate;
    private Integer receivedQty;
    private Integer currentQty;
    private Integer reservedQty;
    private String remark;
    private Date createTime;
    private Date updateTime;
}
