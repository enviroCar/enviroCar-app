/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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
package org.envirocar.storage.dao;

import androidx.room.Dao;
import androidx.room.Query;

import org.envirocar.core.entity.Vehicles;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface LocalVehicleDAO extends BaseLocalCarDAO<Vehicles> {

    @Query("SELECT * FROM vehicles")
    Single<List<Vehicles>> getManufacturerVehiclesList();

    @Query("SELECT * FROM vehicles WHERE manufacturer_id = :manuId")
    Observable<List<Vehicles>> getManufacturerVehiclesId(String manuId);

    @Query("SELECT * FROM vehicles WHERE manufacturer = :manuName")
    Observable<List<Vehicles>> getManufacturerVehicles(String manuName);

    @Query("SELECT * FROM vehicles where manufacturer_id = :manuId AND id = :vehicleId")
    Single<Vehicles> getHsnTsnVehicle(String manuId, String vehicleId);

    @Query("SELECT * FROM vehicles where manufacturer=:manuName AND commerical_name =:model AND allotment_date LIKE '%' || :year || '%'")
    Single<List<Vehicles>> getVehicleAttributeType(String manuName, String model, String year);

}
