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

public class PIDUtil {

	public enum PID {

		FUEL_SYSTEM_STATUS {
			@Override
			public String toString() {
				return "03";
			}
		},
		CALCULATED_ENGINE_LOAD {
			@Override
			public String toString() {
				return "04";
			}
		},
		FUEL_PRESSURE {
			@Override
			public String toString() {
				return "0A";
			}
		},
		INTAKE_MAP {
			@Override
			public String toString() {
				return "0B";
			}
		},
		RPM {
			@Override
			public String toString() {
				return "0C";
			}
		},
		SPEED {
			@Override
			public String toString() {
				return "0D";
			}
		},
		INTAKE_AIR_TEMP {
			@Override
			public String toString() {
				return "0F";
			}
		},
		MAF {
			@Override
			public String toString() {
				return "10";
			}
		},
		TPS {
			@Override
			public String toString() {
				return "11";
			}
		}
	}

	public static PID fromString(String s) {
		if (s == null || s.isEmpty()) return null;
		
		if (s.equalsIgnoreCase(PID.FUEL_SYSTEM_STATUS.toString())) {
			return PID.FUEL_SYSTEM_STATUS;
		}
		else if (s.equalsIgnoreCase(PID.CALCULATED_ENGINE_LOAD.toString())) {
			return PID.CALCULATED_ENGINE_LOAD;
		}
		else if (s.equalsIgnoreCase(PID.FUEL_PRESSURE.toString())) {
			return PID.FUEL_PRESSURE;
		}
		else if (s.equalsIgnoreCase(PID.INTAKE_MAP.toString())) {
			return PID.INTAKE_MAP;
		}
		else if (s.equalsIgnoreCase(PID.RPM.toString())) {
			return PID.RPM;
		}
		else if (s.equalsIgnoreCase(PID.SPEED.toString())) {
			return PID.SPEED;
		}
		else if (s.equalsIgnoreCase(PID.INTAKE_AIR_TEMP.toString())) {
			return PID.INTAKE_AIR_TEMP;
		}
		else if (s.equalsIgnoreCase(PID.MAF.toString())) {
			return PID.MAF;
		}
		else if (s.equalsIgnoreCase(PID.TPS.toString())) {
			return PID.TPS;
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
		default:
			return null;
		}
	}
}
