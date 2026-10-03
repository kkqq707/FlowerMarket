package com.zbkj.common.request.flower;

import com.zbkj.common.request.PageParamRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "FlowerProductSearchRequest", description = "鲜花商品筛选条件")
public class FlowerProductSearchRequest extends PageParamRequest {

    @ApiModelProperty(value = "商品名称关键词")
    private String keyword;
    @ApiModelProperty(value = "商品分类，包含子分类")
    private Integer cid;
    @ApiModelProperty(value = "管理端状态：1出售中，2仓库中，3已售罄，4警戒库存，5回收站")
    @Min(1)
    @Max(5)
    private Integer type;
    @ApiModelProperty(value = "适用场景")
    private String occasion;
    @ApiModelProperty(value = "赠送对象")
    private String recipient;
    @ApiModelProperty(value = "色系")
    private String colorSeries;
    @DecimalMin(value = "0", message = "最低预算不能小于0")
    private BigDecimal minPrice;
    @DecimalMin(value = "0", message = "最高预算不能小于0")
    private BigDecimal maxPrice;
}
