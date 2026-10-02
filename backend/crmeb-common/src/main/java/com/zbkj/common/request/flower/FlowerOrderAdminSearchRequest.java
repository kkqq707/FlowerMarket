package com.zbkj.common.request.flower;

import com.zbkj.common.request.PageParamRequest;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "FlowerOrderAdminSearchRequest", description = "花店订单工作台查询")
public class FlowerOrderAdminSearchRequest extends PageParamRequest {
    private String stage;
    private String keyword;
}
