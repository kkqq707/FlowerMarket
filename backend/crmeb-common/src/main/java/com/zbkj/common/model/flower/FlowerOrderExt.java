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
@TableName("eb_flower_order_ext")
@ApiModel(value = "FlowerOrderExt对象", description = "送花订单扩展信息")
public class FlowerOrderExt implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String orderId;
    private String senderName;
    private String senderPhone;
    private String recipientName;
    private String recipientPhone;
    private String deliveryAddressSnapshot;
    private Integer deliveryZoneId;
    private Date deliveryDate;
    private Integer slotId;
    private String greetingCard;
    private Boolean anonymous;
    private Boolean contactRecipient;
    private String substitutionPolicy;
    private String currentStage;
    private String courierName;
    private String courierPhone;
    private String deliveryRemark;
    private Date reservationExpireTime;

    @ApiModelProperty(value = "预约容量是否已经释放")
    private Boolean reservationReleased;

    private Date createTime;
    private Date updateTime;
}
