package org.envirocar.obd.commands.response.entity;

import org.envirocar.obd.commands.response.DataResponse;

/**
 * Created by matthes on 30.10.15.
 */
public class EngineLoadResponse extends DataResponse {

    private final float value;

    public EngineLoadResponse(float v) {
        this.value = v;
    }

    public float getValue() {
        return value;
    }
}
