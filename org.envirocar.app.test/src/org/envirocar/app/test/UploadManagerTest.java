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

import junit.framework.Assert;

import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.application.UploadManager;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.Car.FuelType;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Track;
import org.envirocar.app.storage.Measurement.PropertyKey;
import org.envirocar.app.storage.TrackWithoutMeasurementsException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;

public class UploadManagerTest extends AndroidTestCase {
	
	private Car car = new Car(FuelType.GASOLINE, "manuf", "modl", "iddddd", 1234, 2345);
	private String expectedJson = "{\"features\":[{\"type\":\"Feature\",\"properties\":{\"phenomenons\":{\"MAF\":{\"value\":12.4},\"Speed\":{\"value\":12}},\"sensor\":\"iddddd\",\"time\":\"2013-09-25T10:30:00Z\"},\"geometry\":{\"type\":\"Point\",\"coordinates\":[-89.1,-87.1]}}],\"type\":\"FeatureCollection\",\"properties\":{\"sensor\":\"iddddd\",\"description\":\"desc\",\"name\":\"test-track\"}}";

	public void testTrackJsonCreation() throws JSONException, TrackWithoutMeasurementsException {
		UploadManager um = new UploadManager(getContext());
		
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
		boolean oldPref = pref.getBoolean(SettingsActivity.OBFUSCATE_POSITION, false);
		
		pref.edit().putBoolean(SettingsActivity.OBFUSCATE_POSITION, false).commit();
		
		Track t = createTrack(); 
		String json = um.getTrackJSON(t);
		
		JSONObject result = new JSONObject(json);
		JSONObject expected = new JSONObject(expectedJson);
		
		Assert.assertTrue("The JSON was null!", json != null);
		Assert.assertTrue("The JSON was not as expected!", result.toString().equals(expected.toString()));
		
		pref.edit().putBoolean(SettingsActivity.OBFUSCATE_POSITION, oldPref).commit();
	}

	private Track createTrack() {
		Track result = new Track("test-id", car, new DbAdapterMockup());
		result.addMeasurement(createMeasurement());
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

}
