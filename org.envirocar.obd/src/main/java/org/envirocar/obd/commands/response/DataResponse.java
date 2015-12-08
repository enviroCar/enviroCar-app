package org.envirocar.obd.commands.response;

import org.envirocar.obd.commands.PID;

public abstract class DataResponse extends CommandResponse {

    private long timestamp;

    public DataResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public abstract PID getPid();

    public abstract Number getValue();
}
