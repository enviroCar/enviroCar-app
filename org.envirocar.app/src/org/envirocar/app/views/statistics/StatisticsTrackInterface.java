package org.envirocar.app.views.statistics;

import org.envirocar.core.entity.Track;

import java.util.List;

public interface StatisticsTrackInterface {

    public void sendTracks(List<Track> trackList);
}
