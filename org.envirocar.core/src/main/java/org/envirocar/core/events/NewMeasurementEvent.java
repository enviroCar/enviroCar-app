package org.envirocar.core.events;

import com.google.common.base.MoreObjects;

import org.envirocar.core.entity.Measurement;


/**
 * @author dewall
 */
public class NewMeasurementEvent {

    public final Measurement mMeasurement;

    /**
     * Constructor.
     *
     * @param measurement
     */
    public NewMeasurementEvent(Measurement measurement) {
        this.mMeasurement = measurement;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Measurement", mMeasurement.toString())
                .toString();
    }
}
