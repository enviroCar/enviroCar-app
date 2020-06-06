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
        StringTokenizer stringTokenizer = null;
        List<Vehicles> vehiclesList = new ArrayList<>();

        try {
            while ((line = reader.readLine()) != null) {

                //tokenizing the string on , and set vehicles attributes

                stringTokenizer = new StringTokenizer(line, ",");
                Vehicles vehicles = new Vehicles();
                vehicles.setManufacturer_id(stringTokenizer.nextToken());
                vehicles.setId(stringTokenizer.nextToken());
                stringTokenizer.nextToken();
                vehicles.setTrade(stringTokenizer.nextToken());
                vehicles.setCommerical_name(stringTokenizer.nextToken());
                vehicles.setAllotment_date(stringTokenizer.nextToken());
                vehicles.setCategory(stringTokenizer.nextToken());
                vehicles.setBodywork(stringTokenizer.nextToken());
                vehicles.setPower_source_id(stringTokenizer.nextToken());
                vehicles.setPower(stringTokenizer.nextToken());
                vehicles.setEngine_capacity(stringTokenizer.nextToken());
                vehicles.setAxles(stringTokenizer.nextToken());
                vehicles.setPowered_axles(stringTokenizer.nextToken());
                vehicles.setSeats(stringTokenizer.nextToken());
                vehicles.setMaximum_mass(stringTokenizer.nextToken());
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
        StringTokenizer stringTokenizer = null;
        List<PowerSource> powerSourceList = new ArrayList<>();

        try {
            while ((line = reader.readLine()) != null) {

                //tokenizing the string on , and set power_sources attributes

                stringTokenizer = new StringTokenizer(line, ",");
                PowerSource powerSource = new PowerSource();
                powerSource.setId(stringTokenizer.nextToken());
                powerSource.setShort_name(stringTokenizer.nextToken());
                powerSource.setDescription(stringTokenizer.nextToken());
                powerSourceList.add(powerSource);

            }
        } catch (IOException e) {
            Log.v("fileRead", e.getMessage());
        }
        return powerSourceList;
    }
}
