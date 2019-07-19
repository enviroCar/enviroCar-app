package org.envirocar.core.exception;

/**
 * @author dewall
 */
public class MailNotConfirmedException extends UnauthorizedException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor with message.
     * @param message
     */
    public MailNotConfirmedException(String message) {
        super(message);
    }
}
