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

import org.envirocar.core.dao.BaseRemoteDAO;
import org.envirocar.core.dao.CarDAO;
import org.envirocar.core.entity.Car;
import org.envirocar.core.exception.DataCreationFailureException;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.service.CarService;
import org.envirocar.remote.service.EnviroCarService;
import org.envirocar.remote.util.EnvirocarServiceUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import retrofit.Call;
import retrofit.Response;
import rx.Observable;
import rx.functions.Func1;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class RemoteCarDAO extends BaseRemoteDAO<CarDAO> implements CarDAO {
    private static final Logger LOG = Logger.getLogger(RemoteCarDAO.class);

    /**
     * Constructor.
     *
     * @param cacheDao cache dao for accessing local cars.
     */
    public RemoteCarDAO(CarDAO cacheDao) {
        super(cacheDao);
    }

    @Override
    public List<Car> getAllCars() throws NotConnectedException, DataRetrievalFailureException {
        return getAllCars(1);
    }

    /**
     * @param page the page to request (limit of cars per page is set to 100)
     * @return
     * @throws DataRetrievalFailureException
     */
    private List<Car> getAllCars(int page) throws DataRetrievalFailureException {
        final CarService carService = EnviroCarService.getCarService();
        Call<List<Car>> carsCall = carService.getAllCars(page);

        try {
            Response<List<Car>> carsResponse = carsCall.execute();
            List<Car> result = carsResponse.body();

            // Check whether this request was not the last page.
            boolean hasNextPage = EnvirocarServiceUtils.hasNextPage(carsResponse);

            // if this is not the last page, then recursively call the method to get
            // all sensor values of the next pages.
            if (hasNextPage) {
                result.addAll(getAllCars(page + 1));
            }

            return result;
        } catch (IOException e) {
            throw new DataRetrievalFailureException(e);
        }
    }

    @Override
    public Observable<List<Car>> getAllCarsObservable() {
        return getAllCarsObservable(1);
    }

    @Override
    public String createCar(Car car) throws NotConnectedException, DataCreationFailureException,
            UnauthorizedException {
        CarService carService = EnviroCarService.getCarService();
        Call<Car> createCarCall = carService.createCar(car);

        try {
            Response<Car> createCarResponse = createCarCall.execute();

            if (!createCarResponse.isSuccess()) {
                LOG.warn("error while creating remote car: " + createCarResponse.code());
            }

            // Get all headers in order to find out the location of the uploaded car.
            Map<String, List<String>> headerListMap = createCarResponse.headers().toMultimap();

            // Get the header of the location
            String location = "";
            if (headerListMap.containsKey("Location")) {
                for (String locationHeader : headerListMap.get("Location")) {
                    location += locationHeader;
                }
            }
            LOG.info("location: " + location);

            // Return the remote location of the header
            return location.substring(location.lastIndexOf("/") + 1, location.length());
        } catch (IOException e) {
            throw new NotConnectedException(e);
        }
    }

    /**
     * This method recursively calls the enviroCar service until each page has been requested.
     *
     * @param page the page to request the data from.
     * @return an observable stream of data.
     */
    private Observable<List<Car>> getAllCarsObservable(final int page) {
        final CarService carService = EnviroCarService.getCarService();
        Call<List<Car>> carsCall = carService.getAllCars(page);

        // return an observeable that returns a list of cars for every 100 cars.
        return Observable.just(carsCall)
                .concatMap(new Func1<Call<List<Car>>, Observable<? extends List<Car>>>() {
                    @Override
                    public Observable<? extends List<Car>> call(Call<List<Car>> listCall) {
                        boolean hasNextPage = false;
                        Response<List<Car>> response = null;
                        try {
                            // Execute the call.
                            response = listCall.execute();

                            Observable<List<Car>> res = Observable.just(response.body());
                            // Search for "rel=last". If this exists, then this was not the last
                            // page.
                            if (EnvirocarServiceUtils.hasNextPage(response)) {
                                // If this is not the last page, then recursively concatenate with
                                // other observables.
                                res = res.concatWith(getAllCarsObservable(page + 1));
                            }
                            return res;
                        } catch (IOException e) {
                            // Return an error observable that invokes the observers onError method.
                            return Observable.error(new DataRetrievalFailureException(e));
                        }
                    }
                });
    }
}
