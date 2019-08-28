package org.envirocar.app.exception;

/**
 * @author dewall
 */
public class LoginException extends Exception {

    public enum ErrorType {
        MAIL_NOT_CONFIREMED,
        UNABLE_TO_COMMUNICATE_WITH_SERVER,
        PASSWORD_INCORRECT
    }

    private final ErrorType type;

    /**
     * Constructor
     *
     * @param message
     * @param type
     */
    public LoginException(String message, ErrorType type) {
        super(message);
        this.type = type;
    }

    /**
     * Returns the errotypes
     *
     * @return the type
     */
    public ErrorType getType() {
        return type;
    }
}