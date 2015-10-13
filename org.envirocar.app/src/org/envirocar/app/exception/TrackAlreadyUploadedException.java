package org.envirocar.app.exception;

/**
 * @author dewall
 */
public class TrackAlreadyUploadedException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    public TrackAlreadyUploadedException() {
        super("Track is already uploaded");
    }

    /**
     * Constructor.
     *
     * @param detailMessage the error message.
     */
    public TrackAlreadyUploadedException(String detailMessage) {
        super(detailMessage);
    }
}
