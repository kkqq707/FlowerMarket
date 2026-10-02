package com.zbkj.common.request.flower;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zbkj.common.enums.FlowerSubstitutionPolicyEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

@Data
@Accessors(chain = true)
@ApiModel(value = "FlowerOrderRequest", description = "送花订单专属信息")
public class FlowerOrderRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String senderName;
    private String senderPhone;
    private String recipientName;
    private String recipientPhone;
    private Integer deliveryZoneId;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date deliveryDate;

    private Integer slotId;

    @Size(max = 200, message = "贺卡内容不能超过200字")
    private String greetingCard;

    @ApiModelProperty(value = "是否匿名送花")
    private Boolean anonymous = false;

    @ApiModelProperty(value = "配送前是否联系收花人")
    private Boolean contactRecipient = true;

    @ApiModelProperty(value = "花材缺货替换策略")
    private FlowerSubstitutionPolicyEnum substitutionPolicy = FlowerSubstitutionPolicyEnum.CONTACT_FIRST;
}
