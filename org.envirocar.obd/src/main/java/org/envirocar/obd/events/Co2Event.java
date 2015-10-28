package org.envirocar.obd.events;

import com.google.common.base.MoreObjects;

/**
 * @author dewall
 */
public class Co2Event {

    public final double mCo2;

    /**
     * Constructor.
     *
     * @param co2   the co2 value of the event.
     */
    public Co2Event(final double co2){
        this.mCo2 = co2;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Co2", mCo2)
                .toString();
    }
}
