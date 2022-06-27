package org.envirocar.app.events;

public class TrackchunkUploadEvent {

    public static final int FAILED = 0;
    public static final int SUCCESSFUL = 1;

    public int getStatus() {
        return status;
    }

    private int status;

    public TrackchunkUploadEvent(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return super.toString();
    }

}
