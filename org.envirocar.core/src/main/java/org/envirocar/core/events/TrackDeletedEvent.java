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
package org.envirocar.core.events;

import androidx.annotation.NonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import org.envirocar.core.entity.Track;

/**
 * @author dewall
 */
public class TrackDeletedEvent {

    public final Track track;

    /**
     * Constructor.
     *
     * @param track the deleted track.
     */
    public TrackDeletedEvent(Track track) {
        Preconditions.checkNotNull(track);
        this.track = track;
    }

    @NonNull
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(track)
                .add("Name", track.getName())
                .add("id", track.getTrackID())
                .toString();
    }
}
