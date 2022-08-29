package org.envirocar.app.events;

import org.envirocar.core.entity.Track;

public class TrackchunkEndUploadedEvent {

    private String name;

    public TrackchunkEndUploadedEvent(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return super.toString();
    }

}
