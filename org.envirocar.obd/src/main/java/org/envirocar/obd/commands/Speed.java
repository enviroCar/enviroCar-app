/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.obd.commands;

import org.envirocar.obd.commands.PIDUtil.PID;
import org.envirocar.core.logging.Logger;

/**
 * Speed Command PID 01 0D
 *
 * @author jakob
 */
public class Speed extends NumberResultCommand {

    private static final Logger LOG = Logger.getLogger(Speed.class);
    public static final String NAME = "Vehicle Speed";
    private int metricSpeed = Short.MIN_VALUE;

    private static int mLastVal = 0;

    public Speed() {
        super("01 ".concat(PID.SPEED.toString()));
    }

    @Override
    public void parseRawData() {
        super.parseRawData();

        if (getNumberResult() != null) {
            int val = getNumberResult().intValue();
            if (val - mLastVal > 49) {
                LOG.warn(String.format("Received a speed value of %s. this is probably an " +
                        "erroneous response", val));
            }
            mLastVal = val;
        }
    }

    @Override
    public String getCommandName() {
        return NAME;
    }

    @Override
    public Number getNumberResult() {
        int[] buffer = getBuffer();
        if (metricSpeed == Short.MIN_VALUE) {
            metricSpeed = buffer[2];
        }
        return metricSpeed;
    }

}