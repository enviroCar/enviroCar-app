package org.envirocar.core.exception;

import org.envirocar.core.entity.Car;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class UnsupportedFuelTypeException extends Exception {
    private static final long serialVersionUID = -4241408172063088244L;

    /**
     * Constructor.
     *
     * @param f the fueltype that is not supported.
     */
    public UnsupportedFuelTypeException(Car.FuelType f) {
        this(f.toString());
    }


    /**
     * Constructor.
     *
     * @param msg the error message of the exception.
     */
    public UnsupportedFuelTypeException(String msg) {
        super(String.format("FuelType '%s' is not supported.", msg));
    }
}
