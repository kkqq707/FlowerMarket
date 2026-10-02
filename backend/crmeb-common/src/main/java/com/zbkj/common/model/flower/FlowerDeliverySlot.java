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
@TableName("eb_flower_delivery_slot")
@ApiModel(value = "FlowerDeliverySlot对象", description = "鲜花预约配送时段")
public class FlowerDeliverySlot implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "配送日期")
    private Date deliveryDate;

    @ApiModelProperty(value = "时段开始时间")
    private Date startTime;

    @ApiModelProperty(value = "时段结束时间")
    private Date endTime;

    @ApiModelProperty(value = "最大接单量")
    private Integer capacity;

    @ApiModelProperty(value = "已占用数量")
    private Integer bookedCount;

    @ApiModelProperty(value = "停止预约时间")
    private Date cutoffTime;

    @ApiModelProperty(value = "是否启用")
    private Boolean enabled;

    @ApiModelProperty(value = "乐观锁版本")
    private Integer version;

    private Date createTime;

    private Date updateTime;
}
