package com.zbkj.common.model.flower;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/** A durable post-refund task committed in the same transaction as the mock refund. */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_flower_refund_outbox")
public class FlowerRefundOutbox implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer orderId;
    /** 0 pending, 1 completed. */
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
