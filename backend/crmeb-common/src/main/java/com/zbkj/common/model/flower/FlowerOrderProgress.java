package com.zbkj.common.model.flower;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_flower_order_progress")
@ApiModel(value = "FlowerOrderProgress对象", description = "花店订单履约进度")
public class FlowerOrderProgress implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String orderId;
    private String stage;
    private Integer operatorId;
    private String proofImage;
    private String remark;
    private Date createTime;
}
