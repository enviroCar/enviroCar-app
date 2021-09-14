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
public interface UserStatistic extends BaseEntity<UserStatistic> {
    String KEY_TRACKCOUNT = "trackCount";
    String KEY_DURATION = "duration";
    String KEY_DISTANCE = "distance";

    /**
     * @return the track count
     */
    int getTrackCount();

    /**
     * Sets the track count.
     *
     * @param trackCount
     */
    void setTrackCount(int trackCount);

    /**
     * @return the distance.
     */
    double getDistance();

    /**
     * Sets the distance.
     *
     * @param distance the distance to set.
     */
    void setDistance(double distance);

    /**
     * @return the duration.
     */
    double getDuration();

    /**
     * Sets the duration.
     *
     * @param duration the duration to set.
     */
    void setDuration(double duration);
}
