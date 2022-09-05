package org.envirocar.app.events;

import org.envirocar.core.entity.Track;

public class TrackRecordingContinueEvent {
    private Track track;

    public TrackRecordingContinueEvent(Track track) {
        this.track = track;
    }

    public Track getTrack() {
        return track;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
