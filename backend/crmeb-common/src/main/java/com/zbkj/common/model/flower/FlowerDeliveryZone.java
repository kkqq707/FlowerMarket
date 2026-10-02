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
import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_flower_delivery_zone")
@ApiModel(value = "FlowerDeliveryZone对象", description = "花店同城配送区域")
public class FlowerDeliveryZone implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "配送区域名称")
    private String zoneName;

    @ApiModelProperty(value = "覆盖行政区域编码，多个值使用英文逗号分隔")
    private String areaCodes;

    @ApiModelProperty(value = "配送费")
    private BigDecimal deliveryFee;

    @ApiModelProperty(value = "起送金额")
    private BigDecimal minimumAmount;

    @ApiModelProperty(value = "是否启用")
    private Boolean enabled;

    private Date createTime;

    private Date updateTime;
}
