package org.envirocar.app.events;

import com.google.common.base.MoreObjects;

/**
 * @author dewall
 */
public class SpeedEvent2 {

    public final int mSpeed;

    /**
     * Constructor.
     *
     * @param speed the speed value of the event.
     */
    public SpeedEvent2(final int speed){
        this.mSpeed = speed;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Speed", mSpeed)
                .toString();
    }
}
