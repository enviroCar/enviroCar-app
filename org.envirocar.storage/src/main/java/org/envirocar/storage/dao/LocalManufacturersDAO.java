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

import org.envirocar.core.entity.Manufacturers;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;


@Dao
public interface LocalManufacturersDAO extends BaseLocalCarDAO<Manufacturers> {

    @Query("SELECT * FROM manufacturers")
    Observable<List<Manufacturers>> getAllManufacturers();

    @Query("INSERT INTO manufacturers(id, name)\n" +
            "  SELECT DISTINCT\n" +
            "    manufacturer_id AS id,\n" +
            "    manufacturer AS name\n" +
            "  FROM (\n" +
            "    SELECT \n" +
            "      manufacturer_id, \n" +
            "      max(allotment_date) as allotment_date\n" +
            "    FROM vehicles\n" +
            "WHERE manufacturer != 'manufacturer plaintext'" +
            "    GROUP BY manufacturer_id\n" +
            "  ) AS dates\n" +
            "  JOIN vehicles AS v USING (manufacturer_id)\n" +
            "  WHERE v.allotment_date = dates.allotment_date")
    void inserManufacturer();
}
