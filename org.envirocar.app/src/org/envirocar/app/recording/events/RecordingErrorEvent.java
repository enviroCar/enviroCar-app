package org.envirocar.app.recording.events;

import org.envirocar.app.recording.RecordingError;

import java.util.HashMap;
import java.util.Map;

public class RecordingErrorEvent {

    private RecordingError errorType;

    private String errorMessage;

    private Map<String, String> extraData;

    public RecordingErrorEvent(RecordingError errorType, String errorMessage) {
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.extraData = new HashMap<>();
    }

    public RecordingError getErrorType() {
        return errorType;
    }

    public void setErrorType(RecordingError errorType) {
        this.errorType = errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Map<String, String> getExtraData() {
        return extraData;
    }

    public void addExtraData(String key, String value) {
        this.extraData.put(key, value);
    }
}
