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
import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.PIDUtil;
import org.envirocar.obd.exception.AdapterFailedException;

import java.util.ArrayDeque;
import java.util.Queue;

public class UniCarScanAdapter extends OBDLinkAdapter {

    public UniCarScanAdapter(CycleCommandProfile cmp) {
        super(cmp);
    }

    protected Queue<BasicCommand> createInitCommands() {
        Queue<BasicCommand> result = new ArrayDeque<>();
        result.add(new DelayedConfigurationCommand("ATD", ConfigurationCommand.Instance.DEFAULTS, true, 100));
        result.add(new DelayedConfigurationCommand("ATZ", ConfigurationCommand.Instance.RESET, true, 100));
        result.add(new DelayedConfigurationCommand("AT@1", ConfigurationCommand.Instance.DEVICE_DESCRIPTION, true, 100));
        result.add(new DelayedConfigurationCommand("AT@2", ConfigurationCommand.Instance.DEVICE_IDENTIFIER, true, 100));
        result.add(new DelayedConfigurationCommand("ATI", ConfigurationCommand.Instance.IDENTIFY, true, 100));
        result.add(ConfigurationCommand.instance(ConfigurationCommand.Instance.SELECT_AUTO_PROTOCOL));
        result.add(new DelayedConfigurationCommand("ATE0", ConfigurationCommand.Instance.ECHO_OFF, true, 100));
        result.add(new DelayedConfigurationCommand("ATL0", ConfigurationCommand.Instance.LINE_FEED_OFF, true, 100));
        result.add(new DelayedConfigurationCommand("ATH1", ConfigurationCommand.Instance.HEADERS_ON, true, 100));
        result.add(new DelayedConfigurationCommand("ATDPN", ConfigurationCommand.Instance.DESCRIBE_PROTOCOL_NUMBER, true, 100));

        return result;
    }

    @Override
    protected boolean analyzeMetadataResponse(byte[] response, BasicCommand sentCommand) throws AdapterFailedException {
        super.analyzeMetadataResponse(response, sentCommand);
        
        // try {
        //     Thread.sleep(5000);
        // } catch (InterruptedException e) {
        //     throw new AdapterFailedException("UniCarScan", new java.util.concurrent.RejectedExecutionException(e));
        // }
        
        return succesfulCount >= 4;
    }

    @Override
    public boolean supportsDevice(String deviceName) {
        return deviceName.contains("UniCarScan");
    }
}
