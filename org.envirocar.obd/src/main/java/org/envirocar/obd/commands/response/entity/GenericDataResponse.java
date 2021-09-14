/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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

import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.response.DataResponse;

/**
 * Created by matthes on 03.11.15.
 */
public class GenericDataResponse extends DataResponse {
    private final PID pid;
    private final int[] processedData;
    private final byte[] rawData;

    public GenericDataResponse(PID pid, int[] processedData, byte[] rawData) {
        this.pid = pid;
        this.processedData = processedData;
        this.rawData = rawData;
    }

    public PID getPid() {
        return pid;
    }

    @Override
    public Number getValue() {
        return null;
    }

    public int[] getProcessedData() {
        return processedData;
    }

    public byte[] getRawData() {
        return rawData;
    }
}
