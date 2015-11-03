package org.envirocar.obd.adapter;

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
