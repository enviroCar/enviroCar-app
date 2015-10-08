package org.envirocar.core.exception;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class DataCreationFailureException extends DAOException {

    /**
     * Constructor.
     *
     * @param e the caught exception.
     */
    public DataCreationFailureException(Exception e) {
        super(e);
    }

    /**
     * Constructor.
     *
     * @param string the custom error message.
     */
    public DataCreationFailureException(String string) {
        super(string);
    }
}
