package com.zbkj.common.enums;

import org.junit.Test;

import static com.zbkj.common.enums.FlowerOrderStageEnum.COMPLETED;
import static com.zbkj.common.enums.FlowerOrderStageEnum.DELIVERING;
import static com.zbkj.common.enums.FlowerOrderStageEnum.PRODUCING;
import static com.zbkj.common.enums.FlowerOrderStageEnum.WAIT_ACCEPT;
import static com.zbkj.common.enums.FlowerOrderStageEnum.WAIT_PRODUCTION;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FlowerOrderStageEnumTest {

    @Test
    public void shouldAllowOnlyTheImmediateNextStage() {
        assertTrue(WAIT_ACCEPT.canTransitTo(WAIT_PRODUCTION));
        assertTrue(WAIT_PRODUCTION.canTransitTo(PRODUCING));
        assertFalse(WAIT_ACCEPT.canTransitTo(PRODUCING));
        assertFalse(COMPLETED.canTransitTo(DELIVERING));
    }

    @Test
    public void shouldTreatTheSameStageAsIdempotent() {
        assertTrue(WAIT_ACCEPT.isSameStage(WAIT_ACCEPT));
        assertFalse(WAIT_ACCEPT.isSameStage(WAIT_PRODUCTION));
    }
}
