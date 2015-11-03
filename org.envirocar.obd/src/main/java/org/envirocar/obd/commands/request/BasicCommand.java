package org.envirocar.obd.commands.request;

public interface BasicCommand {

    byte[] getOutputBytes();

    boolean awaitsResults();

}
