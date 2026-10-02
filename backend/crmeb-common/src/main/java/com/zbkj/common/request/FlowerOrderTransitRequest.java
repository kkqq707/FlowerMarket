package com.zbkj.common.request;

import com.zbkj.common.enums.FlowerOrderStageEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@ApiModel(value = "FlowerOrderTransitRequest", description = "花店订单履约阶段变更参数")
public class FlowerOrderTransitRequest {

    @NotNull(message = "目标阶段不能为空")
    @ApiModelProperty(value = "目标阶段", required = true)
    private FlowerOrderStageEnum targetStage;

    private String remark;
    private String proofImage;
    private String courierName;
    private String courierPhone;
}
