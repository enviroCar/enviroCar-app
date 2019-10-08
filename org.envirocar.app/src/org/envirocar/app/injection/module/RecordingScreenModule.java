package org.envirocar.app.injection.module;

import org.envirocar.app.injection.scopes.PerActivity;
import org.envirocar.app.views.recordingscreen.TempomatFragment;
import org.envirocar.app.views.recordingscreen.TrackMapFragment;

import dagger.Module;
import dagger.Provides;

/**
 * @author dewall
 */
@Module
public class RecordingScreenModule {

    @Provides
    @PerActivity
    public TrackMapFragment provideTrackMapFragment(){
        return new TrackMapFragment();
    }

    @Provides
    @PerActivity
    public TempomatFragment provideTempomatFragment(){
        return new TempomatFragment();
    }
}
