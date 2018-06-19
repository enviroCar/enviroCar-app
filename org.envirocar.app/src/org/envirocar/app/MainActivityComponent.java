package org.envirocar.app;

import org.envirocar.app.view.LogbookFragment;
import org.envirocar.app.view.dashboard.DashBoardFragment;
import org.envirocar.app.view.dashboard.DashboardMainFragment;
import org.envirocar.app.view.dashboard.DashboardMapFragment;
import org.envirocar.app.view.dashboard.DashboardTempomatFragment;
import org.envirocar.app.view.dashboard.DashboardTrackDetailsFragment;
import org.envirocar.app.view.dashboard.DashboardTrackMapFragment;
import org.envirocar.app.view.dashboard.DashboardTrackSettingsFragment;
import org.envirocar.app.view.recordingscreen.GPSOnlyTrackRecordingScreen;
import org.envirocar.app.view.recordingscreen.OBDPlusGPSTrackRecordingScreen;
import org.envirocar.app.view.tracklist.TrackListLocalCardFragment;
import org.envirocar.app.view.tracklist.TrackListPagerFragment;
import org.envirocar.app.view.tracklist.TrackListRemoteCardFragment;

import dagger.Subcomponent;

/**
 * @author Sai Krishna
 */
@Subcomponent(
        modules = MainActivityModule.class
)
public interface MainActivityComponent {

    void inject(BaseMainActivity baseMainActivity);
    void inject(BaseMainActivityBottomBar baseMainActivity);
    void inject(DashboardMainFragment dashboardMainFragment);
    void inject(DashboardMapFragment dashboardMapFragment);
    void inject(DashboardTempomatFragment dashboardTempomatFragment);
    void inject(DashboardTrackDetailsFragment dashboardTrackDetailsFragment);
    void inject(DashboardTrackMapFragment dashboardTrackMapFragment);
    void inject(DashboardTrackSettingsFragment dashboardTrackSettingsFragment);
    void inject(TrackListLocalCardFragment trackListLocalCardFragment);
    void inject(TrackListPagerFragment trackListPagerFragment);
    void inject(TrackListRemoteCardFragment trackListRemoteCardFragment);
    void inject(LogbookFragment logbookFragment);
    void inject(DashBoardFragment dashBoardFragment);
    void inject(OBDPlusGPSTrackRecordingScreen obdPlusGPSTrackRecordingScreen);
    void inject(GPSOnlyTrackRecordingScreen gpsOnlyTrackRecordingScreen);


}
