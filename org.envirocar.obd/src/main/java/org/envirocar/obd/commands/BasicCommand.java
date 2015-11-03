package org.envirocar.obd.commands;

public interface BasicCommand {

    byte[] getOutputBytes();

    boolean awaitsResults();

}
