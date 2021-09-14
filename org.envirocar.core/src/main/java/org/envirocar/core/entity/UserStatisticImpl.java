/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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
package org.envirocar.core.entity;

/**
 * @author dewall
 */
public class UserStatisticImpl implements UserStatistic {

    private int trackCount;
    private double distance;
    private double duration;

    public UserStatisticImpl(){
    }

    public UserStatisticImpl(int trackCount, double distance, double duration) {
        this.trackCount = trackCount;
        this.distance = distance;
        this.duration = duration;
    }

    @Override
    public int getTrackCount() {
        return trackCount;
    }

    @Override
    public void setTrackCount(int trackCount) {
        this.trackCount = trackCount;
    }

    @Override
    public double getDistance() {
        return distance;
    }

    @Override
    public void setDistance(double distance) {
        this.distance = distance;
    }

    @Override
    public double getDuration() {
        return duration;
    }

    @Override
    public void setDuration(double duration) {
        this.duration = duration;
    }

    @Override
    public UserStatistic carbonCopy() {
        UserStatisticImpl other = new UserStatisticImpl();
        other.distance = distance;
        other.duration = duration;
        return other;
    }
}
