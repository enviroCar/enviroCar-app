package org.envirocar.app.events;

import org.envirocar.core.entity.Track;

public class TrackchunkEndUploadedEvent {

    private Track track;

    public TrackchunkEndUploadedEvent(Track track) {
        this.track = track;
    }

    public Track getTrack() {
        return this.track;
    }

    @Override
    public String toString() {
        return super.toString();
    }

}
