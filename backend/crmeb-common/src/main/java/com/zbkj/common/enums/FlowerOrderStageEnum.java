package com.zbkj.common.enums;

import java.util.EnumMap;
import java.util.Map;

/**
 * 花店订单履约阶段。
 */
public enum FlowerOrderStageEnum {
    WAIT_PAY,
    WAIT_ACCEPT,
    WAIT_PRODUCTION,
    PRODUCING,
    WAIT_DELIVERY,
    DELIVERING,
    COMPLETED;

    private static final Map<FlowerOrderStageEnum, FlowerOrderStageEnum> NEXT_STAGE =
            new EnumMap<>(FlowerOrderStageEnum.class);

    static {
        NEXT_STAGE.put(WAIT_PAY, WAIT_ACCEPT);
        NEXT_STAGE.put(WAIT_ACCEPT, WAIT_PRODUCTION);
        NEXT_STAGE.put(WAIT_PRODUCTION, PRODUCING);
        NEXT_STAGE.put(PRODUCING, WAIT_DELIVERY);
        NEXT_STAGE.put(WAIT_DELIVERY, DELIVERING);
        NEXT_STAGE.put(DELIVERING, COMPLETED);
    }

    public boolean canTransitTo(FlowerOrderStageEnum targetStage) {
        return targetStage != null && targetStage == NEXT_STAGE.get(this);
    }

    public boolean isSameStage(FlowerOrderStageEnum targetStage) {
        return this == targetStage;
    }
}
