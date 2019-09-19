package org.envirocar.app.recording.events;

import androidx.annotation.NonNull;

import com.google.common.base.MoreObjects;

import org.envirocar.app.recording.RecordingType;

/**
 * @author dewall
 */
public class RecordingTypeSelectedEvent {

    public final RecordingType recordingType;

    /**
     * Constructor.
     *
     * @param recordingType
     */
    public RecordingTypeSelectedEvent(RecordingType recordingType) {
        this.recordingType = recordingType;
    }

    @NonNull
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("RecordingType", recordingType.toString())
                .toString();
    }
}
