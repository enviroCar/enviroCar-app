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
package org.envirocar.core.dao;


import android.security.keystore.UserNotAuthenticatedException;

import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.User;
import org.envirocar.core.exception.DataCreationFailureException;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;

import java.util.List;

import rx.Observable;

/**p
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface CarDAO {

    List<Car> getAllCars() throws NotConnectedException, DataRetrievalFailureException;

    Observable<List<Car>> getAllCarsObservable();

    List<Car> getCarsByUser(User user) throws UserNotAuthenticatedException,
            NotConnectedException, DataRetrievalFailureException, UnauthorizedException;

    Observable<List<Car>> getCarsByUserObservable(User user);

    String createCar(Car car) throws NotConnectedException, DataCreationFailureException,
            UnauthorizedException;

    Observable<Car> createCarObservable(Car car);
}
