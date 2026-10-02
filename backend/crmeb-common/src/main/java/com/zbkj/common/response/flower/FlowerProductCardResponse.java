package com.zbkj.common.response.flower;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel(value = "FlowerProductCardResponse", description = "鲜花商品卡片")
public class FlowerProductCardResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    @ApiModelProperty(value = "纯商品主图，不包含价格文字")
    private String image;
    private String storeName;
    private String storeInfo;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer sales;
    private Integer stock;
    private Boolean isShow;
    private String flowerMaterials;
    private String flowerLanguage;
    private String occasion;
    private String recipient;
    private String colorSeries;
}
