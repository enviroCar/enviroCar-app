/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
package org.envirocar.core.utils.rx;

public class OptionalOrError<T> extends Optional<T> {

    public static <T> OptionalOrError<T> create(T t) {
        return new OptionalOrError<T>(t);
    }

    public static <T> OptionalOrError<T> create(Exception e) {
        return new OptionalOrError<T>(e);
    }

    private final Exception e;

    /**
     * Constructor.
     *
     * @param optional
     */
    public OptionalOrError(T optional) {
        this(optional, null);
    }

    public OptionalOrError(Exception e) {
        this(null, e);
    }

    public OptionalOrError(T optional, Exception e) {
        super(optional);
        this.e = e;
    }

    public Exception getE() {
        return e;
    }

    public boolean isSuccessful(){
        return e == null;
    }
}
