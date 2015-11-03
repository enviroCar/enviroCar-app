package org.envirocar.obd.commands.request;

import org.envirocar.obd.commands.PID;

/**
 * Created by matthes on 31.10.15.
 */
public class PIDCommand implements BasicCommand {

    private String mode;
    private PID pid;

    public PIDCommand(String mode, PID pid) {
        this.mode = mode;
        this.pid = pid;
    }

    public String getMode() {
        return mode;
    }

    public PID getPid() {
        return pid;
    }

    @Override
    public byte[] getOutputBytes() {
        int ml = mode.length();
        String pidString = pid.toString();
        int pl = pidString.length();
        byte[] bytes = new byte[ml + pl + 1];

        int pos = 0;
        for (int i = 0; i < ml; i++) {
            bytes[pos++] = (byte) mode.charAt(i);
        }

        bytes[pos++] = ' ';

        for (int i = 0; i < pl; i++) {
            bytes[pos++] = (byte) pidString.charAt(i);
        }

        return bytes;
    }

    @Override
    public boolean awaitsResults() {
        return true;
    }
}
