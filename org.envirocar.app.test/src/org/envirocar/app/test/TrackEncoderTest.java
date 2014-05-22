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

package org.envirocar.app.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import junit.framework.Assert;

import org.envirocar.app.json.StreamTrackEncoder;
import org.envirocar.app.json.TrackEncoder;
import org.envirocar.app.json.TrackWithoutMeasurementsException;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.Car.FuelType;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Track;
import org.envirocar.app.storage.Measurement.PropertyKey;
import org.envirocar.app.storage.TrackAlreadyFinishedException;
import org.envirocar.app.storage.TrackMetadata;
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

import android.test.AndroidTestCase;

public class TrackEncoderTest extends AndroidTestCase {
	
	private Car car = new Car(FuelType.GASOLINE, "manuf", "modl", "iddddd", 1234, 2345);
	private String expectedJson = "{\"features\":[{\"type\":\"Feature\",\"properties\":{\"phenomenons\":{\"MAF\":{\"value\":12.4},\"Speed\":{\"value\":12}},\"sensor\":\"iddddd\",\"time\":\"2013-09-25T10:30:00Z\"},\"geometry\":{\"type\":\"Point\",\"coordinates\":[-89.1,-87.1]}}],\"type\":\"FeatureCollection\",\"properties\":{\"sensor\":\"iddddd\",\"description\":\"desc\",\"name\":\"test-track\"}}";

	public void testTrackJsonCreation() throws JSONException, TrackAlreadyFinishedException {
		Track t = createTrack();
		String json;
		try {
			json = new TrackEncoder().createTrackJson(t, false).toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		JSONObject result = new JSONObject(json);
		JSONObject expected = new JSONObject(expectedJson);

		Assert.assertTrue("The JSON was null!", json != null);
		Assert.assertTrue("The JSON was not as expected!", result.toString().equals(expected.toString()));
		
	}
	

	public void testObfuscationNoMeasurements() throws JSONException, TrackAlreadyFinishedException {
		
		Track t = createTrack(); 
		try {
			new TrackEncoder().createTrackJson(t, true);
		} catch (TrackWithoutMeasurementsException e) {
			Assert.assertNotNull("Expected an exception!", e);
		}
	}
	
	public void testMetadataEncoding() throws TrackAlreadyFinishedException, JSONException, TrackWithoutMeasurementsException {
		Track t = createTrack();
		TrackMetadata m1 = new TrackMetadata();
		m1.putEntry(TrackMetadata.APP_VERSION, "v1");
		m1.putEntry(TrackMetadata.OBD_DEVICE, "OBDIII");
		t.setMetadata(m1);
		
		TrackMetadata m2 = new TrackMetadata();
		m2.putEntry(TrackMetadata.TOU_VERSION, "2020-10-01");
		
		t.updateMetadata(m2);
		
		String result = new TrackEncoder().createTrackJson(t, false).toString();
		
		Assert.assertTrue(result.contains(TrackMetadata.APP_VERSION));
		Assert.assertTrue(result.contains("v1"));
		Assert.assertTrue(result.contains(TrackMetadata.OBD_DEVICE));
		Assert.assertTrue(result.contains("OBDIII"));
		Assert.assertTrue(result.contains(TrackMetadata.TOU_VERSION));
		Assert.assertTrue(result.contains("2020-10-01"));
	}

	private Track createTrack() throws TrackAlreadyFinishedException {
		Track result = Track.createNewLocalTrack(new DbAdapterMockup());
		result.setCar(car);
		result.setMeasurementsAsArrayList(Collections.singletonList(createMeasurement()));
		result.setDescription("desc");
		result.setName("test-track");
		return result;
	}

	private Measurement createMeasurement() {
		Measurement m = new Measurement(-87.1, -89.1);
		m.setProperty(PropertyKey.MAF, 12.4);
		m.setProperty(PropertyKey.SPEED, 12.0);
		m.setTime(1380105000000L);
		return m;
	}
	
	public void testStreamEncoding() throws FileNotFoundException, IOException, TrackWithoutMeasurementsException, JSONException, TrackAlreadyFinishedException {
		InputStreamWithLength in = new StreamTrackEncoder().createTrackJsonAsInputStream(createTrack(), false);
		ByteArrayOutputStream content = Util.readStreamContents(in.getInputStream());
		
		String json = new String(content.toByteArray());

		JSONObject result = new JSONObject(json);
		JSONObject expected = new JSONObject(expectedJson);

		Assert.assertTrue("The JSON was null!", json != null);
		Assert.assertTrue("The JSON was not as expected!", result.toString().equals(expected.toString()));
		
	}

	public InputStream createTrackJsonAsInputStream(Track track, boolean obfuscate) throws FileNotFoundException, IOException, TrackWithoutMeasurementsException, JSONException {
		File result = new File(Util.resolveExternalStorageBaseFolder(), UUID.randomUUID().toString());
		FileOutputStream out = new FileOutputStream(result);
		
		Gson gson = new Gson();
		
		JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent("  ");
        
        /*
         * the featurecollection
         */
        writer.beginObject();
        
        String trackSensorName = encodeFeatureArray(track, gson, writer);
		
		writer.name("type");
		writer.value("FeatureCollection");
		
		writer.name("properties");
		gson.toJson(createTrackProperties(track, trackSensorName), writer);
		
		/*
		 * end: the featurecollection
		 */
		writer.endObject();
		writer.flush();
		writer.close();
		return new FileInputStream(result);
	}


	private String encodeFeatureArray(Track track, Gson gson, JsonWriter writer)
			throws IOException, TrackWithoutMeasurementsException,
			JSONException {
		/*
         * the features array
         */
        writer.name("features");
        writer.beginArray();
        
		List<Measurement> measurements = track.getMeasurements();

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
		return trackSensorName;
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
//			if (supportedPhenomenons.contains(key)) {
				result.add(key.toString(), createValue(props.get(key)));
//			}
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
