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
package org.envirocar.app.commands;

import org.envirocar.app.commands.PIDUtil.PID;

public abstract class O2LambdaProbe extends NumberResultCommand {

	private String cylinderPosition;
	private double equivalenceRation = Double.NaN;

	public static O2LambdaProbe fromPIDEnum(PID pid) {
		switch (pid) {
		case O2_LAMBDA_PROBE_1_VOLTAGE:
			return new O2LambdaProbeVoltage(pid.toString()); 
		case O2_LAMBDA_PROBE_2_VOLTAGE:
			return new O2LambdaProbeVoltage(pid.toString());
		case O2_LAMBDA_PROBE_3_VOLTAGE:
			return new O2LambdaProbeVoltage(pid.toString());
		case O2_LAMBDA_PROBE_4_VOLTAGE:
			return new O2LambdaProbeVoltage(pid.toString());
		case O2_LAMBDA_PROBE_5_VOLTAGE:
			return new O2LambdaProbeVoltage(pid.toString());
		case O2_LAMBDA_PROBE_6_VOLTAGE:
			return new O2LambdaProbeVoltage(pid.toString());
		case O2_LAMBDA_PROBE_7_VOLTAGE:
			return new O2LambdaProbeVoltage(pid.toString());
		case O2_LAMBDA_PROBE_8_VOLTAGE:
			return new O2LambdaProbeVoltage(pid.toString());
		case O2_LAMBDA_PROBE_1_CURRENT:
			return new O2LambdaProbeCurrent(pid.toString());
		case O2_LAMBDA_PROBE_2_CURRENT:
			return new O2LambdaProbeCurrent(pid.toString());
		case O2_LAMBDA_PROBE_3_CURRENT:
			return new O2LambdaProbeCurrent(pid.toString());
		case O2_LAMBDA_PROBE_4_CURRENT:
			return new O2LambdaProbeCurrent(pid.toString());
		case O2_LAMBDA_PROBE_5_CURRENT:
			return new O2LambdaProbeCurrent(pid.toString());
		case O2_LAMBDA_PROBE_6_CURRENT:
			return new O2LambdaProbeCurrent(pid.toString());
		case O2_LAMBDA_PROBE_7_CURRENT:
			return new O2LambdaProbeCurrent(pid.toString());
		case O2_LAMBDA_PROBE_8_CURRENT:
			return new O2LambdaProbeCurrent(pid.toString());
			
		default:
			break;
		}
		
		return null;
	}
	
	public O2LambdaProbe(String cylinderPosition) {
		super("01 ".concat(cylinderPosition));
		this.cylinderPosition = cylinderPosition;
	}
	
	@Override
	public Number getNumberResult() {
		//command provides two results
		return null;
	}
	
	@Override
	public void parseRawData() {
		super.parseRawData();
	}
	
	public double getEquivalenceRatio() {
		if (Double.isNaN(this.equivalenceRation)) {
			int[] data = getBuffer();
			
			this.equivalenceRation = ((data[2]*256d)+data[3])/32768d;
		}
		
		return this.equivalenceRation;
	}
	

	@Override
	public String getCommandName() {
		return "O2 Lambda Probe "+cylinderPosition;
	}
	
}
