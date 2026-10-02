package com.zbkj.common.request.flower;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Data
@ApiModel(value = "FlowerProductExtRequest", description = "鲜花商品扩展信息")
public class FlowerProductExtRequest {

    @NotBlank(message = "主要花材不能为空")
    @ApiModelProperty(value = "主要花材", required = true)
    private String flowerMaterials;

    @ApiModelProperty(value = "花语")
    private String flowerLanguage;

    @ApiModelProperty(value = "适用场景，多个值使用逗号分隔")
    private String occasion;

    @ApiModelProperty(value = "赠送对象，多个值使用逗号分隔")
    private String recipient;

    @ApiModelProperty(value = "色系，多个值使用逗号分隔")
    private String colorSeries;

    @Min(value = 0, message = "花材枝数不能小于0")
    @ApiModelProperty(value = "花材枝数")
    private Integer stemCount;

    @ApiModelProperty(value = "包装方式")
    private String packaging;

    @ApiModelProperty(value = "养护说明")
    private String careTips;

    @ApiModelProperty(value = "启用花材批次管理前须先配置配方")
    private Boolean batchManaged;
}
