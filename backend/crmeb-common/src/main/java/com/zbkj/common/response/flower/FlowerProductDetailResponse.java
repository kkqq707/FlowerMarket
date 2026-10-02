package com.zbkj.common.response.flower;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel(value = "FlowerProductDetailResponse", description = "鲜花商品详情")
public class FlowerProductDetailResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    @ApiModelProperty(value = "纯商品主图，不包含价格文字")
    private String image;
    private String sliderImage;
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
    private Integer stemCount;
    private String packaging;
    private String careTips;
    private Boolean batchManaged;
    private Integer suggestedDisplayDays;
}
