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
package org.envirocar.core.events.gps;

import com.google.common.base.MoreObjects;

/**
 * Holder class for the positional, horizontal, and vertical Dulition of Precision (DOP)
 */
public class GpsDOP {

    private Double vdop;
    private Double hdop;
    private Double pdop;

    /**
     * Constructor.
     *
     * @param pdop  positional DOP
     * @param hdop  horizontal DOP
     * @param vdop  vertical DOP
     */
    public GpsDOP(Double pdop, Double hdop, Double vdop) {
        this.pdop = pdop;
        this.hdop = hdop;
        this.vdop = vdop;
    }

    /**
     * Returns the vertical DOP.
     *
     * @return the vertical DOP.
     */
    public Double getVdop() {
        return vdop;
    }

    /**
     * Returns the horizontal DOP.
     *
     * @return the horizontal DOP.
     */
    public Double getHdop() {
        return hdop;
    }


    public Double getPdop() {
        return pdop;
    }

    public boolean hasVdop() {
        return vdop != null && vdop != 0.0;
    }

    public boolean hasPdop() {
        return pdop != null && pdop != 0.0;
    }

    /**
     * Returns true if the horizontal DOP is correctly set.
     *
     * @return true if hdop is correctly set.
     */
    public boolean hasHdop() {
        return hdop != null && hdop != 0.0;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("PDOP", pdop)
                .add("HDOP", hdop)
                .add("VDOP", vdop)
                .toString();
    }
}
