package org.envirocar.app.main;

import org.envirocar.app.views.LogbookFragment;
import org.envirocar.app.views.dashboard.DashBoardFragment;
import org.envirocar.app.views.recordingscreen.TempomatFragment;
import org.envirocar.app.views.recordingscreen.TrackMapFragment;
import org.envirocar.app.views.recordingscreen.GPSOnlyTrackRecordingScreen;
import org.envirocar.app.views.recordingscreen.OBDPlusGPSTrackRecordingScreen;
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
