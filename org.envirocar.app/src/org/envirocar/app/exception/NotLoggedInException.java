package org.envirocar.app.exception;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class NotLoggedInException extends Exception {

    /**
     * Constructor.
     *
     * @param message the error message.
     */
    public NotLoggedInException(String message) {
        super(message);
    }
}
