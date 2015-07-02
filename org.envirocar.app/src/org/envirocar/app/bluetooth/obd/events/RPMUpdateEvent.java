package org.envirocar.app.bluetooth.obd.events;

import com.google.common.base.MoreObjects;

/**
 * @author dewall
 */
public final class RPMUpdateEvent {

    public final int mRPM;

    /**
     * Constructor.
     *
     * @param rpm the rpm value of this event.
     */
    public RPMUpdateEvent(final int rpm) {
        this.mRPM = rpm;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("RPM", mRPM)
                .toString();
    }
}
