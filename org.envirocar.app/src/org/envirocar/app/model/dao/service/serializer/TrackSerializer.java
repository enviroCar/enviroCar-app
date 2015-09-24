package org.envirocar.app.model.dao.service.serializer;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.envirocar.app.json.StreamTrackEncoder;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.dao.service.TrackService;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.RemoteTrack;
import org.envirocar.app.storage.Track;
import org.envirocar.app.util.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.List;

/**
 * @author dewall
 */
public class TrackSerializer implements JsonSerializer<Track>, JsonDeserializer<Track> {
    private static final Logger LOG = Logger.getLogger(TrackSerializer.class);

    @Override
    public JsonElement serialize(Track src, Type typeOfSrc, JsonSerializationContext context) {
        LOG.info("serialize() track");
        // set the properties of the json object
        JsonObject trackProperties = new JsonObject();
        trackProperties.addProperty(TrackService.KEY_TRACK_PROPERTIES_NAME, src.getName());
        trackProperties.addProperty(TrackService.KEY_TRACK_PROPERTIES_DESCRIPTION,
                src.getDescription());
        trackProperties.addProperty(TrackService.KEY_TRACK_PROPERTIES_SENSOR, src.getCar().getId());

        try {
            if (src.getMetadata() != null) {
                JSONObject json = src.getMetadata().toJson();
                JSONArray names = json.names();
                for (int i = 0; i < names.length(); i++) {
                    trackProperties.addProperty(names.get(i).toString(),
                            json.getString(names.get(i).toString()));
                }
            }
        } catch (JSONException e) {
            LOG.severe("Error while parsing metadata of track", e);
        }

        // serialize the array of features.
        JsonArray trackFeatures = new JsonArray();
        List<Measurement> measurements = getNonObfuscatedMeasurements(src, true);
        if (measurements == null || measurements.isEmpty()) {
            LOG.severe("Track did not contain any non obfuscated measurements.");
            return null;
        }

        JsonElement measurementJson;
        try {
            for (Measurement measurement : measurements) {
                measurementJson = createMeasurementProperties(measurement, src.getCar().getId());
                trackFeatures.add(measurementJson);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObject result = new JsonObject();
        result.addProperty(TrackService.KEY_TRACK_TYPE, "FeatureCollection");
        result.add(TrackService.KEY_TRACK_PROPERTIES, trackProperties);
        result.add(TrackService.KEY_TRACK_FEATURES, trackFeatures);

        return result;
    }

    private List<Measurement> getNonObfuscatedMeasurements(Track track, boolean obfuscate) {
        // TODO
        return track.getMeasurements();
    }

    private JsonElement createMeasurementProperties(Measurement src, String sensorId) throws
            JSONException {
        // Create the Geometry json object
        JsonObject geometryJsonObject = new JsonObject();
        geometryJsonObject.addProperty(TrackService.KEY_TRACK_TYPE, "Point");

        // Create the coordinates of the geometry json object
        JsonArray coordinatesArray = new JsonArray();
        coordinatesArray.add(new JsonPrimitive(src.getLongitude()));
        coordinatesArray.add(new JsonPrimitive(src.getLatitude()));
        geometryJsonObject.add(TrackService.KEY_TRACK_FEATURES_GEOMETRY_COORDINATES,
                coordinatesArray);

        // Create measurement properties.
        JsonObject propertiesJson = new JsonObject();
        propertiesJson.addProperty(TrackService.KEY_TRACK_FEATURES_PROPERTIES_TIME,
                Util.longToIsoDate(src.getTime()));
        propertiesJson.addProperty("sensor", sensorId);

        // Add all measured phenomenons to this measurement.
        JsonObject phenomenons = StreamTrackEncoder.createPhenomenons(src);
        if (phenomenons != null) {
            propertiesJson.add(TrackService.KEY_TRACK_FEATURES_PROPERTIES_PHENOMENONS, phenomenons);
        }

        // Create the final json Measurement
        JsonObject result = new JsonObject();
        result.addProperty("type", "Feature");
        result.add(TrackService.KEY_TRACK_FEATURES_GEOMETRY, geometryJsonObject);
        result.add(TrackService.KEY_TRACK_FEATURES_PROPERTIES, propertiesJson);

        return result;
    }


    @Override
    public Track deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        LOG.info("deserialize()");

        // Get the properties json object.
        JsonObject properties = json.getAsJsonObject()
                .get(TrackService.KEY_TRACK_PROPERTIES).getAsJsonObject();

        // Parse the properties
        String id = properties.get(TrackService.KEY_TRACK_PROPERTIES_ID).getAsString();
        String name = properties.has(TrackService.KEY_TRACK_PROPERTIES_NAME) ?
                properties.get(TrackService.KEY_TRACK_PROPERTIES_NAME).getAsString() :
                ("unnamed Track #" + id);
        String description = properties.has(TrackService.KEY_TRACK_PROPERTIES_DESCRIPTION) ?
                properties.get(TrackService.KEY_TRACK_PROPERTIES_DESCRIPTION).getAsString() :
                "";

        // Parse the car object.
        JsonObject carObject = properties.get(TrackService.KEY_TRACK_PROPERTIES_SENSOR)
                .getAsJsonObject();
        Car car = context.<Car>deserialize(carObject, Car.class);

        LOG.info("deserialize() measurements");

        // Parse the measurements
        JsonArray measurementsJsonArray = json.getAsJsonObject().get(TrackService
                .KEY_TRACK_FEATURES).getAsJsonArray();
        List<Measurement> measurements = Lists.newArrayList();
        for (int i = 0; i < measurementsJsonArray.size(); i++) {
            JsonObject measurementObject = measurementsJsonArray.get(i).getAsJsonObject();
            measurements.add(context.<Measurement>deserialize(
                    measurementObject, Measurement.class));
        }

        LOG.info("deserialze(): storing measurements in database");

        // Create the track
        RemoteTrack track = new RemoteTrack(id);
        track.setName(name);
        track.setDescription(description);
        track.setCar(car);
        track.setMeasurementsAsArrayList(measurements); // Storing happens here...

        return track;
    }


}
