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
