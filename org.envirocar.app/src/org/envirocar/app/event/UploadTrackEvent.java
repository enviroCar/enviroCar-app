package org.envirocar.app.event;

import org.envirocar.app.storage.Track;

public class UploadTrackEvent implements AbstractEvent<Track> {
	
	private Track track;
	
	public UploadTrackEvent(Track track) {
		this.track = track;
	}

	@Override
	public Track getPayload() {
		return track;
	}

}
