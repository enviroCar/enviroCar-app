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
package org.envirocar.remote.serde;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.stream.JsonWriter;

import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.TrackImpl;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.FileWithMetadata;
import org.envirocar.core.util.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.envirocar.core.entity.Measurement.PropertyKey.CALCULATED_MAF;
import static org.envirocar.core.entity.Measurement.PropertyKey.CO2;
import static org.envirocar.core.entity.Measurement.PropertyKey.CONSUMPTION;
import static org.envirocar.core.entity.Measurement.PropertyKey.ENERGY_CONSUMPTION;
import static org.envirocar.core.entity.Measurement.PropertyKey.ENERGY_CONSUMPTION_CO2;
import static org.envirocar.core.entity.Measurement.PropertyKey.ENGINE_LOAD;
import static org.envirocar.core.entity.Measurement.PropertyKey.FUEL_SYSTEM_LOOP;
import static org.envirocar.core.entity.Measurement.PropertyKey.FUEL_SYSTEM_STATUS_CODE;
import static org.envirocar.core.entity.Measurement.PropertyKey.GPS_ACCURACY;
import static org.envirocar.core.entity.Measurement.PropertyKey.GPS_ALTITUDE;
import static org.envirocar.core.entity.Measurement.PropertyKey.GPS_BEARING;
import static org.envirocar.core.entity.Measurement.PropertyKey.GPS_HDOP;
import static org.envirocar.core.entity.Measurement.PropertyKey.GPS_PDOP;
import static org.envirocar.core.entity.Measurement.PropertyKey.GPS_SPEED;
import static org.envirocar.core.entity.Measurement.PropertyKey.GPS_VDOP;
import static org.envirocar.core.entity.Measurement.PropertyKey.INTAKE_PRESSURE;
import static org.envirocar.core.entity.Measurement.PropertyKey.INTAKE_TEMPERATURE;
import static org.envirocar.core.entity.Measurement.PropertyKey.LAMBDA_CURRENT;
import static org.envirocar.core.entity.Measurement.PropertyKey.LAMBDA_CURRENT_ER;
import static org.envirocar.core.entity.Measurement.PropertyKey.LAMBDA_VOLTAGE;
import static org.envirocar.core.entity.Measurement.PropertyKey.LAMBDA_VOLTAGE_ER;
import static org.envirocar.core.entity.Measurement.PropertyKey.LONG_TERM_TRIM_1;
import static org.envirocar.core.entity.Measurement.PropertyKey.MAF;
import static org.envirocar.core.entity.Measurement.PropertyKey.RPM;
import static org.envirocar.core.entity.Measurement.PropertyKey.SHORT_TERM_TRIM_1;
import static org.envirocar.core.entity.Measurement.PropertyKey.SPEED;
import static org.envirocar.core.entity.Measurement.PropertyKey.THROTTLE_POSITON;

/**
 * @author dewall
 */
public class TrackSerde extends AbstractJsonSerde implements JsonSerializer<Track>, JsonDeserializer<Track> {
    private static final Logger LOG = Logger.getLogger(TrackSerde.class);


    public static final Set<Measurement.PropertyKey> supportedPhenomenons = new HashSet<>();

    static {
        supportedPhenomenons.add(CALCULATED_MAF);
        supportedPhenomenons.add(MAF);
        supportedPhenomenons.add(CO2);
        supportedPhenomenons.add(SPEED);
        supportedPhenomenons.add(RPM);
        supportedPhenomenons.add(INTAKE_PRESSURE);
        supportedPhenomenons.add(INTAKE_TEMPERATURE);
        supportedPhenomenons.add(CONSUMPTION);
        supportedPhenomenons.add(ENERGY_CONSUMPTION);
        supportedPhenomenons.add(ENERGY_CONSUMPTION_CO2);
        supportedPhenomenons.add(ENGINE_LOAD);
        supportedPhenomenons.add(THROTTLE_POSITON);
        supportedPhenomenons.add(GPS_ACCURACY);
        supportedPhenomenons.add(GPS_ALTITUDE);
        supportedPhenomenons.add(GPS_BEARING);
        supportedPhenomenons.add(GPS_HDOP);
        supportedPhenomenons.add(GPS_PDOP);
        supportedPhenomenons.add(GPS_VDOP);
        supportedPhenomenons.add(GPS_SPEED);
        supportedPhenomenons.add(SHORT_TERM_TRIM_1);
        supportedPhenomenons.add(LONG_TERM_TRIM_1);
        supportedPhenomenons.add(LAMBDA_CURRENT);
        supportedPhenomenons.add(LAMBDA_CURRENT_ER);
        supportedPhenomenons.add(LAMBDA_VOLTAGE);
        supportedPhenomenons.add(LAMBDA_VOLTAGE_ER);
        supportedPhenomenons.add(FUEL_SYSTEM_LOOP);
        supportedPhenomenons.add(FUEL_SYSTEM_STATUS_CODE);
    }

    @Override
    public JsonElement serialize(Track src, Type typeOfSrc, JsonSerializationContext context) {
        LOG.info("serialize() track");
        // set the properties of the json object
        JsonObject trackProperties = new JsonObject();
        trackProperties.addProperty(Track.KEY_TRACK_NAME, src.getName());
        trackProperties.addProperty(Track.KEY_TRACK_DESC,
                src.getDescription());
        trackProperties.addProperty(Track.KEY_TRACK_SENSOR, src.getCar().getId());
        trackProperties.addProperty(Track.KEY_TRACK_STATUS, src.getTrackStatus().name());

        try {
            if (src.getMetadata() != null) {
                JSONObject json = src.getMetadata().toJson();
                JSONArray names = json.names();
                for (int i = 0; i < names.length(); i++) {
                    trackProperties.addProperty(names.get(i).toString(),
                            json.getString(names.get(i).toString()));
                }
            } else {
                LOG.warn("The track does not provide metadata!");
            }
        } catch (JSONException e) {
            LOG.severe("Error while parsing metadata of track", e);
        }

        // serialize the array of features.
        JsonArray trackFeatures = new JsonArray();
        List<Measurement> measurements = src.getMeasurements();
        if (measurements == null || measurements.isEmpty()) {
            LOG.info("Track did not contain any non obfuscated measurements.");
        }

        try {
            for (Measurement measurement : measurements) {
                JsonElement measurementJson = createMeasurementProperties(
                        measurement, src.getCar());
                trackFeatures.add(measurementJson);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObject result = new JsonObject();
        result.addProperty(Track.KEY_TRACK_TYPE, "FeatureCollection");
        result.add(Track.KEY_TRACK_PROPERTIES, trackProperties);
        result.add(Track.KEY_TRACK_FEATURES, trackFeatures);

        LOG.info(result.toString());

        return result;
    }

    private JsonElement createMeasurementProperties(Measurement src, Car car) throws
            JSONException {
        // Create the Geometry json object
        JsonObject geometryJsonObject = new JsonObject();
        geometryJsonObject.addProperty(Track.KEY_TRACK_TYPE, "Point");

        // Create the coordinates of the geometry json object
        JsonArray coordinatesArray = new JsonArray();
        coordinatesArray.add(new JsonPrimitive(src.getLongitude()));
        coordinatesArray.add(new JsonPrimitive(src.getLatitude()));
        geometryJsonObject.add(Track.KEY_TRACK_FEATURES_GEOMETRY_COORDINATES,
                coordinatesArray);

        // Create measurement properties.
        JsonObject propertiesJson = new JsonObject();
        propertiesJson.addProperty(Track.KEY_TRACK_FEATURES_PROPERTIES_TIME,
                Util.longToIsoDate(src.getTime()));
        propertiesJson.addProperty("sensor", car.getId());

        // Add all measured phenomenons to this measurement.
        JsonObject phenomenons = createPhenomenons(src, car.getFuelType() == Car.FuelType.DIESEL);
        if (phenomenons != null) {
            propertiesJson.add(Track.KEY_TRACK_FEATURES_PROPERTIES_PHENOMENONS, phenomenons);
        }

        // Create the final json Measurement
        JsonObject result = new JsonObject();
        result.addProperty("type", "Feature");
        result.add(Track.KEY_TRACK_FEATURES_GEOMETRY, geometryJsonObject);
        result.add(Track.KEY_TRACK_FEATURES_PROPERTIES, propertiesJson);

        return result;
    }


    @Override
    public Track deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        // Get the properties json object.
        JsonObject p = json.getAsJsonObject().get(Track.KEY_TRACK_PROPERTIES).getAsJsonObject();

        // Parse the properties
        String id = p.get(Track.KEY_TRACK_ID).getAsString();
        String name = p.has(Track.KEY_TRACK_NAME) ? p.get(Track.KEY_TRACK_NAME).getAsString() : "unnamed Track #" + id;
        String description = p.has(Track.KEY_TRACK_DESC) ? p.get(Track.KEY_TRACK_DESC).getAsString() : "";

        // parse start and end time
        Long startTime = parseStringAsTime(Track.KEY_TRACK_BEGIN, p);
        Long endTime = parseStringAsTime(Track.KEY_TRACK_END, p);

        // parse the length
        Double length = parseAsDouble(Track.KEY_TRACK_LENGTH, p);

        // Parse the car object.
        JsonObject carObject = p.get(Track.KEY_TRACK_SENSOR).getAsJsonObject();
        Car car = context.deserialize(carObject, Car.class);

        // Parse the measurements
        JsonArray measurementsJsonArray = json.getAsJsonObject().get(Track.KEY_TRACK_FEATURES).getAsJsonArray();
        List<Measurement> measurements = new ArrayList<>();
        for (int i = 0; i < measurementsJsonArray.size(); i++) {
            JsonObject measurementObject = measurementsJsonArray.get(i).getAsJsonObject();
            measurements.add(context.deserialize(
                    measurementObject, Measurement.class));
        }

        // Create the track
        Track track = new TrackImpl(Track.DownloadState.DOWNLOADED);
        track.setTrackStatus(Track.TrackStatus.FINISHED);
        track.setRemoteID(id);
        track.setName(name);
        track.setDescription(description);
        track.setStartTime(startTime);
        track.setEndTime(endTime);

        track.setLength(length);
        track.setCar(car);
        track.setMeasurements(measurements); // Storing happens here...

        return track;
    }

    private JsonObject createPhenomenons(Measurement measurement, boolean isDiesel) throws
            JSONException {
        if (measurement.getAllProperties().isEmpty()) {
            return null;
        }

        JsonObject result = new JsonObject();
        Map<Measurement.PropertyKey, Double> props = measurement.getAllProperties();
        for (Measurement.PropertyKey key : props.keySet()) {
            if (supportedPhenomenons.contains(key)) {
                if (isDiesel && (key == Measurement.PropertyKey.CO2 || key == Measurement.PropertyKey.CONSUMPTION)) {
                    // DO NOTHING TODO delete when necessary
                } else {
                    result.add(key.toString(), createValue(props.get(key)));
                }
            }
        }
        return result;
    }

    public static JsonObject createValue(Double double1) {
        JsonObject result = new JsonObject();
        result.addProperty("value", double1);
        return result;
    }

    public static FileWithMetadata createTrackFile(Track track, String path) throws IOException {
        File result = new File(Util.resolveStorageBaseFolder(path), "enviroCar-track-" +
                track.getTrackID() + ".json");

        FileOutputStream out = new FileOutputStream(result);
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent("  ");

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Track.class, new TrackSerde())
                .create();
        gson.toJson(track, Track.class, writer);

        writer.flush();
        writer.close();

        return new FileWithMetadata(result, false);
    }

}
