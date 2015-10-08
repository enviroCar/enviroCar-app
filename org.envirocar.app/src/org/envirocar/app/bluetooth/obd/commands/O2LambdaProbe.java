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

import org.envirocar.app.bluetooth.obd.commands.PIDUtil.PID;
import org.envirocar.core.logging.Logger;

public abstract class O2LambdaProbe extends NumberResultCommand {

	private static final Logger logger = Logger.getLogger(O2LambdaProbe.class);
	private String cylinderPosition;
	private double equivalenceRation = Double.NaN;
	private String pid;

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
		this.pid = cylinderPosition;
	}
	
	@Override
	public Number getNumberResult() {
		//command provides two results
		return null;
	}
	
	@Override
	public void parseRawData() {
		super.parseRawData();
		if (getBuffer() == null || getBuffer().length < 6) {
			setCommandState(CommonCommandState.EXECUTION_ERROR);
			// TODO
//			logger.warn("The response did not contain the correct expected count: "+
//					(getBuffer() == null ? "null" : getBuffer().length));
		}
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

	public String getPID() {
		return pid;
	}
	
	public String lambdaString() {
		return getClass().getSimpleName() +" ("+pid+"): "+getEquivalenceRatio() +"; "+getNumberResult();
	}
	
}
