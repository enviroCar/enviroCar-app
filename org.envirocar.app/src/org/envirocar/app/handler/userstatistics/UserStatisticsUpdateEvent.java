/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
package org.envirocar.app.handler.userstatistics;

import com.google.common.base.MoreObjects;

/**
 * @author dewall
 */
public class UserStatisticsUpdateEvent {

    public final int numTracks;
    public final double totalDistance;
    public final long totalDuration;

    /**
     * Constructor.
     *
     * @param numTracks
     * @param totalDistance
     * @param totalDuration
     */
    public UserStatisticsUpdateEvent(int numTracks, double totalDistance, long totalDuration) {
        this.numTracks = numTracks;
        this.totalDistance = totalDistance;
        this.totalDuration = totalDuration;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Number of Tracks", numTracks)
                .add("Total Distance", totalDistance)
                .add("Total Duration", totalDuration)
                .toString();
    }
}
