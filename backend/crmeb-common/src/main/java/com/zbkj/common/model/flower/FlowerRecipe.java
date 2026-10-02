package com.zbkj.common.model.flower;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("eb_flower_recipe")
public class FlowerRecipe {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Integer productId;
    private Integer materialId;
    private Integer qtyPerItem;
}
