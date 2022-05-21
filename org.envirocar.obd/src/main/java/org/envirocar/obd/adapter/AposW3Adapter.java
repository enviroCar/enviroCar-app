/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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
package org.envirocar.obd.adapter;

import org.envirocar.obd.commands.CycleCommandProfile;
import org.envirocar.obd.commands.request.BasicCommand;
import org.envirocar.obd.commands.request.elm.ConfigurationCommand;
import org.envirocar.obd.commands.request.elm.DelayedConfigurationCommand;
import org.envirocar.obd.commands.request.elm.Timeout;
import org.envirocar.obd.exception.AdapterFailedException;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Created by matthes on 03.11.15.
 */
public class AposW3Adapter extends ELM327Adapter {

    public AposW3Adapter(CycleCommandProfile cmp) {
        super(cmp);
    }

    @Override
    protected Queue<BasicCommand> createInitCommands() {
        Queue<BasicCommand> result = new ArrayDeque<>();
        result.add(ConfigurationCommand.instance(ConfigurationCommand.Instance.RESET));

        /**
         * hack for too fast init requests,
         * issue observed with Galaxy Nexus (4.3) and VW Tiguan 2013
         */
        result.add(new DelayedConfigurationCommand("AT E0", ConfigurationCommand.Instance.ECHO_OFF, false, 250));
        result.add(new DelayedConfigurationCommand("AT E0", ConfigurationCommand.Instance.ECHO_OFF, false, 250));
        result.add(ConfigurationCommand.instance(ConfigurationCommand.Instance.LINE_FEED_OFF));
        result.add(ConfigurationCommand.instance(ConfigurationCommand.Instance.DEVICE_DESCRIPTION));
        result.add(ConfigurationCommand.instance(ConfigurationCommand.Instance.DEVICE_IDENTIFIER));
        result.add(new Timeout(62));
        result.add(ConfigurationCommand.instance(ConfigurationCommand.Instance.SELECT_AUTO_PROTOCOL));
        return result;
    }

    @Override
    protected boolean analyzeMetadataResponse(byte[] response, BasicCommand sentCommand) throws AdapterFailedException {
        if (sentCommand == null || !(sentCommand instanceof ConfigurationCommand)) {
            return false;
        }

        ConfigurationCommand sent = (ConfigurationCommand) sentCommand;

        if (sent.getInstance() == ConfigurationCommand.Instance.ECHO_OFF) {
            String content = new String(response);
            if (content.contains("OK")) {
                succesfulCount++;
            }
        } else {
            super.analyzeMetadataResponse(response, sentCommand);
        }

        return succesfulCount >= 4;
    }

    @Override
    public boolean supportsDevice(String deviceName) {
        return deviceName.contains("APOS") && deviceName.contains("OBD_W3");
    }

}
