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

import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.response.DataResponse;

/**
 * Created by matthes on 02.11.15.
 */
public class ShortTermFuelTrimResponse extends DataResponse {
    private final double value;
    private final int bank;

    public ShortTermFuelTrimResponse(double v, int bank) {
        this.value = v;
        this.bank = bank;
    }

    public Number getValue() {
        return value;
    }

    public int getBank() {
        return bank;
    }

    @Override
    public PID getPid() {
        return PID.SHORT_TERM_FUEL_TRIM_BANK_1;
    }
}
