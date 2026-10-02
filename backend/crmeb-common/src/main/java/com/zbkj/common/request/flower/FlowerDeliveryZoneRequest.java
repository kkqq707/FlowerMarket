package com.zbkj.common.request.flower;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@ApiModel(value = "FlowerDeliveryZoneRequest", description = "花店配送区域")
public class FlowerDeliveryZoneRequest {
    private Integer id;
    @NotBlank(message = "区域名称不能为空")
    private String zoneName;
    @NotBlank(message = "行政区域编码不能为空")
    @ApiModelProperty(value = "多个行政区域编码使用逗号分隔")
    private String areaCodes;
    @NotNull(message = "配送费不能为空")
    @DecimalMin(value = "0", message = "配送费不能小于0")
    private BigDecimal deliveryFee;
    @NotNull(message = "起送金额不能为空")
    @DecimalMin(value = "0", message = "起送金额不能小于0")
    private BigDecimal minimumAmount;
    private Boolean enabled = true;
}
