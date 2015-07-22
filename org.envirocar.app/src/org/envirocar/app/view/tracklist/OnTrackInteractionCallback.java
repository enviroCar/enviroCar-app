package org.envirocar.app.view.tracklist;

import android.view.View;

import org.envirocar.app.storage.Track;

/**
 * @author dewall
 */
interface OnTrackInteractionCallback {

    /**
     *
     * @param track the track to show the details for.
     */
    void onTrackDetailsClicked(Track track, View transitionView);

    /**
     *
     * @param track the track to delete.
     */
    void onDeleteTrackClicked(Track track);

    /**
     *
     * @param track the track to upload.
     */
    void onUploadTrackClicked(Track track);

    /**
     *
     * @param track the track to export.
     */
    void onExportTrackClicked(Track track);
}
