package org.envirocar.storage.dao;

import androidx.room.Dao;
import androidx.room.Query;

import org.envirocar.core.entity.Manufacturers;

import java.util.List;

import io.reactivex.Single;


@Dao
public interface LocalManufacturersDAO extends BaseLocalCarDAO<Manufacturers> {

    @Query("SELECT * FROM manufacturers")
    Single<List<Manufacturers>> getAllManufacturers();

    @Query("INSERT INTO manufacturers(id, name)\n" +
            "  SELECT DISTINCT\n" +
            "    manufacturer_id AS id,\n" +
            "    manufacturer AS name\n" +
            "  FROM (\n" +
            "    SELECT \n" +
            "      manufacturer_id, \n" +
            "      max(allotment_date) as allotment_date\n" +
            "    FROM vehicles\n" +
            "    GROUP BY manufacturer_id\n" +
            "  ) AS dates\n" +
            "  JOIN vehicles AS v USING (manufacturer_id)\n" +
            "  WHERE v.allotment_date = dates.allotment_date")
    void inserManufacturer();
}
