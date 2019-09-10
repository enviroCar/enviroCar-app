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

    static RecordingStrategy createStrategy(String type, BaseApplicationComponent injector){
        RecordingStrategy strategy = null;
        if(type == "obd"){
//            strategy = new OBDRecordingStrategy();
//        }
//        if( type == "gps"){
//            strategy = new GPSRecordingStrategy();
        }
//        injector.inject(strategy);
        return strategy;
    }

    void startRecording(Service service, RecordingListener listener);

    void stopRecording();
}
