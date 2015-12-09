package org.envirocar.obd.commands.response.entity;

import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.response.DataResponse;

public class EngineLoadResponse extends DataResponse {

    private final float value;

    public EngineLoadResponse(float v) {
        this.value = v;
    }

    public Number getValue() {
        return value;
    }

    @Override
    public PID getPid() {
        return PID.CALCULATED_ENGINE_LOAD;
    }
}
