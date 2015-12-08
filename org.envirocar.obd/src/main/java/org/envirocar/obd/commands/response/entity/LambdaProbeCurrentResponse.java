package org.envirocar.obd.commands.response.entity;

import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.response.DataResponse;

/**
 * Created by matthes on 30.10.15.
 */
public class LambdaProbeCurrentResponse extends DataResponse {
    private final double current;
    private final double equivalenceRatio;

    public LambdaProbeCurrentResponse(double current, double er) {
        this.current = current;
        this.equivalenceRatio = er;
    }

    public double getCurrent() {
        return current;
    }

    public double getEquivalenceRatio() {
        return equivalenceRatio;
    }

    @Override
    public PID getPid() {
        return PID.O2_LAMBDA_PROBE_1_CURRENT;
    }

    @Override
    public Number getValue() {
        return getEquivalenceRatio();
    }
}
