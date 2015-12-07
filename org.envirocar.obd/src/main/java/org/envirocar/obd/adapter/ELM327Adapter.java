package org.envirocar.obd.adapter;

import org.envirocar.obd.commands.request.BasicCommand;
import org.envirocar.obd.commands.request.PIDCommand;
import org.envirocar.obd.commands.request.elm.ConfigurationCommand;
import org.envirocar.obd.commands.request.elm.Timeout;
import org.envirocar.obd.exception.AdapterFailedException;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import rx.Observable;

/**
 * Created by matthes on 02.11.15.
 */
public class ELM327Adapter extends SyncAdapter {


    private Queue<BasicCommand> initCommands;
    protected int succesfulCount;


    @Override
    protected BasicCommand pollNextInitializationCommand() {
        return this.initCommands.poll();
    }

    @Override
    public Observable<Boolean> initialize(InputStream is, OutputStream os) {
        this.initCommands = createInitCommands();
        return super.initialize(is, os);
    }

    protected Queue<BasicCommand> createInitCommands() {
        Queue<BasicCommand> result = new ArrayDeque<>();
        result.add(ConfigurationCommand.instance(ConfigurationCommand.Instance.RESET));
        result.add(ConfigurationCommand.instance(ConfigurationCommand.Instance.ECHO_OFF));
        result.add(ConfigurationCommand.instance(ConfigurationCommand.Instance.ECHO_OFF));
        result.add(ConfigurationCommand.instance(ConfigurationCommand.Instance.LINE_FEED_OFF));
        result.add(new Timeout(62));
        result.add(ConfigurationCommand.instance(ConfigurationCommand.Instance.SELECT_AUTO_PROTOCOL));
        return result;
    }

    @Override
    protected List<PIDCommand> providePendingCommands() {
        return super.defaultCycleCommands();
    }

    @Override
    protected boolean analyzeMetadataResponse(byte[] response, BasicCommand sentCommand) throws AdapterFailedException {
        String content = new String(response);

        if (sentCommand == null || !(sentCommand instanceof ConfigurationCommand)) {
            return false;
        }

        ConfigurationCommand sent = (ConfigurationCommand) sentCommand;

        if (sent.getInstance() == ConfigurationCommand.Instance.ECHO_OFF) {
            if (content.contains("ELM327v1.")) {
                succesfulCount++;
            }
            else if (content.contains("ATE0") && content.contains("OK")) {
                succesfulCount++;
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

        return succesfulCount >= 5;
    }

    @Override
    protected byte[] preProcess(byte[] bytes) {
        return bytes;
    }

    @Override
    public boolean supportsDevice(String deviceName) {
        return deviceName.contains("OBDII") || deviceName.contains("ELM327");
    }

    @Override
    public boolean hasVerifiedConnection() {
        return false;
    }
}
