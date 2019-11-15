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
