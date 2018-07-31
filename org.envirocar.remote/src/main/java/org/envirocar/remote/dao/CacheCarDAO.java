/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.remote.dao;

import android.security.keystore.UserNotAuthenticatedException;

import org.envirocar.core.dao.AbstractCacheDAO;
import org.envirocar.core.dao.CarDAO;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.User;
import org.envirocar.core.exception.DataCreationFailureException;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.logging.Logger;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class CacheCarDAO extends AbstractCacheDAO implements CarDAO {
    private static final Logger logger = Logger.getLogger(CacheCarDAO.class);
    public static final String CAR_CACHE_FILE_NAME = "cache_cars";

    @Inject
    public CacheCarDAO() {
    }

    @Override
    public List<Car> getAllCars() {
        return null;
    }

    @Override
    public Observable<List<Car>> getAllCarsObservable() {
        return Observable.error(new NoSuchMethodException("Not implemented yet!"));
    }

    @Override
    public List<Car> getCarsByUser(User user) {
        return null;
    }

    @Override
    public Observable<List<Car>> getCarsByUserObservable(User user) {
        return null;
    }

    @Override
    public String createCar(Car car) {
        return null;
    }

    @Override
    public Observable<Car> createCarObservable(Car car) {
        return Observable.error(new NoSuchMethodException("Not implemented yet!"));
    }
}
