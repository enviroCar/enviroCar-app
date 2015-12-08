package org.envirocar.obd.commands.response.entity;

import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.response.DataResponse;

/**
 * Created by matthes on 30.10.15.
 */
public class FuelPressureResponse extends DataResponse {
    private final int value;

    public FuelPressureResponse(int v) {
        this.value = v;
    }

    public Number getValue() {
        return value;
    }

    @Override
    public PID getPid() {
        return PID.FUEL_PRESSURE;
    }
}
