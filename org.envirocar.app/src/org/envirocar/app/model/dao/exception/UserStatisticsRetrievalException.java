package org.envirocar.app.model.dao.exception;

/**
 * @author dewall
 */
public class UserStatisticsRetrievalException extends DAOException{
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param exception the exception string.
     */
    public UserStatisticsRetrievalException(String exception){
        super(exception);
    }

    /**
     * Constructor.
     *
     * @param e the enclosed exception.
     */
    public UserStatisticsRetrievalException(Exception e){
        super(e);
    }
}
