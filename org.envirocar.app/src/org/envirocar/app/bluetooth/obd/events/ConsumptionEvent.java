package org.envirocar.app.bluetooth.obd.events;

import com.google.common.base.MoreObjects;

/**
 * @author dewall
 */
public final class ConsumptionEvent {

    public final double mConsumption;

    /**
     * Constructor.
     *
     * @param consumption the fuel consumption value of the event.
     */
    public ConsumptionEvent(final double consumption) {
        this.mConsumption = consumption;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Consumption", mConsumption)
                .toString();
    }
}
