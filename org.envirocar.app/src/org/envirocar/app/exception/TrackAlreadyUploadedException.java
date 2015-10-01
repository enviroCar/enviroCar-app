package org.envirocar.app.exception;

/**
 * @author dewall
 */
public class TrackAlreadyUploadedException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param detailMessage
     */
    public TrackAlreadyUploadedException(String detailMessage) {
        super(detailMessage);
    }
}
