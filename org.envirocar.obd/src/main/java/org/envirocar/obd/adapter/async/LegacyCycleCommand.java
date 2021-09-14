/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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

import org.envirocar.obd.commands.request.BasicCommand;

import java.util.List;

public class LegacyCycleCommand implements BasicCommand {


    public enum PID {
        SPEED {
            @Override
            public String toString() {
                return convert("0D");
            }
        },
        MAF {
            @Override
            public String toString() {
                return convert("10");
            }
        },
        RPM {
            @Override
            public String toString() {
                return convert("0C");
            }
        },
        IAP {
            @Override
            public String toString() {
                return convert("0B");
            }
        },
        IAT {
            @Override
            public String toString() {
                return convert("0F");
            }
        },
        SHORT_TERM_FUEL_TRIM {
            @Override
            public String toString() {
                return convert("06");
            }
        },
        LONG_TERM_FUEL_TRIM {
            @Override
            public String toString() {
                return convert("07");
            }
        },
        O2_LAMBDA_PROBE_1_VOLTAGE {
            @Override
            public String toString() {
                return convert("24");
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

    private static final String NAME = "a17";
    public static final char RESPONSE_PREFIX_CHAR = 'B';
    public static final char TOKEN_SEPARATOR_CHAR = '<';
    private byte[] bytes;


    public LegacyCycleCommand(List<PID> pidList) {
        bytes = new byte[3+pidList.size()];
        byte[] prefix = "a17".getBytes();

        for (int i = 0; i < prefix.length; i++) {
            bytes[i] = prefix[i];
        }

        int i = 0;
        for (PID pid : pidList) {
            bytes[prefix.length + i++] = (byte) Integer.valueOf(pid.toString()).intValue();
        }
    }

    @Override
    public byte[] getOutputBytes() {
        return bytes;
    }

    @Override
    public boolean awaitsResults() {
        return false;
    }

}
