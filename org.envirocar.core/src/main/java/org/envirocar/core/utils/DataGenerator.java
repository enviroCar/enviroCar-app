package org.envirocar.core.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

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
        BufferedReader reader = readFile(context, fileName);
        String line = "";

        List<Vehicles> vehiclesList = new ArrayList<>();

        try {
            while ((line = reader.readLine()) != null) {

                //tokenizing the string on , and set vehicles attributes

                String[] tokens = line.split(",");
                Vehicles vehicles = new Vehicles();
                vehicles.setManufacturer_id(tokens[0]);
                vehicles.setId(tokens[1]);

                vehicles.setManufacturer(tokens[2]);

                vehicles.setTrade(tokens[3]);

                vehicles.setCommerical_name(tokens[4]);

                vehicles.setAllotment_date(tokens[5]);

                vehicles.setCategory(tokens[6]);

                vehicles.setBodywork(tokens[7]);

                vehicles.setPower_source_id(tokens[8]);

                vehicles.setPower(tokens[9]);

                vehicles.setEngine_capacity(tokens[10]);

                vehicles.setAxles(tokens[11]);

                vehicles.setPowered_axles(tokens[12]);

                vehicles.setSeats(tokens[13]);
                vehicles.setMaximum_mass(tokens[14]);
                vehiclesList.add(vehicles);


            }
        } catch (IOException e) {
            Log.v("fileRead", e.getMessage());
        }
        return vehiclesList;
    }

    public static List<PowerSource> getPowerSources(Context context, String fileName) {
        BufferedReader reader = readFile(context, fileName);
        String line = "";
        List<PowerSource> powerSourceList = new ArrayList<>();

        try {
            while ((line = reader.readLine()) != null) {

                //tokenizing the string on , and set power_sources attributes

                String[] tokens = line.split(",");
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
