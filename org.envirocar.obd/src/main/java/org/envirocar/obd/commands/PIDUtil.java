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
package org.envirocar.obd.commands;

public class PIDUtil {

	public static PID fromString(String s) {
		if (s == null || s.isEmpty()) return null;
		
		for (PID p : PID.values()) {
			if (s.equalsIgnoreCase(p.getHexadecimalRepresentation())) {
				return p;
			}
		}
		
		return null;
	}

	
	public static CommonCommand instantiateCommand(String pid) {
		return instantiateCommand(fromString(pid));
	}
	
	public static CommonCommand instantiateCommand(PID pid) {
		switch (pid) {
		case FUEL_SYSTEM_STATUS:
			return new FuelSystemStatus();
		case CALCULATED_ENGINE_LOAD:
			return new EngineLoad();
		case FUEL_PRESSURE:
			return new FuelPressure();
		case INTAKE_MAP:
			return new IntakePressure();
		case RPM:
			return new RPM();
		case SPEED:
			return new Speed();
		case INTAKE_AIR_TEMP:
			return new IntakeTemperature();
		case MAF:
			return new MAF();
		case TPS:
			return new TPS();
		case O2_LAMBDA_PROBE_1_VOLTAGE:
			return O2LambdaProbe.fromPIDEnum(pid);
		case O2_LAMBDA_PROBE_2_VOLTAGE:
			return O2LambdaProbe.fromPIDEnum(pid);
		case O2_LAMBDA_PROBE_3_VOLTAGE:
			return O2LambdaProbe.fromPIDEnum(pid);
		case O2_LAMBDA_PROBE_4_VOLTAGE:
			return O2LambdaProbe.fromPIDEnum(pid);
		case O2_LAMBDA_PROBE_5_VOLTAGE:
			return O2LambdaProbe.fromPIDEnum(pid);
		case O2_LAMBDA_PROBE_6_VOLTAGE:
			return O2LambdaProbe.fromPIDEnum(pid);
		case O2_LAMBDA_PROBE_7_VOLTAGE:
			return O2LambdaProbe.fromPIDEnum(pid);
		case O2_LAMBDA_PROBE_8_VOLTAGE:
			return O2LambdaProbe.fromPIDEnum(pid);
		default:
			return null;
		}
	}
}
