package org.envirocar.core.events.gps;

import com.google.common.base.MoreObjects;

/**
 * @author dewall
 */
public class GpsStateChangedEvent {

    public final boolean mIsGPSEnabled;

    /**
     * Constructor.
     *
     * @param mIsGPSEnabled
     */
    public GpsStateChangedEvent(boolean mIsGPSEnabled) {
        this.mIsGPSEnabled = mIsGPSEnabled;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("IsGPSEnabled", mIsGPSEnabled)
                .toString();
    }
}
