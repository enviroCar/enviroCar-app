package org.envirocar.app.injection.component;

import org.envirocar.app.injection.module.RecordingScreenModule;
import org.envirocar.app.injection.scopes.PerActivity;
import org.envirocar.app.views.recordingscreen.RecordingScreenActivity;
import org.envirocar.app.views.recordingscreen.TempomatFragment;
import org.envirocar.app.views.recordingscreen.TrackMapFragment;

import dagger.Subcomponent;

/**
 * @author dewall
 */
@PerActivity
@Subcomponent(modules = {RecordingScreenModule.class})
public interface RecordingScreenComponent {

    void inject(RecordingScreenActivity activity);

    void inject(TempomatFragment fragment);

    void inject(TrackMapFragment fragment);

}
