package org.envirocar.obd.adapter;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.commands.BasicCommand;
import org.envirocar.obd.commands.CommonCommand;
import org.envirocar.obd.commands.StringResultCommand;
import org.envirocar.obd.protocol.exception.AdapterFailedException;
import org.envirocar.obd.protocol.sequential.ELM327Connector;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class CarTrendAdapter extends SequentialAdapter {

    private static final Logger logger = Logger.getLogger(CarTrendAdapter.class);
    private static final int MAX_METADATA_COUNT = 25;
    private int requiredCount;
    private boolean protocolFound;
    private boolean identifySuccess;
    private int metadataResponseCount;
    private boolean connectionEstablished;

    @Override
    protected List<BasicCommand> providePendingCommands() {
        if (this.connectionEstablished) {
            return super.defaultCycleCommands();
        }

        List<BasicCommand> result = new ArrayList<BasicCommand>();
        result.add(new EmptyCommand());
        result.add(new IdentifyCommand());
        result.add(new EmptyCommand());
        result.add(new ProtocolCommand("S"));
        result.add(new ProtocolCommand("1"));
        result.add(new ProtocolCommand("2"));
        result.add(new ProtocolCommand("3"));
        result.add(new ProtocolCommand("4"));
        result.add(new ProtocolCommand("5"));
        result.add(new ProtocolCommand("6"));
        result.add(new ConfigCommand("@E0"));
        result.add(new ConfigCommand("@H1"));

        return result;
    }

    @Override
    protected boolean analyzeMetadataResponse(byte[] response, BasicCommand sentCommand) throws AdapterFailedException {
        if (response == null || response.length == 0) {
            return false;
        }
        
        if (++this.metadataResponseCount > MAX_METADATA_COUNT) {
            throw new AdapterFailedException("Too many tries. Could not establish data link");
        }

        String asString = new String(response).toLowerCase();

        if (sentCommand instanceof IdentifyCommand) {
            if (asString.contains("MS4200")) {
                identifySuccess = true;
            }
        }

        if (identifySuccess && asString.contains("onnected")) {
            this.connectionEstablished = true;
        }

        return this.connectionEstablished;
    }

    @Override
    protected byte[] preProcess(byte[] bytes) {
        return bytes;
    }

    @Override
    public boolean supportsDevice(String deviceName) {
        return deviceName.toLowerCase().contains("cartrend");
    }


    private static class ConfigCommand extends GenericCommand {

        protected ConfigCommand(String content) {
            super(content);
        }

    }

    private class ProtocolCommand extends GenericCommand {

        protected ProtocolCommand(String id) {
            super("@P".concat(id));
        }

        @Override
        public byte[] getOutputBytes() {
            return CarTrendAdapter.this.protocolFound ? new byte[0] : super.getOutputBytes();
        }

    }

    private static class EmptyCommand extends GenericCommand {

        protected EmptyCommand() {
            super("");
        }

    }

    private static class IdentifyCommand extends GenericCommand {

        protected IdentifyCommand() {
            super("@");
        }

    }

    private static class GenericCommand extends BasicCommand {

        private final String name;

        protected GenericCommand(String content) {
            super(null, null);
            this.name = content;
        }

        @Override
        public byte[] getOutputBytes() {

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                logger.warn(e.getMessage(), e);
            }

            return this.name.getBytes();
        }
    }

}
