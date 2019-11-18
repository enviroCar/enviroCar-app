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
package org.envirocar.obd.adapter;

import android.util.Base64;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.commands.request.BasicCommand;
import org.envirocar.obd.commands.request.elm.ConfigurationCommand;
import org.envirocar.obd.commands.request.elm.Timeout;
import org.envirocar.obd.exception.AdapterFailedException;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * @author dewall
 */
public class OBDLinkAdapter extends ELM327Adapter{
    private static final Logger LOG = Logger.getLogger(OBDLinkAdapter.class);

    @Override
    protected Queue<BasicCommand> createInitCommands() {
        Queue<BasicCommand> result = new ArrayDeque<>();
        result.add(new ConfigurationCommand("AT Z", ConfigurationCommand.Instance.RESET, true));
        result.add(ConfigurationCommand.instance(ConfigurationCommand.Instance.ECHO_OFF));
        result.add(ConfigurationCommand.instance(ConfigurationCommand.Instance.ECHO_OFF));
        result.add(ConfigurationCommand.instance(ConfigurationCommand.Instance.MEMORY_OFF));
        result.add(ConfigurationCommand.instance(ConfigurationCommand.Instance.LINE_FEED_OFF));
        result.add(new Timeout(62));
        result.add(ConfigurationCommand.instance(ConfigurationCommand.Instance.SELECT_AUTO_PROTOCOL));
        return result;
    }

    @Override
    protected boolean analyzeMetadataResponse(byte[] response, BasicCommand sentCommand) throws AdapterFailedException {
        String content = new String(response);
        LOG.info("Analyzing metadata response: "+ Base64.encodeToString(response, Base64.DEFAULT));

        if (sentCommand == null || !(sentCommand instanceof ConfigurationCommand)) {
            return false;
        }

        ConfigurationCommand sent = (ConfigurationCommand) sentCommand;

        if (sent.getInstance() == ConfigurationCommand.Instance.RESET) {
            if (content.contains("ELM327") || content.contains("OK")) {
                succesfulCount++;
                certifiedConnection = true;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (sent.getInstance() == ConfigurationCommand.Instance.ECHO_OFF) {
            if (content.contains("ELM327") || content.contains("OK")) {
                succesfulCount++;
                certifiedConnection = true;

            }
        }

        else if (sent.getInstance() == ConfigurationCommand.Instance.LINE_FEED_OFF) {
            if (content.contains("OK")) {
                succesfulCount++;
            }
        }

        else if (sent instanceof Timeout) {
            if (content.contains("OK")) {
                succesfulCount++;
            }
        }

        else if (sent.getInstance() == ConfigurationCommand.Instance.SELECT_AUTO_PROTOCOL) {
            if (content.contains("OK")) {
                succesfulCount++;
            }
        }

        LOG.info("succesfulCount="+succesfulCount);

        return succesfulCount >= 5;
    }

    @Override
    public boolean supportsDevice(String deviceName) {
        return deviceName.toLowerCase().contains("obdlink");
    }
}
