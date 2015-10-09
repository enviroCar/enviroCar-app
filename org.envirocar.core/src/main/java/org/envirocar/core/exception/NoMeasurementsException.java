package org.envirocar.core.exception;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class NoMeasurementsException extends Exception {

    /**
     * Constructor.
     *
     * @param detailMessage the detail message of the error.
     */
    public NoMeasurementsException(String detailMessage) {
        super(detailMessage);
    }
}
