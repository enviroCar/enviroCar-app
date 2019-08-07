/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.obd.commands.request.elm;

import androidx.annotation.NonNull;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.commands.request.BasicCommand;

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
                return new ConfigurationCommand("AT Z", i, true);
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

    @NonNull
    @Override
    public String toString() {
        return instance.toString();
    }
}
