package org.envirocar.app.events;

import com.google.common.base.MoreObjects;

/**
 * @author dewall
 */
public class DistanceValueUpdateEvent {

    public final double mDistanceValue;

    /**
     * Constructor.
     *
     * @param mDistanceValue the distance value of the event.
     */
    public DistanceValueUpdateEvent(double mDistanceValue) {
        this.mDistanceValue = mDistanceValue;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Distance", mDistanceValue)
                .toString();
    }
}
