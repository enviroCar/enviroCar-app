package org.envirocar.app.recording.strategy;

import android.app.Service;

import androidx.lifecycle.LifecycleObserver;

import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.recording.RecordingState;


/**
 * @author dewall
 */
public interface RecordingStrategy extends LifecycleObserver {

    interface RecordingListener {
        void onRecordingStateChanged(RecordingState recordingState);
    }

    interface Factory {
        RecordingStrategy create();
    }

    void startRecording(Service service, RecordingListener listener);

    void stopRecording();
}
