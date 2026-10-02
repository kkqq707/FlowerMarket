package com.zbkj.common.enums;

/**
 * The expiry scanner's explicit cancellation/release decision.
 */
public enum ExpiredReservationCancellationResult {
    NEW_CANCELLATION,
    RECOVERABLE_RELEASE,
    NO_ACTION;

    public boolean shouldReleaseReservation() {
        return this == NEW_CANCELLATION || this == RECOVERABLE_RELEASE;
    }
}
