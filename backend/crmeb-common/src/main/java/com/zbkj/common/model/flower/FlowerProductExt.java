package com.zbkj.common.model.flower;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_flower_product_ext")
@ApiModel(value = "FlowerProductExt对象", description = "花卉商品扩展信息")
public class FlowerProductExt implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "原商城商品编号")
    private Integer productId;

    @ApiModelProperty(value = "主要花材")
    private String flowerMaterials;

    @ApiModelProperty(value = "花语")
    private String flowerLanguage;

    @ApiModelProperty(value = "适用场景，多个值使用英文逗号分隔")
    private String occasion;

    @ApiModelProperty(value = "赠送对象，多个值使用英文逗号分隔")
    private String recipient;

    @ApiModelProperty(value = "色系，多个值使用英文逗号分隔")
    private String colorSeries;

    @ApiModelProperty(value = "花材枝数")
    private Integer stemCount;

    @ApiModelProperty(value = "包装方式")
    private String packaging;

    @ApiModelProperty(value = "养护说明")
    private String careTips;

    @ApiModelProperty(value = "是否启用花材批次管理")
    private Boolean batchManaged;

    private Date createTime;

    private Date updateTime;
}
