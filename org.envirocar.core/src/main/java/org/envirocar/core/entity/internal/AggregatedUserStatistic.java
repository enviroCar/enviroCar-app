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
package org.envirocar.core.entity.internal;

/**
 * @author dewall
 */
public class AggregatedUserStatistic {

    private int numTracks;
    private double totalDuration;
    private double totalDistance;

    public AggregatedUserStatistic() {
        this(0, 0 , 0);
    }

    public AggregatedUserStatistic(int numTracks, double totalDuration, double totalDistance) {
        this.numTracks = numTracks;
        this.totalDuration = totalDuration;
        this.totalDistance = totalDistance;
    }

    public int getNumTracks() {
        return numTracks;
    }

    public double getTotalDuration() {
        return totalDuration;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setNumTracks(int numTracks) {
        this.numTracks = numTracks;
    }

    public void setTotalDuration(double totalDuration) {
        this.totalDuration = totalDuration;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }
}
