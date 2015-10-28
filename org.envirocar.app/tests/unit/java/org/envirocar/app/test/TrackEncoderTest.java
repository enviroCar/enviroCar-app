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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;

import junit.framework.Assert;

import org.envirocar.app.json.StreamTrackEncoder;
import org.envirocar.app.json.TrackEncoder;
import org.envirocar.app.json.TrackWithoutMeasurementsException;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.Car.FuelType;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Track;
import org.envirocar.app.storage.Measurement.PropertyKey;
import org.envirocar.core.exception.TrackAlreadyFinishedException;
import org.envirocar.app.storage.TrackMetadata;
import org.envirocar.app.util.InputStreamWithLength;
import org.envirocar.app.util.Util;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class TrackEncoderTest {
	
	private Car car = new Car(FuelType.GASOLINE, "manuf", "modl", "iddddd", 1234, 2345);
	private String expectedJson = "{\"features\":[{\"type\":\"Feature\",\"properties\":{\"phenomenons\":{\"MAF\":{\"value\":12.4},\"Speed\":{\"value\":12}},\"sensor\":\"iddddd\",\"time\":\"2013-09-25T10:30:00Z\"},\"geometry\":{\"type\":\"Point\",\"coordinates\":[-89.1,-87.1]}}],\"type\":\"FeatureCollection\",\"properties\":{\"sensor\":\"iddddd\",\"description\":\"desc\",\"name\":\"test-track\"}}";

	@Test
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

    @Test
	public void testObfuscationNoMeasurements() throws JSONException, TrackAlreadyFinishedException {
		
		Track t = createTrack(); 
		try {
			new TrackEncoder().createTrackJson(t, true);
		} catch (TrackWithoutMeasurementsException e) {
			Assert.assertNotNull("Expected an exception!", e);
		}
	}

    @Test
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
		Track result = Track.createLocalTrack();
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

    @Test
	public void testStreamEncoding() throws FileNotFoundException, IOException, TrackWithoutMeasurementsException, JSONException, TrackAlreadyFinishedException {
		InputStreamWithLength in = new StreamTrackEncoder().createTrackJsonAsInputStream(createTrack(), false);
		ByteArrayOutputStream content = Util.readStreamContents(in.getInputStream());
		
		String json = new String(content.toByteArray());

		JSONObject result = new JSONObject(json);
		JSONObject expected = new JSONObject(expectedJson);

		Assert.assertTrue("The JSON was null!", json != null);
		Assert.assertTrue("The JSON was not as expected!", result.toString().equals(expected.toString()));
		
	}
}
