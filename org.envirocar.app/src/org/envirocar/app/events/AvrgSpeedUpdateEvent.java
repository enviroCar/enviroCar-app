package org.envirocar.app.events;

import com.google.common.base.MoreObjects;

/**
 * @author dewall
 */
public class AvrgSpeedUpdateEvent {

    public final int mAvrgSpeed;

    /**
     * Constructor.
     *
     * @param mAvrgSpeed the new avrg speed value;
     */
    public AvrgSpeedUpdateEvent(int mAvrgSpeed) {
        this.mAvrgSpeed = mAvrgSpeed;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Average Speed", mAvrgSpeed)
                .toString();
    }
}
