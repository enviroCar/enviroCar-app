package org.envirocar.obd.commands.response;

import org.envirocar.obd.commands.PID;
import org.envirocar.obd.events.Timestamped;

public abstract class DataResponse extends CommandResponse implements Timestamped {

    private final long timestamp;

    public DataResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public abstract PID getPid();

    public abstract Number getValue();

    public boolean isComposite() {
        return false;
    }

    public Number[] getCompositeValues() {
        return new Number[] {getValue()};
    }
}
