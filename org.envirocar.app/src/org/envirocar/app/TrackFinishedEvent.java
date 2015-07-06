package org.envirocar.app;

import com.google.common.base.MoreObjects;

import org.envirocar.app.storage.Track;

/**
 * @author dewall
 */
public class TrackFinishedEvent {

    public final Track mTrack;

    /**
     * Constructor.
     *
     * @param track the finished track
     */
    public TrackFinishedEvent(final Track track){
        this.mTrack = track;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("TrackID", mTrack != null ? mTrack.getTrackId() : "null")
                .toString();
    }
}
