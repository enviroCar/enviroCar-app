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
package org.envirocar.app.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.envirocar.app.application.TemporaryFileManager;
import org.envirocar.app.exception.InvalidObjectStateException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Track;
import org.envirocar.app.storage.Measurement.PropertyKey;
import org.envirocar.app.util.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;

public class StreamTrackEncoder extends TrackEncoder {

	private static final Logger logger = Logger.getLogger(StreamTrackEncoder.class);
	
	public InputStream createTrackJsonAsInputStream(Track track, boolean obfuscate) throws FileNotFoundException, IOException, TrackWithoutMeasurementsException, JSONException {
		File result;
		try {
			result = TemporaryFileManager.instance().createTemporaryFile();
		} catch (InvalidObjectStateException e) {
			logger.warn(e.getMessage(), e);
			logger.warn("Creating persistent file on external storage instead!");
			result = Util.createFileOnExternalStorage(UUID.randomUUID().toString());
		}
		FileOutputStream out = new FileOutputStream(result);
		Gson gson = new Gson();
		
		JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent("  ");
        
        /*
         * the featurecollection
         */
        writer.beginObject();
        
        encodeFeatureArray(track, gson, writer, obfuscate);
		
		writer.name("type");
		writer.value("FeatureCollection");
		
		writer.name("properties");
		gson.toJson(createTrackProperties(track, track.getCar().getId()), writer);
		
		/*
		 * end: the featurecollection
		 */
		writer.endObject();
		writer.flush();
		writer.close();
		FileInputStream stream = new FileInputStream(result);
		
		stream.available();
		
		return stream;
	}


	private void encodeFeatureArray(Track track, Gson gson, JsonWriter writer, boolean obfuscate)
			throws IOException, TrackWithoutMeasurementsException,
			JSONException {
		/*
         * the features array
         */
        writer.name("features");
        writer.beginArray();
        
		List<Measurement> measurements = getNonObfuscatedMeasurements(track, obfuscate);

		if (measurements == null || measurements.isEmpty()) {
			writer.close();
			throw new TrackWithoutMeasurementsException("Track did not contain any non obfuscated measurements.");
		}
			
		String trackSensorName = track.getCar().getId();
		JsonObject measurementJson;
		for (Measurement measurement : measurements) {
			measurementJson = createMeasurementJson(track, trackSensorName, measurement);
			gson.toJson(measurementJson, writer);
		}
		
		/*
		 * end: the features array
		 */
		writer.endArray();
	}
	
	private JsonObject createMeasurementJson(Track track, String trackSensorName, Measurement measurement) throws JSONException {
		JsonObject result = new JsonObject();
		result.addProperty("type", "Feature");
		
		result.add("properties", createMeasurementProperties(measurement, trackSensorName));
		result.add("geometry", createGeometry(measurement));
		
		return result;
	}
	
	private JsonObject createGeometry(Measurement measurement) throws JSONException {
		JsonObject result = new JsonObject();
		result.addProperty("type", "Point");
		
		JsonArray array = new JsonArray();
		array.add(new JsonPrimitive(measurement.getLongitude()));
		array.add(new JsonPrimitive(measurement.getLatitude()));
		
		result.add("coordinates", array);
		return result;
	}
	
	private JsonObject createMeasurementProperties(Measurement measurement, String trackSensorName) throws JSONException {
		JsonObject result = new JsonObject();
		
		JsonObject phens = createPhenomenons(measurement);
		if (phens != null) {
			result.add("phenomenons", phens);
		}
		
		result.addProperty("sensor", trackSensorName);
		result.addProperty("time", Util.longToIsoDate(measurement.getTime()));
		
		return result;
	}

	private JsonObject createPhenomenons(Measurement measurement) throws JSONException {
		if (measurement.getAllProperties().isEmpty()) {
			return null;
		}
		
		JsonObject result = new JsonObject();
		Map<PropertyKey, Double> props = measurement.getAllProperties();
		for (PropertyKey key : props.keySet()) {
			if (supportedPhenomenons.contains(key)) {
				result.add(key.toString(), createValue(props.get(key)));
			}
		}
		return result;
	}
	
	private JsonObject createValue(Double double1) throws JSONException {
		JsonObject result = new JsonObject();
		result.addProperty("value", double1);
		return result;
	}
	
	private JsonObject createTrackProperties(Track track, String trackSensorName) throws JSONException {
		JsonObject result = new JsonObject();
		
		result.addProperty("sensor", trackSensorName);
		result.addProperty("description", track.getDescription());
		result.addProperty("name", track.getName());
		
		if (track.getMetadata() != null) {
			JSONObject json = track.getMetadata().toJson();
			JSONArray names = json.names();
			for (int i = 0; i < names.length(); i++) {
				result.addProperty(names.get(i).toString(), json.getString(names.get(i).toString()));
			}
		}
		
		return result;
	}
}
