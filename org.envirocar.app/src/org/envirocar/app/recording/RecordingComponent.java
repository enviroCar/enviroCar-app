package org.envirocar.app.recording;

import org.envirocar.app.recording.notification.RecordingNotification;
import org.envirocar.app.recording.strategy.OBDRecordingStrategy;

import dagger.Subcomponent;

/**
 * @author dewall
 */
@RecordingScope
@Subcomponent(modules = {RecordingModule.class})
public interface RecordingComponent {

    void inject(RecordingService recordingService);

    void inject(OBDRecordingStrategy obdRecordingStrategy);

    void inject(RecordingNotification recordingNotification);
}
