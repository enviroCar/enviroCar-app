/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
package org.envirocar.obd.commands.response.entity;

import com.google.common.base.MoreObjects;

import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.response.DataResponse;

/**
 * Created by matthes on 30.10.15.
 */
public class LambdaProbeVoltageResponse extends DataResponse {
    private final double voltage;
    private final double equivalenceRatio;

    public LambdaProbeVoltageResponse(double voltage, double er) {
        this.voltage = voltage;
        this.equivalenceRatio = er;
    }

    public double getVoltage() {
        return voltage;
    }

    public double getEquivalenceRatio() {
        return equivalenceRatio;
    }

    @Override
    public PID getPid() {
        return PID.O2_LAMBDA_PROBE_1_VOLTAGE;
    }

    @Override
    public Number getValue() {
        return getEquivalenceRatio();
    }

    @Override
    public boolean isComposite() {
        return true;
    }

    @Override
    public Number[] getCompositeValues() {
        return new Number[] {equivalenceRatio, voltage};
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("equivalenceRatio", equivalenceRatio)
                .add("voltage", voltage).toString();
    }
}
