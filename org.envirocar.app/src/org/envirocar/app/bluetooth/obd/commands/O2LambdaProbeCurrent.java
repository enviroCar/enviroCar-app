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
package org.envirocar.app.bluetooth.obd.commands;

public class O2LambdaProbeCurrent extends O2LambdaProbe {

	private double current = Double.NaN;
	
	public O2LambdaProbeCurrent(String cylinderPosition) {
		super(cylinderPosition);
	}

	public double getCurrent() {
		if (Double.isNaN(current)) {
			int[] data = getBuffer();
			
			this.current = ((data[4]*256d)+data[5])/256d - 128;
		}
		
		return current;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName());
		sb.append(" Current: ");
		sb.append(getCurrent());
		sb.append("; Equivalence Ratio: ");
		sb.append(getEquivalenceRatio());
		return sb.toString();
	}

}
