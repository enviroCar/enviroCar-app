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
package org.envirocar.core.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.opencsv.CSVReader;

import org.envirocar.core.entity.PowerSource;
import org.envirocar.core.entity.Vehicles;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


public class DataGenerator {

    public static AssetManager assetManager;
    public static InputStream inputStream;

    public static BufferedReader readFile(Context context, String fileName) {
        assetManager = context.getAssets();
        try {
            inputStream = assetManager.open("database/" + fileName + ".csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
    }

    public static List<Vehicles> getVehicleData(Context context, String fileName) {
        CSVReader csvReader = new CSVReader(readFile(context, fileName));
        String tokens[] = null;

        List<Vehicles> vehiclesList = new ArrayList<>();

        try {
            while ((tokens = csvReader.readNext()) != null) {


                Vehicles vehicles = new Vehicles();
                Log.i("dataVehicle", " " + tokens[8]);
                vehicles.setManufacturer_id(tokens[0]);
                vehicles.setId(tokens[1]);
                vehicles.setManufacturer(tokens[2]);
                if (tokens[3] == null)
                    vehicles.setTrade(" ");
                else
                    vehicles.setTrade(tokens[3]);
                if (tokens[4] == null)
                    vehicles.setCommerical_name(" ");
                else
                    vehicles.setCommerical_name(tokens[4]);
                if (tokens[5] == null)
                    vehicles.setAllotment_date(" ");
                else
                    vehicles.setAllotment_date(tokens[5]);
                if (tokens[6] == null)
                    vehicles.setCategory(" ");
                else
                    vehicles.setCategory(tokens[6]);
                if (tokens[7] == null)
                    vehicles.setBodywork(" ");
                else
                    vehicles.setBodywork(tokens[7]);
                if (tokens[8] == null)
                    vehicles.setPower_source_id(" ");
                else
                    vehicles.setPower_source_id(tokens[8]);
                if (tokens[9] == null)
                    vehicles.setPower(" ");
                else
                    vehicles.setPower(tokens[9]);
                if (tokens[10] == null)
                    vehicles.setEngine_capacity(" ");
                else
                    vehicles.setEngine_capacity(tokens[10]);
                if (tokens[11] == null)
                    vehicles.setAxles(" ");
                else
                    vehicles.setAxles(tokens[11]);
                if (tokens[12] == null)
                    vehicles.setPowered_axles(" ");
                else
                    vehicles.setPowered_axles(tokens[12]);
                if (tokens[13] == null)
                    vehicles.setSeats(" ");
                else
                    vehicles.setSeats(tokens[13]);
                if (tokens[14] == null)
                    vehicles.setMaximum_mass(" ");
                else
                    vehicles.setMaximum_mass(tokens[14]);
                vehiclesList.add(vehicles);


            }
        } catch (IOException e) {
            Log.v("fileRead", e.getMessage());
        }
        return vehiclesList;
    }

    public static List<PowerSource> getPowerSources(Context context, String fileName) {
        List<PowerSource> powerSourceList = new ArrayList<>();
        CSVReader csvReader = new CSVReader(readFile(context, fileName));
        String tokens[] = null;
        try {
            while ((tokens = csvReader.readNext()) != null) {

                PowerSource powerSource = new PowerSource();
                powerSource.setId(tokens[0]);
                powerSource.setShort_name(tokens[1]);
                powerSource.setDescription(tokens[2]);
                powerSourceList.add(powerSource);

            }
        } catch (IOException e) {
            Log.v("fileRead", e.getMessage());
        }
        return powerSourceList;
    }
}
