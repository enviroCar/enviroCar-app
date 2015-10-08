package org.envirocar.core.exception;

/**
 * TODO JavaDoc
 * @author dewall
 */
public class DataUpdateFailureException extends DAOException{

    /**
     * Constructor.
     *
     * @param e the caught exception.
     */
    public DataUpdateFailureException(Exception e) {
        super(e);
    }

    /**
     * Constructor.
     *
     * @param string the custom error message.
     */
    public DataUpdateFailureException(String string) {
        super(string);
    }
}
