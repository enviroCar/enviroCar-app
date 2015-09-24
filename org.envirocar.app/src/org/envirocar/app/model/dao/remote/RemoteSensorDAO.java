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
package org.envirocar.app.model.dao.remote;

import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.dao.SensorDAO;
import org.envirocar.app.model.dao.cache.CacheSensorDAO;
import org.envirocar.app.model.dao.exception.NotConnectedException;
import org.envirocar.app.model.dao.exception.SensorRetrievalException;
import org.envirocar.app.model.dao.exception.UnauthorizedException;
import org.envirocar.app.model.dao.service.CarService;
import org.envirocar.app.model.dao.service.EnviroCarService;
import org.envirocar.app.model.dao.service.utils.EnvirocarServiceUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import retrofit.Call;
import retrofit.Response;
import rx.Observable;

public class RemoteSensorDAO extends BaseRemoteDAO implements SensorDAO, AuthenticatedDAO {

    static final Logger LOG = Logger.getLogger(RemoteSensorDAO.class);
    private CacheSensorDAO cache;

    /**
     * Constructor.
     *
     * @param cacheSensorDAO an instance of the DAO for cached sensors.
     */
    public RemoteSensorDAO(CacheSensorDAO cacheSensorDAO) {
        this.cache = cacheSensorDAO;
    }


    @Override
    public List<Car> getAllSensors() throws SensorRetrievalException {
        //        List<JSONObject> parentObject = readRemoteResource("/sensors", true);
        //        if (cache != null) {
        //            try {
        //                cache.storeAllSensors(parentObject);
        //            } catch (IOException e) {
        //                LOG.warn(e.getMessage());
        //            }
        //        }

        // Start returning all sensor values of the first page.
        return getAllSensors(1);
    }

    /**
     * @param page the page to request (limit of cars per page is set to 100)
     * @return
     * @throws SensorRetrievalException
     */
    private List<Car> getAllSensors(int page) throws SensorRetrievalException {
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
                result.addAll(getAllSensors(page + 1));
            }

            return result;
        } catch (IOException e) {
            throw new SensorRetrievalException(e);
        }
    }

    @Override
    public Observable<List<Car>> getAllSensorsObservable() {
        // Start returning all sensor values as observable
        return getAllSensorsObservable(1);
    }

    /**
     * This method recursively calls the enviroCar service until each page has been requested.
     *
     * @param page the page to request the data from.
     * @return an observable stream of data.
     */
    private Observable<List<Car>> getAllSensorsObservable(int page) {
        final CarService carService = EnviroCarService.getCarService();
        Call<List<Car>> carsCall = carService.getAllCars(page);

        // return an observeable that returns a list of cars for every 100 cars.
        return Observable.just(carsCall)
                .concatMap(listCall -> {
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
                            res = res.concatWith(getAllSensorsObservable(page + 1));
                        }
                        return res;
                    } catch (IOException e) {
                        // Return an error observable that invokes the observers onError method.
                        return Observable.error(new SensorRetrievalException(e));
                    }
                });
    }

    @Override
    public String saveSensor(Car car) throws NotConnectedException, UnauthorizedException {
        CarService carService = EnviroCarService.getCarService();
        Call<Car> createCarCall = carService.createCar(car);

        try {
            Response<Car> createCarResponse = createCarCall.execute();

            if(!createCarResponse.isSuccess()){
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

    //    private String registerSensor(String sensorString) throws IOException,
    // NotConnectedException,
    //            UnauthorizedException, ResourceConflictException {
    //
    //        HttpPost postRequest = new HttpPost(
    //                ConstantsEnvirocar.BASE_URL + "/sensors");
    //
    //        StringEntity se = new StringEntity(sensorString);
    //        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
    //
    //        postRequest.setEntity(se);
    //
    //        HttpResponse response = super.executePayloadRequest(postRequest);
    //
    //        Header[] h = response.getAllHeaders();
    //
    //        String location = "";
    //        for (int i = 0; i < h.length; i++) {
    //            if (h[i].getName().equals("Location")) {
    //                location += h[i].getValue();
    //                break;
    //            }
    //        }
    //        LOG.info("location: "
    //                + location);
    //
    //        return location.substring(
    //                location.lastIndexOf("/") + 1,
    //                location.length());
    //    }

}
