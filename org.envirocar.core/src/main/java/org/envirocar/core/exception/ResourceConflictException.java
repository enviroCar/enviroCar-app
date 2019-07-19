/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.core.exception;

/**
 * @author dewall
 */
public class ResourceConflictException extends DAOException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public enum ConflictType {
        USERNAME,
        MAIL,
        UNKNOWN
    }

    private final ConflictType conflictType;

    /**
     * Constructor with unknown conflictType
     *
     * @param error
     */
    public ResourceConflictException(String error) {
        this(error, ConflictType.UNKNOWN);
    }

    /**
     * Constructor with conflictType
     *
     * @param error
     * @param conflictType
     */
    public ResourceConflictException(String error, ConflictType conflictType) {
        super(error);
        this.conflictType = conflictType;
    }

    /**
     * Getter
     *
     * @return the conflictType
     */
    public ConflictType getConflictType() {
        return conflictType;
    }
}
