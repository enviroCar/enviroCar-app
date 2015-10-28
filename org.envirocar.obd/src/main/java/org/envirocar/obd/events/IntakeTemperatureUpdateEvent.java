package org.envirocar.obd.events;

import com.google.common.base.MoreObjects;

/**
 * @author dewall
 */
public class IntakeTemperatureUpdateEvent {

    public final double mIntakeTemperature;

    /**
     * Constructor.
     *
     * @param temperature
     */
    public IntakeTemperatureUpdateEvent(final double temperature) {
        this.mIntakeTemperature = temperature;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Temperature", mIntakeTemperature)
                .toString();
    }
}
