package org.envirocar.obd.events;

import com.google.common.base.MoreObjects;

/**
 * @author dewall
 */
public class SpeedUpdateEvent {

    public final int mSpeed;

    /**
     * Constructor.
     *
     * @param speed the speed value of the event.
     */
    public SpeedUpdateEvent(final int speed){
        this.mSpeed = speed;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Speed", mSpeed)
                .toString();
    }
}
