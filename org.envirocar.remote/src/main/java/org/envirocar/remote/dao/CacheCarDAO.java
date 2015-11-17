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
package org.envirocar.remote.dao;

import org.envirocar.core.dao.AbstractCacheDAO;
import org.envirocar.core.dao.CarDAO;
import org.envirocar.core.entity.Car;
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
    public CacheCarDAO(){}

    @Override
    public List<Car> getAllCars() throws NotConnectedException, DataRetrievalFailureException {
        return null;
    }

    @Override
    public Observable<List<Car>> getAllCarsObservable() {
        return Observable.error(new NoSuchMethodException("Not implemented yet!"));
    }

    @Override
    public String createCar(Car car) throws NotConnectedException, DataCreationFailureException, UnauthorizedException {
        return null;
    }

    @Override
    public Observable<Car> createCarObservable(Car car) {
        return Observable.error(new NoSuchMethodException("Not implemented yet!"));
    }
}
