package org.envirocar.app.recording.events;

import androidx.annotation.NonNull;

import com.google.common.base.MoreObjects;

import org.envirocar.app.recording.RecordingState;

/**
 * @author dewall
 */
public class RecordingStateEvent {

    public final RecordingState recordingState;

    /**
     * Constructor.
     *
     * @param recordingState
     */
    public RecordingStateEvent(RecordingState recordingState) {
        this.recordingState = recordingState;
    }

    @NonNull
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("RecordingState", recordingState.toString())
                .toString();
    }
}
