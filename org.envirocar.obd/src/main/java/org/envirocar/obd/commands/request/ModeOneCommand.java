package org.envirocar.obd.commands.request;

import org.envirocar.obd.commands.PID;

/**
 * Created by matthes on 31.10.15.
 */
public class ModeOneCommand extends PIDCommand {

    public ModeOneCommand(PID pid) {
        super("01", pid);
    }

}
