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
package org.envirocar.app.views.tracklist;

import android.view.View;

import org.envirocar.core.entity.Track;


/**
 * The interface On track interaction callback.
 *
 * @author dewall
 */
interface OnTrackInteractionCallback {

    /**
     * On track details clicked.
     *
     * @param track          the track to show the details for.
     * @param transitionView the transition view
     */
    void onTrackMapClicked(Track track, View transitionView);

    /**
     * On delete track clicked.
     *
     * @param track the track to delete.
     */
    void onDeleteTrackClicked(Track track);

    /**
     * On upload track clicked.
     *
     * @param track the track to upload.
     */
    void onUploadTrackClicked(Track track);

    /**
     * On export track clicked.
     *
     * @param track the track to export.
     */
    void onExportTrackClicked(Track track);

    /**
     * On download track clicked.
     *
     * @param track  the track to download.
     * @param holder the holder
     */
    void onDownloadTrackClicked(Track track, AbstractTrackListCardAdapter.TrackCardViewHolder holder);

    /**
     * Show toast.
     *
     * @param message the message to toast
     */
    void showToast(String message);

    /**
     * On track stats button clicked.
     *
     * @param track the track
     */
    void onTrackStatsClicked(Track track);
}