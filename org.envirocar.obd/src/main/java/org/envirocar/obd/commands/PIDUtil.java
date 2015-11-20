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
		},
		O2_LAMBDA_PROBE_1_VOLTAGE {
			@Override
			public String toString() {
				return "24";
			}
		},
		O2_LAMBDA_PROBE_2_VOLTAGE {
			@Override
			public String toString() {
				return "25";
			}
		},
		O2_LAMBDA_PROBE_3_VOLTAGE {
			@Override
			public String toString() {
				return "26";
			}
		},
		O2_LAMBDA_PROBE_4_VOLTAGE {
			@Override
			public String toString() {
				return "27";
			}
		},
		O2_LAMBDA_PROBE_5_VOLTAGE {
			@Override
			public String toString() {
				return "28";
			}
		},
		O2_LAMBDA_PROBE_6_VOLTAGE {
			@Override
			public String toString() {
				return "29";
			}
		},
		O2_LAMBDA_PROBE_7_VOLTAGE {
			@Override
			public String toString() {
				return "2A";
			}
		},
		O2_LAMBDA_PROBE_8_VOLTAGE {
			@Override
			public String toString() {
				return "2B";
			}
		}, O2_LAMBDA_PROBE_1_CURRENT {
			public String toString() {
				return "34";
			};
		}
		, O2_LAMBDA_PROBE_2_CURRENT {
			public String toString() {
				return "35";
			};
		}
		, O2_LAMBDA_PROBE_3_CURRENT {
			public String toString() {
				return "36";
			};
		}
		, O2_LAMBDA_PROBE_4_CURRENT {
			public String toString() {
				return "37";
			};
		}
		, O2_LAMBDA_PROBE_5_CURRENT {
			public String toString() {
				return "38";
			};
		}
		, O2_LAMBDA_PROBE_6_CURRENT {
			public String toString() {
				return "39";
			};
		}
		, O2_LAMBDA_PROBE_7_CURRENT {
			public String toString() {
				return "3A";
			};
		}
		, O2_LAMBDA_PROBE_8_CURRENT {
			public String toString() {
				return "3B";
			};
		}
	}

	public static PID fromString(String s) {
		if (s == null || s.isEmpty()) return null;
		
		for (PID p : PID.values()) {
			if (s.equalsIgnoreCase(p.toString())) {
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
