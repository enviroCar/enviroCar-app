package org.envirocar.app.bluetooth.obd.events;

import com.google.common.base.MoreObjects;

/**
 * @author dewall
 */
public final class IntakePreasureUpdateEvent {

    public final int mIntakePreasure;

    /**
     * Constructor.
     *
     * @param intakePreasure the preasure value of the event.
     */
    public IntakePreasureUpdateEvent(final int intakePreasure) {
        this.mIntakePreasure = intakePreasure;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Preasure", mIntakePreasure)
                .toString();
    }
}
