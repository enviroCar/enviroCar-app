package org.envirocar.core.exception;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class FuelConsumptionException extends Exception {
    private static final long serialVersionUID = 7331880723569229640L;

    /**
     * Constructor.
     */
    public FuelConsumptionException() {
        this("Error in determining the fuel consumption.");
    }

    /**
     * Constructor.
     *
     * @param string the error message.
     */
    public FuelConsumptionException(String string) {
        super(string);
    }

    /**
     * Constructor.
     *
     * @param e the error to wrap.
     */
    public FuelConsumptionException(Throwable e) {
        super(e);
    }
}
