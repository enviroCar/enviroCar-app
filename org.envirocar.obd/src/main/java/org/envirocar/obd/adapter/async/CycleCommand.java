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
package org.envirocar.obd.adapter.async;

import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.request.BasicCommand;

import java.util.List;

public class CycleCommand implements BasicCommand {

	private byte[] bytes;

	public static enum DriveDeckPID implements DriveDeckPIDEnumInstance {
		SPEED {
			@Override
			public byte getByteRepresentation() {
				return convert(PID.SPEED.getHexadecimalRepresentation());
			}
		},
		MAF {
			@Override
			public byte getByteRepresentation() {
				return convert(PID.MAF.getHexadecimalRepresentation());
			}
		},
		RPM {
			@Override
			public byte getByteRepresentation() {
				return convert(PID.RPM.getHexadecimalRepresentation());
			}
		},
		IAP {
			@Override
			public byte getByteRepresentation() {
				return convert(PID.INTAKE_MAP.getHexadecimalRepresentation());
			}
		},
		IAT {
			@Override
			public byte getByteRepresentation() {
				return convert(PID.INTAKE_AIR_TEMP.getHexadecimalRepresentation());
			}
		},
		TPS {
			@Override
			public byte getByteRepresentation() {
				return convert(PID.TPS.getHexadecimalRepresentation());
			}
		},
		ENGINE_LOAD {
			@Override
			public byte getByteRepresentation() {
				return convert(PID.CALCULATED_ENGINE_LOAD.getHexadecimalRepresentation());
			}
		},
		O2_LAMBDA_PROBE_1_VOLTAGE {
			@Override
			public byte getByteRepresentation() {
				return convert(PID.O2_LAMBDA_PROBE_1_VOLTAGE.getHexadecimalRepresentation());
			}
		},
		O2_LAMBDA_PROBE_1_CURRENT {
			@Override
			public byte getByteRepresentation() {
				return convert(PID.O2_LAMBDA_PROBE_1_CURRENT.getHexadecimalRepresentation());
			}
		};
		
		protected byte convert(String pidHex) {
            int by13 = incrementBy13(hexToInt(pidHex));
            return (byte) by13;
		}

		protected int hexToInt(String string) {
			return Integer.valueOf(string, 16);
		}

		protected int incrementBy13(int hexToInt) {
			return hexToInt + 13;
		}

        public static DriveDeckPID fromDefaultPID(PID p) {
            switch (p) {
                case SHORT_TERM_FUEL_TRIM_BANK_1:
                case LONG_TERM_FUEL_TRIM_BANK_1:
                case FUEL_PRESSURE:
                case FUEL_SYSTEM_STATUS:
                    return null;
                case CALCULATED_ENGINE_LOAD:
                    return DriveDeckPID.ENGINE_LOAD;
                case INTAKE_MAP:
                    return DriveDeckPID.IAP;
                case RPM:
                    return DriveDeckPID.RPM;
                case SPEED:
                    return DriveDeckPID.SPEED;
                case INTAKE_AIR_TEMP:
                    return DriveDeckPID.IAT;
                case MAF:
                    return DriveDeckPID.MAF;
                case TPS:
                    return DriveDeckPID.TPS;
                case O2_LAMBDA_PROBE_1_VOLTAGE:
                    return DriveDeckPID.O2_LAMBDA_PROBE_1_VOLTAGE;
                case O2_LAMBDA_PROBE_1_CURRENT:
                    return DriveDeckPID.O2_LAMBDA_PROBE_1_CURRENT;
                case O2_LAMBDA_PROBE_2_VOLTAGE:
                case O2_LAMBDA_PROBE_3_VOLTAGE:
                case O2_LAMBDA_PROBE_4_VOLTAGE:
                case O2_LAMBDA_PROBE_5_VOLTAGE:
                case O2_LAMBDA_PROBE_6_VOLTAGE:
                case O2_LAMBDA_PROBE_7_VOLTAGE:
                case O2_LAMBDA_PROBE_8_VOLTAGE:
                case O2_LAMBDA_PROBE_2_CURRENT:
                case O2_LAMBDA_PROBE_3_CURRENT:
                case O2_LAMBDA_PROBE_4_CURRENT:
                case O2_LAMBDA_PROBE_5_CURRENT:
                case O2_LAMBDA_PROBE_6_CURRENT:
                case O2_LAMBDA_PROBE_7_CURRENT:
                case O2_LAMBDA_PROBE_8_CURRENT:
                    return null;
            }

            return null;
        }
	}


	public CycleCommand(List<DriveDeckPID> pidList) {
		bytes = new byte[3 + pidList.size()];
		byte[] prefix = "A17".getBytes();
		
		for (int i = 0; i < prefix.length; i++) {
			bytes[i] = prefix[i];
		}
		
		int i = 0;
		for (DriveDeckPID pid : pidList) {
			bytes[prefix.length + i++] = pid.getByteRepresentation();
		}
	}

	@Override
	public byte[] getOutputBytes() {
		return bytes;
	}

	@Override
	public boolean awaitsResults() {
		return true;
	}

}
