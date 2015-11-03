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
package org.envirocar.obd.adapter.async;

import org.envirocar.obd.commands.CommonCommand;
import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.PIDUtil;
import org.envirocar.obd.commands.request.BasicCommand;

import java.util.List;

public class CycleCommand implements BasicCommand {

	public static enum DriveDeckPID implements DriveDeckPIDEnumInstance {
		SPEED {
			@Override
			public String getByteRepresentation() {
				return convert(PID.SPEED.getHexadecimalRepresentation());
			}
		},
		MAF {
			@Override
			public String getByteRepresentation() {
				return convert(PID.MAF.getHexadecimalRepresentation());
			}
		},
		RPM {
			@Override
			public String getByteRepresentation() {
				return convert(PID.RPM.getHexadecimalRepresentation());
			}
		},
		IAP {
			@Override
			public String getByteRepresentation() {
				return convert(PID.INTAKE_MAP.getHexadecimalRepresentation());
			}
		},
		IAT {
			@Override
			public String getByteRepresentation() {
				return convert(PID.INTAKE_AIR_TEMP.getHexadecimalRepresentation());
			}
		},
		TPS {
			@Override
			public String getByteRepresentation() {
				return convert(PID.TPS.getHexadecimalRepresentation());
			}
		},
		ENGINE_LOAD {
			@Override
			public String getByteRepresentation() {
				return convert(PID.CALCULATED_ENGINE_LOAD.getHexadecimalRepresentation());
			}
		},
		O2_LAMBDA_PROBE_1_VOLTAGE {
			@Override
			public String getByteRepresentation() {
				return convert(PID.O2_LAMBDA_PROBE_1_VOLTAGE.getHexadecimalRepresentation());
			}
		},
		O2_LAMBDA_PROBE_1_CURRENT {
			@Override
			public String getByteRepresentation() {
				return convert(PID.O2_LAMBDA_PROBE_1_CURRENT.getHexadecimalRepresentation());
			}
		};
		
		protected String convert(String string) {
			return Integer.toString(incrementBy13(hexToInt(string)));
		}

		protected int hexToInt(String string) {
			return Integer.valueOf(string, 16);
		}

		protected int incrementBy13(int hexToInt) {
			return hexToInt + 13;
		}

		protected String intToHex(int val) {
			String result = Integer.toString(val, 16);
			if (result.length() == 1) result = "0"+result;
			return "0x".concat(result);
		}
	}

	public static final char RESPONSE_PREFIX_CHAR = 'B';
	public static final char TOKEN_SEPARATOR_CHAR = '<';
	private byte[] bytes;

	public CycleCommand(List<DriveDeckPID> pidList) {
		bytes = new byte[3 + pidList.size()];
		byte[] prefix = "a17".getBytes();
		
		for (int i = 0; i < prefix.length; i++) {
			bytes[i] = prefix[i];
		}
		
		int i = 0;
		for (DriveDeckPID pid : pidList) {
			bytes[prefix.length + i++] = (byte) Integer.valueOf(pid.getByteRepresentation()).intValue();
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
