package org.envirocar.app.services.trackdetails;

import com.mapbox.mapboxsdk.overlay.PathOverlay;

/**
 * @author dewall
 */
public class TrackPathOverlayEvent {

    public final PathOverlay mTrackOverlay;

    /**
     * Constructor.
     *
     * @param mTrackOverlay
     */
    public TrackPathOverlayEvent(PathOverlay mTrackOverlay) {
        this.mTrackOverlay = mTrackOverlay;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
