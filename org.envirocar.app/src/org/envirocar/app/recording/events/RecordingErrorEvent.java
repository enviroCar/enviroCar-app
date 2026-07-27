/**
 * Copyright (C) 2013 - 2026 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */

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
