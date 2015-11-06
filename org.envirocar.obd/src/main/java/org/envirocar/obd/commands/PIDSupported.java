package org.envirocar.obd.commands;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.envirocar.obd.commands.request.BasicCommand;
import org.envirocar.obd.commands.response.DataResponse;
import org.envirocar.obd.commands.response.ResponseParser;
import org.envirocar.obd.commands.response.entity.GenericDataResponse;
import org.envirocar.obd.exception.AdapterSearchingException;
import org.envirocar.obd.exception.NoDataReceivedException;
import org.envirocar.obd.exception.UnmatchedResponseException;
import org.envirocar.obd.exception.InvalidCommandResponseException;


/**
 * Turns off line-feed.
 */
public class PIDSupported implements BasicCommand {

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

    /**
     * @return the set of PIDs that are supported by a car,
     * encoded as their HEX byte strings
     */
    public Set<PID> getSupportedPIDs() {
        if (pids == null) {
            pids = new HashSet<PID>();

            for (int i = 0; i < bytes.length; i++) {
                int current = bytes[i];

                for (int bit = 3; bit >= 0; bit--) {
                    boolean is = ((current >> bit) & 1 ) == 1;
                    if (is) {
						/*
						 * we are starting at PID 01 and not 00
						 */
                        PID pid = PIDUtil.fromString(createHex(i*4 + (3-bit) + 1));
                        if (pid != null) {
                            pids.add(pid);
                        }
                    }
                }

            }
        }

        return pids;
    }


    private String createHex(int i) {
        String result = Integer.toString(i, 16);
        if (result.length() == 1) result = "0".concat(result);
        return result;
    }


    public void parseRawData(byte[] rawData) throws InvalidCommandResponseException, NoDataReceivedException, UnmatchedResponseException, AdapterSearchingException {
        DataResponse parsed = new ResponseParser().parse(preProcessRawData(rawData));

        GenericDataResponse generic;
        if (parsed instanceof GenericDataResponse) {
            generic = (GenericDataResponse) parsed;
        }
        else {
            return;
        }

        int index = 4;
        int length = 2;

        byte[] data = generic.getRawData();

        bytes = new byte[data.length-4];

        if (bytes.length != 8) {
            throw new InvalidCommandResponseException(((GenericDataResponse) parsed).getPid().toString());
        }

        while (index < data.length) {
			/*
			 * this is a hex number
			 */
            bytes[index-4] = (byte) Integer.valueOf(String.valueOf((char) data[index]), 16).intValue();
            if (bytes[index-4] < 0){
                throw new InvalidCommandResponseException(((GenericDataResponse) parsed).getPid().toString());
            }
            index++;
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
        return bytes;
    }

    @Override
    public boolean awaitsResults() {
        return true;
    }

    public String getGroup() {
        return group;
    }
}