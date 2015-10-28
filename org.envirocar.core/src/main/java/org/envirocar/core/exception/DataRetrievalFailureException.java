package org.envirocar.core.exception;

import org.envirocar.core.exception.DAOException;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class DataRetrievalFailureException extends DAOException {

    /**
     * Constructor.
     *
     * @param e the caught exception.
     */
    public DataRetrievalFailureException(Exception e) {
        super(e);
    }

    /**
     * Constructor.
     *
     * @param string the custom error message.
     */
    public DataRetrievalFailureException(String string) {
        super(string);
    }
}
