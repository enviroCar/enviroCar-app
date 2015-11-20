/**
 * Copyright (C) 2013 - 2015 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.core.events.gps;

import android.location.Location;

import com.google.common.base.MoreObjects;

/**
 * @author dewall
 */
public class GpsLocationChangedEvent {

    public final Location mLocation;

    /**
     * Constructor.
     *
     * @param location  the new location.
     */
    public GpsLocationChangedEvent(final Location location){
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
