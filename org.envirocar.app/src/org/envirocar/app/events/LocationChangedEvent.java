package org.envirocar.app.events;

import android.location.Location;

import com.google.common.base.MoreObjects;

/**
 * @author dewall
 */
public class LocationChangedEvent {

    public final Location mLocation;

    /**
     * Constructor.
     *
     * @param location  the new location.
     */
    public LocationChangedEvent(final Location location){
        this.mLocation = location;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Latitude", mLocation.getLatitude())
                .add("Longitude", mLocation.getLongitude())
                .add("Altitude", mLocation.getAltitude())
                .add("Accuracy", mLocation.getAccuracy()).toString();
    }
}
