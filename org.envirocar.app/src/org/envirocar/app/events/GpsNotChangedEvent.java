package org.envirocar.app.events;

import org.envirocar.app.recording.strategy.RecordingStrategy;

public class GpsNotChangedEvent {

    private long seconds;

    public GpsNotChangedEvent(long seconds) {
        this.seconds = seconds;
    }

    public long getSeconds() {
        return seconds;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
