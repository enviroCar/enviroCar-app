package org.envirocar.app.exception;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class NotAcceptedTermsOfUseException extends Exception {

    /**
     * Constructor.
     */
    public NotAcceptedTermsOfUseException() {
        super("User has not accepted the terms of use.");
    }

    /**
     * Constructor.
     *
     * @param detailMessage the error message.
     */
    public NotAcceptedTermsOfUseException(String detailMessage) {
        super(detailMessage);
    }
}
