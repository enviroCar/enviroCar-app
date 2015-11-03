package org.envirocar.obd.commands.elm;

import org.envirocar.obd.commands.BasicCommand;

public class ConfigurationCommand implements BasicCommand {

    private final String output;
    private final boolean awaitsResult;
    private final Instance instance;

    public ConfigurationCommand(String output, Instance i, boolean awaitsResult) {
        this.output = output;
        this.awaitsResult = awaitsResult;
        this.instance = i;
    }

    @Override
    public byte[] getOutputBytes() {
        return this.output.getBytes();
    }

    @Override
    public boolean awaitsResults() {
        return this.awaitsResult;
    }

    public Instance getInstance() {
        return instance;
    }

    public static ConfigurationCommand instance(Instance i) {
        switch (i) {
            case DEFAULTS:
                return new ConfigurationCommand("AT D", i, true);
            case ECHO_OFF:
                return new ConfigurationCommand("AT E0", i, true);
            case HEADERS_ON:
                return new ConfigurationCommand("AT H1", i, true);
            case HEADERS_OFF:
                return new ConfigurationCommand("AT H0", i, true);
            case LINE_FEED_OFF:
                return new ConfigurationCommand("AT L0", i, true);
            case MEMORY_OFF:
                return new ConfigurationCommand("AT M0", i, true);
            case RESET:
                return new ConfigurationCommand("AT Z", i, false);
            case SELECT_AUTO_PROTOCOL:
                return new ConfigurationCommand("AT SP 0", i, true);
            case SPACES_OFF:
                return new ConfigurationCommand("AT S0", i, true);
        }

        return null;
    }

    public enum Instance {
        DEFAULTS,
        ECHO_OFF,
        HEADERS_ON,
        HEADERS_OFF,
        LINE_FEED_OFF,
        MEMORY_OFF,
        RESET,
        SELECT_AUTO_PROTOCOL,
        TIMEOUT,
        SPACES_OFF
    }

}
