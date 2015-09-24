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

import org.envirocar.app.model.Car;
import org.envirocar.app.model.dao.exception.NotConnectedException;
import org.envirocar.app.model.dao.exception.ResourceConflictException;
import org.envirocar.app.model.dao.exception.SensorRetrievalException;
import org.envirocar.app.model.dao.exception.UnauthorizedException;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import rx.Observable;

public interface SensorDAO {

    /**
     * an implementation shall return the list of sensors its data backend
     * contains.
     *
     * @return the list of all sensors provided through this DAO
     * @throws SensorRetrievalException if the data backend cannot be accessed
     */
    List<Car> getAllSensors() throws SensorRetrievalException;

    /**
     * Returns the list of all sensors as an observable stream of chunks each one containing at
     * most 100 sensors. Thus, the implementation wraps around the pagination feature.
     *
     * @return
     */
    Observable<List<Car>> getAllSensorsObservable();

    /**
     * an implementation shall save the given car at the underlying
     * data backend or throw a {@link NotConnectedException} if it cannot
     * store the car.
     *
     * @param car the sensor to save
     * @return the ID of the saved sensor as provided by the underlying DAO
     * @throws NotConnectedException
     * @throws ResourceConflictException
     * @throws UnauthorizedException
     */
    public String saveSensor(Car car) throws NotConnectedException, UnauthorizedException;


    public static class SensorDAOUtil {

        public static List<Car> sortByManufacturer(List<Car> sensors) {
            final Collator collator = Collator.getInstance(Locale.ENGLISH);

            Collections.sort(sensors, new Comparator<Car>() {
                @Override
                public int compare(Car lhs, Car rhs) {
                    if (lhs.getManufacturer().equals(rhs.getManufacturer())) {
                        return collator.compare(lhs.getModel(), rhs.getModel());
                    } else {
                        return collator.compare(lhs.getManufacturer(), rhs.getManufacturer());
                    }
                }
            });

            return sensors;
        }

    }
}
