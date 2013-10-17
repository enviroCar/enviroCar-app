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
package org.envirocar.app.event;

public class GpsDOP {

	private Double vdop;
	private Double hdop;
	private Double pdop;

	public GpsDOP(Double pdop, Double hdop, Double vdop) {
		this.pdop = pdop;
		this.hdop = hdop;
		this.vdop = vdop;
	}

	public Double getVdop() {
		return vdop;
	}

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
	
	public boolean hasHdop() {
		return hdop != null && hdop != 0.0;
	}
	

}
