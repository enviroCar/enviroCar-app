package org.envirocar.app.test.it.db;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import junit.framework.Assert;

import org.envirocar.app.json.TrackDecoder;
import org.envirocar.app.storage.DbAdapterImpl;
import org.envirocar.app.storage.Track;
import org.envirocar.app.storage.Track.TrackStatus;
import org.envirocar.app.test.ResourceLoadingTestCase;
import org.json.JSONException;

public class TrackJsonParsingTest extends ResourceLoadingTestCase {

	public void testParsingAndResolving() throws NumberFormatException, JSONException, ParseException, IOException, InstantiationException {
		if (DbAdapterImpl.instance() == null) {
			DbAdapterImpl.init(getInstrumentation().getTargetContext());
		}

		Track t = new TrackDecoder().fromJson(createJsonViaStream());
		
		Track dbTrack = DbAdapterImpl.instance().getTrack(t.getId());
		
		Assert.assertTrue("Car was null!", dbTrack.getCar() != null);
		Assert.assertTrue("Track contained no measurements!", dbTrack.getMeasurements() != null &&
				dbTrack.getMeasurements().size() > 0);
		Assert.assertTrue("Track contained wrong number of measurements!", dbTrack.getMeasurements().size() == 3);
		Assert.assertTrue("Track not set as FINISHED!", dbTrack.getStatus() == TrackStatus.FINISHED);
		
		DbAdapterImpl.instance().deleteTrack(dbTrack.getId());

		Assert.assertTrue("Expected an empty list!", DbAdapterImpl.instance().getAllMeasurementsForTrack(dbTrack).isEmpty());
	}

	private InputStream createJsonViaStream() throws IOException {
		return getInstrumentation().getContext().getAssets().open("track_mockup.json");
	}
	
}
