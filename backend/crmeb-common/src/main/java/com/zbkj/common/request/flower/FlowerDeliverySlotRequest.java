package com.zbkj.common.request.flower;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@ApiModel(value = "FlowerDeliverySlotRequest", description = "鲜花预约配送时段")
public class FlowerDeliverySlotRequest {
    private Integer id;

    @NotNull(message = "配送日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date deliveryDate;

    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "HH:mm:ss")
    private Date startTime;

    @NotNull(message = "结束时间不能为空")
    @JsonFormat(pattern = "HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "HH:mm:ss")
    private Date endTime;

    @NotNull(message = "接单容量不能为空")
    @Min(value = 1, message = "接单容量至少为1")
    private Integer capacity;

    @NotNull(message = "截单时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date cutoffTime;

    private Boolean enabled = true;
}
