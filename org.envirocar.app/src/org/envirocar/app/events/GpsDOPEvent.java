/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.app.events;

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
