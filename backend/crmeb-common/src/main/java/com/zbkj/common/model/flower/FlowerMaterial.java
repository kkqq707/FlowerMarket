package com.zbkj.common.model.flower;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("eb_flower_material")
public class FlowerMaterial {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String name;
    private String unit;
    private Integer freshDays;
    private Integer displayDays;
    private Integer lowStockThreshold;
    private Boolean enabled;
    private Date createTime;
    private Date updateTime;
}
