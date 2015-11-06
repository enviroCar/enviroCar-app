package org.envirocar.obd.commands.response.entity;

import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.response.DataResponse;

/**
 * Created by matthes on 03.11.15.
 */
public class GenericDataResponse extends DataResponse {
    private final PID pid;
    private final int[] processedData;
    private final byte[] rawData;

    public GenericDataResponse(PID pid, int[] processedData, byte[] rawData) {
        this.pid = pid;
        this.processedData = processedData;
        this.rawData = rawData;
    }

    public PID getPid() {
        return pid;
    }

    public int[] getProcessedData() {
        return processedData;
    }

    public byte[] getRawData() {
        return rawData;
    }
}
