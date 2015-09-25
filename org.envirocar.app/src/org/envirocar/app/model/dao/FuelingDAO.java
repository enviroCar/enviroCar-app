/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.app.model.dao;

import org.envirocar.app.exception.InvalidObjectStateException;
import org.envirocar.app.model.Fueling;
import org.envirocar.app.model.dao.exception.FuelingRetrievalException;
import org.envirocar.app.model.dao.exception.NotConnectedException;
import org.envirocar.app.model.dao.exception.UnauthorizedException;

import java.util.List;

import rx.Observable;

/**
 * Data access object interface that defines the access to the fueling data of a user.
 *
 * @author dewall
 */
public interface FuelingDAO {

    /**
     * Stores a given fueling
     *
     * @param fueling the fueling to store.
     * @throws NotConnectedException
     * @throws InvalidObjectStateException
     */
    void storeFueling(Fueling fueling) throws NotConnectedException, InvalidObjectStateException,
            UnauthorizedException;

    /**
     * Gets the list of fuelings for a given user.
     *
     * @return the list of fuelings for a given user.
     * @throws FuelingRetrievalException
     */
    List<Fueling> getFuelings() throws FuelingRetrievalException;

    /**
     * Gets an observable of fuelings for a given user.
     *
     * @return the observable of fuelings for a given user.
     * @throws FuelingRetrievalException
     */
    Observable<List<Fueling>> getFuelingsObservable();

}
