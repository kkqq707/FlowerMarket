package com.zbkj.common.response.flower;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "FlowerStoreConfigResponse", description = "花店公开资料")
public class FlowerStoreConfigResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("门店名称")
    private String storeName;

    @ApiModelProperty("联系电话")
    private String storePhone;

    @ApiModelProperty("门店地址")
    private String storeAddress;

    @ApiModelProperty("营业时间")
    private String businessHours;

    @ApiModelProperty("品牌文案")
    private String brandSlogan;
}
