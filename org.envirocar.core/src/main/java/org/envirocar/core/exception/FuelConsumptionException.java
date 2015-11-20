/**
 * Copyright (C) 2013 - 2015 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
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
