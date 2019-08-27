package org.envirocar.app.exception;

/**
 * @author dewall
 */
public class CacheEmptyException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    public CacheEmptyException() {
        super("Cache is empty.");
    }

    /**
     * Constructor.
     *
     * @param detailMessage the error message.
     */
    public CacheEmptyException(String detailMessage) {
        super(detailMessage);
    }
}
