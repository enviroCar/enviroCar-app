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
package org.envirocar.core.events.gps;

import com.google.common.base.MoreObjects;


/**
 * Event holder for the Dilution of Precision (DOP)
 */
public class GpsDOPEvent {

    public final GpsDOP mDOP;

    /**
     * Constructor.
     *
     * @param dop the dilution of precision holder.
     */
    public GpsDOPEvent(GpsDOP dop) {
        this.mDOP = dop;
    }

    /**
     * Constructor.
     *
     * @param pdop Positional DOP
     * @param hdop Horizontal DOP
     * @param vdop Vertical DOP
     */
    public GpsDOPEvent(Double pdop, Double hdop, Double vdop) {
        this.mDOP = new GpsDOP(pdop, hdop, vdop);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("PDOP", mDOP.getPdop())
                .add("HDOP", mDOP.getHdop())
                .add("VDOP", mDOP.getVdop())
                .toString();
    }
}
