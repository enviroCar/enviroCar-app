package org.envirocar.core.exception;

import org.envirocar.core.entity.Track;

/**
 * @author dewall
 */
public class TrackUploadException extends Exception {

    public enum Reason {
        NOT_ENOUGH_MEASUREMENTS,
        TRACK_WITH_NO_VALID_CAR,
        TRACK_ALREADY_UPLOADED,
        NO_NETWORK_CONNECTION,
        NOT_LOGGED_IN,
        NO_CAR_ASSIGNED,
        GPS_TRACKS_NOT_ALLOWED,
        UNAUTHORIZED,
        UNKNOWN
    }

    private final Track track;
    private final Reason reason;

    public TrackUploadException(Track track, Reason reason) {
        this(track, reason, null);
    }

    public TrackUploadException(Track track, Reason reason, Throwable throwable) {
        super(throwable);
        this.track = track;
        this.reason = reason;
    }

    public Track getTrack() {
        return track;
    }

    public Reason getReason() {
        return reason;
    }
}
