package org.envirocar.obd.commands.response.entity;

import org.envirocar.obd.commands.response.DataResponse;

/**
 * Created by matthes on 30.10.15.
 */
public class SpeedResponse extends DataResponse {
    private final int value;

    public SpeedResponse(int v) {
        this.value = v;
    }

    public int getValue() {
        return value;
    }
}
