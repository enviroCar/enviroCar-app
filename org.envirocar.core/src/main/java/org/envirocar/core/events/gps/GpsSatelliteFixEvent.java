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

import com.google.common.base.MoreObjects;


/**
 * Event holder that holds all necessary information about a GpsSatelliteFix.
 *
 * @author dewall
 */
public class GpsSatelliteFixEvent {

    public final GpsSatelliteFix mGpsSatelliteFix;

    /**
     * Constructor.
     *
     * @param gpsSatelliteFix input satellite fix.
     */
    public GpsSatelliteFixEvent(GpsSatelliteFix gpsSatelliteFix) {
        this.mGpsSatelliteFix = gpsSatelliteFix;
    }

    /**
     * Constructor.
     *
     * @param numberOfSats number of satellites.
     * @param fix          is fixed.
     */
    public GpsSatelliteFixEvent(int numberOfSats, boolean fix) {
        this(new GpsSatelliteFix(numberOfSats, fix));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("holded event info", mGpsSatelliteFix.toString())
                .toString();
    }
}
