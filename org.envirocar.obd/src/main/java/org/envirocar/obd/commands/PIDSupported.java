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
package org.envirocar.obd.commands;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.commands.request.BasicCommand;
import org.envirocar.obd.exception.InvalidCommandResponseException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class PIDSupported implements BasicCommand {
    private static final Logger LOG = Logger.getLogger(PIDSupported.class);

    private final byte[] output;
    private Set<PID> pids;
    private byte[] bytes;
    private String group;

    public PIDSupported() {
        this("00");
    }

    /**
     * @param group the group of commands ("00", "20", "40" ...)
     */
    public PIDSupported(String group) {
        this.output = "01 ".concat(group).getBytes();
        this.group = group;
    }

    private String createHex(int i) {
        String result = Integer.toString(i, 16);
        if (result.length() == 1) result = "0".concat(result);
        return result;
    }


    public Set<PID> parsePIDs(byte[] rawData) throws InvalidCommandResponseException {
        if (rawData == null) {
            throw new InvalidCommandResponseException("Null response on PIDSupported request");
        }

        String rawAsString = new String(rawData);
        int startIndex = rawAsString.indexOf("41".concat(group));
        if (startIndex >= 0) {
            if (rawData.length < startIndex + 12) {
                throw new InvalidCommandResponseException("The response was too small");
            }

            String receivedGroup = rawAsString.substring(startIndex + 2, startIndex + 4);
            if (!receivedGroup.equals(group)) {
                throw new InvalidCommandResponseException("Unexpected group received: " + receivedGroup);
            }
            rawData = rawAsString.substring(startIndex + 4, startIndex + 12).getBytes();
        } else {
            throw new InvalidCommandResponseException("The expected status response '41" + group + "' was not in the response");
        }

        if (rawData.length != 8) {
            throw new InvalidCommandResponseException("Invalid PIDSupported length: " + rawData.length);
        }

        try {
            List<Integer> pids = new ArrayList<>(8);

            int groupOffset = Integer.parseInt(this.group, 16);
            char[] binaries;
            byte b;

            /**
             * the 32 PIDs of the group are encoded bitwise from MSB to LSB (resulting in 8 bytes):
             * assuming group 00 and the first byte is a 0x0A = (int) 10,
             * then bits 4 (MSB) and 2 are set, resulting in PIDs 0x01 and 0x03 (counting starts at
             * PID 0x01, 0x21, ...) are supported
             */
            for (int i = 0; i < rawData.length; i++) {
                b = rawData[i];
                int fromHex = Integer.parseInt(new String(new char[]{'0', (char) b}), 16);
                BigInteger bigInt = BigInteger.valueOf(fromHex);
                for (int j = 3; j >= 0; j--) {
                    //check from MSB down to LSB
                    if (bigInt.testBit(j)) {
                        //create an int representation and apply the group offset
                        pids.add(1 + (i * 4 + 3 - j) + groupOffset);
                    }
                }
            }

            Set<PID> list = new HashSet<>();
            /**
             * conver to hex string so the PIDUtil can parse it
             */
            for (Integer pidInt : pids) {
                LOG.info("Supported RAW PIDs: " + pidInt);

                String hex = Integer.toHexString(pidInt);
                if (hex.length() == 1) {
                    hex = "0".concat(hex);
                }
                PID tmp = PIDUtil.fromString(hex);
                if (tmp != null) {
                    list.add(tmp);
                }
            }

            return list;
        } catch (RuntimeException e) {
            throw new InvalidCommandResponseException("The response contained invalid byte values: " + e.getMessage());
        }

    }


    private byte[] preProcessRawData(byte[] data) {
        String str = new String(data);
        if (str.contains("41".concat(this.group))) {
            int index = str.indexOf("41".concat(this.group));
            return Arrays.copyOfRange(data, index, data.length);
        }
        return data;
    }

    @Override
    public byte[] getOutputBytes() {
        return output;
    }

    @Override
    public boolean awaitsResults() {
        return true;
    }

    public String getGroup() {
        return group;
    }
}