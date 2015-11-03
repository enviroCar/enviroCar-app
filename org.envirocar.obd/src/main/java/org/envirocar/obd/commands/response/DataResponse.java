package org.envirocar.obd.commands.response;

public class DataResponse extends CommandResponse {

    private long timestamp;

    public DataResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }
}
