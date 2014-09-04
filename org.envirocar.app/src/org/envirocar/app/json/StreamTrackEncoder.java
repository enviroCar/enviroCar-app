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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import org.envirocar.app.application.TemporaryFileManager;
import org.envirocar.app.exception.InvalidObjectStateException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Track;
import org.envirocar.app.storage.Measurement.PropertyKey;
import org.envirocar.app.util.FileWithMetadata;
import org.envirocar.app.util.InputStreamWithLength;
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
	
	public FileWithMetadata createTrackJsonAsFile(Track track, boolean obfuscate, File result) throws FileNotFoundException, IOException, TrackWithoutMeasurementsException, JSONException {
		return createTrackJsonAsFile(track, obfuscate, result, false);
	}
	
	public FileWithMetadata createTrackJsonAsFile(Track track, boolean obfuscate, File result, boolean gzip) throws FileNotFoundException, IOException, TrackWithoutMeasurementsException, JSONException {
		FileOutputStream out = new FileOutputStream(result);
		Gson gson = new Gson();
		
		OutputStream targetOut;
		if (gzip) {
			targetOut = new GZIPOutputStream(out);
		}
		else {
			targetOut = out;
		}
		
		JsonWriter writer = new JsonWriter(
				new OutputStreamWriter(targetOut, "UTF-8"));
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
		
		return new FileWithMetadata(result, gzip);
	}
	
	public InputStreamWithLength createTrackJsonAsInputStream(Track track, boolean obfuscate) throws FileNotFoundException, IOException, TrackWithoutMeasurementsException, JSONException {
		return createTrackJsonAsInputStream(track, obfuscate, false);
	}
	
	public InputStreamWithLength createTrackJsonAsInputStream(Track track, boolean obfuscate, boolean gzip) throws FileNotFoundException, IOException, TrackWithoutMeasurementsException, JSONException {
		File result;
		try {
			result = TemporaryFileManager.instance().createTemporaryFile();
		} catch (InvalidObjectStateException e) {
			logger.warn(e.getMessage(), e);
			logger.warn("Creating persistent file on external storage instead!");
			result = Util.createFileOnExternalStorage(UUID.randomUUID().toString());
		}
		
		createTrackJsonAsFile(track, obfuscate, result, gzip);
		
		FileInputStream stream = new FileInputStream(result);
		
		return new InputStreamWithLength(stream, result.length(), gzip);
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
		
		logger.debug(String.format("Encoding %s features...", measurements.size()));
		
		String trackSensorName = track.getCar().getId();
		JsonObject measurementJson;
		int i = 0;
		for (Measurement measurement : measurements) {
			measurementJson = createMeasurementJson(track, trackSensorName, measurement);
			gson.toJson(measurementJson, writer);
			i++;
			if (i % 250 == 0) {
				logger.debug(String.format("Encoded %s/%s features...", i, measurements.size()));
			}
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
		result.addProperty("length", track.getLengthOfTrack());
		
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
