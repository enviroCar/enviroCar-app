package org.envirocar.app.services.autoconnect;

import androidx.lifecycle.LifecycleObserver;

/**
 * @author dewall
 */
public interface AutoRecordingStrategy extends LifecycleObserver {

    interface Factory {
        AutoRecordingStrategy create();
    }

    enum PreconditionType {
        SATISFIED,
        BT_DISABLED,
        GPS_DISABLED,
        OBD_NOT_SELECTED,
        CAR_NOT_SELECTED
    }

    interface AutoRecordingCallback {

        /**
         *
         * @param preconditionType
         */
        void onPreconditionUpdate(PreconditionType preconditionType);

        /**
         * Called when the specific requirements of the recordingtype has been met for starting a track.
         */
        void onRecordingTypeConditionsMet();
    }

    boolean preconditionsFulfilled();

    void run(AutoRecordingCallback callback);

    void stop();

}
