package org.envirocar.app.test.it.db;

import java.io.IOException;
import java.text.ParseException;

import junit.framework.Assert;

import org.envirocar.app.storage.DbAdapterImpl;
import org.envirocar.app.storage.Track;
import org.envirocar.app.storage.Track.TrackStatus;
import org.envirocar.app.storage.TrackWithoutMeasurementsException;
import org.envirocar.app.test.ResourceLoadingTestCase;
import org.json.JSONException;
import org.json.JSONObject;

public class TrackJsonParsingTest extends ResourceLoadingTestCase {

	public void testParsingAndResolving() throws NumberFormatException, JSONException, ParseException, TrackWithoutMeasurementsException, IOException, InstantiationException {
		if (DbAdapterImpl.instance() == null) {
			DbAdapterImpl.init(getInstrumentation().getTargetContext());
		}

		Track t = Track.fromJson(createJson(), DbAdapterImpl.instance());
		
		Track dbTrack = DbAdapterImpl.instance().getTrack(t.getId());
		
		Assert.assertTrue("Car was null!", dbTrack.getCar() != null);
		Assert.assertTrue("Track contained no measurements!", dbTrack.getMeasurements() != null &&
				dbTrack.getMeasurements().size() > 0);
		Assert.assertTrue("Track contained wrong number of measurements!", dbTrack.getMeasurements().size() == 3);
		Assert.assertTrue("Track not set as FINISHED!", dbTrack.getStatus() == TrackStatus.FINISHED);
		
		DbAdapterImpl.instance().deleteTrack(dbTrack.getId());
		try {
			DbAdapterImpl.instance().getAllMeasurementsForTrack(dbTrack);
		} catch (TrackWithoutMeasurementsException e) {
			Assert.assertNotNull("Expected an exception as the track should not have any measurements left in the DB!", e);
		}
	}

	private JSONObject createJson() throws JSONException, IOException {
		return new JSONObject(readJsonAsset("track_mockup.json"));
	}
	
}
