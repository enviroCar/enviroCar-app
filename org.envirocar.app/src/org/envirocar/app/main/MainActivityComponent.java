/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.main;

import org.envirocar.app.views.LogbookFragment;
import org.envirocar.app.views.dashboard.DashBoardFragment;
import org.envirocar.app.views.recordingscreen.TempomatFragment;
import org.envirocar.app.views.recordingscreen.TrackMapFragment;
import org.envirocar.app.views.recordingscreen.GPSOnlyTrackRecordingScreen;
import org.envirocar.app.views.recordingscreen.OBDPlusGPSTrackRecordingScreen;
import org.envirocar.app.views.statistics.GraphFragment;
import org.envirocar.app.views.statistics.StatisticsFragment;
import org.envirocar.app.views.tracklist.TrackListLocalCardFragment;
import org.envirocar.app.views.tracklist.TrackListPagerFragment;
import org.envirocar.app.views.tracklist.TrackListRemoteCardFragment;

import dagger.Subcomponent;

/**
 * @author Sai Krishna
 */
@Subcomponent(
        modules = MainActivityModule.class
)
public interface MainActivityComponent {

    void inject(BaseMainActivityBottomBar baseMainActivity);
    void inject(StatisticsFragment statisticsFragment);
    void inject(GraphFragment graphFragment);
    void inject(TempomatFragment tempomatFragment);
    void inject(TrackMapFragment trackMapFragment);
    void inject(TrackListLocalCardFragment trackListLocalCardFragment);
    void inject(TrackListPagerFragment trackListPagerFragment);
    void inject(TrackListRemoteCardFragment trackListRemoteCardFragment);
    void inject(LogbookFragment logbookFragment);
    void inject(DashBoardFragment dashBoardFragment);
    void inject(OBDPlusGPSTrackRecordingScreen obdPlusGPSTrackRecordingScreen);
    void inject(GPSOnlyTrackRecordingScreen gpsOnlyTrackRecordingScreen);


}
