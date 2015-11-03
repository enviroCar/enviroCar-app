package org.envirocar.obd.protocol.sequential;

import org.envirocar.obd.commands.CommonCommand;
import org.envirocar.core.logging.Logger;

import java.util.ArrayList;
import java.util.List;

public class CarTrendConnector extends ELM327Connector {

    private static final Logger logger = Logger.getLogger(CarTrendConnector.class);
    private int requiredCount;
    private boolean protocolFound;

    @Override
    public List<CommonCommand> getInitializationCommands() {
        List<CommonCommand> result = new ArrayList<CommonCommand>();
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

        //all except protocol commands
        requiredCount = 4;

        return result;
    }

    @Override
    public boolean supportsDevice(String deviceName) {
        return deviceName.toLowerCase().contains("cartrend");
    }

    @Override
    public void processInitializationCommand(CommonCommand cmd) {

        if (cmd instanceof GenericCommand) {
            logger.info("Processing GenericCommand: "+cmd.getCommandName()+"; Response: "+((GenericCommand) cmd).getStringResult());
        }

        if (cmd instanceof ProtocolCommand) {
            if (((ProtocolCommand) cmd).isSuccessful()) {
                protocolFound = true;
            }
        } else if (cmd instanceof GenericCommand) {
            if (((GenericCommand) cmd).isSuccessful()) {
                succesfulCount++;
            }
        } else {
            super.processInitializationCommand(cmd);
        }

        logger.info(String.format("Connection state: %s/%s success; protocol found? %s", succesfulCount, requiredCount, protocolFound));

    }

    @Override
    public ConnectionState connectionState() {
        if (this.protocolFound && this.succesfulCount >= this.requiredCount) {
            return ConnectionState.CONNECTED;
        }
        return ConnectionState.DISCONNECTED;
    }

    private static class ConfigCommand extends GenericCommand {

        protected ConfigCommand(String content) {
            super(content);
        }

        @Override
        public boolean isSuccessful() {
            String content = getStringResult();
            return content.contains("OK");
        }
    }

    private class ProtocolCommand extends GenericCommand {

        protected ProtocolCommand(String id) {
            super("@P".concat(id));
        }

        @Override
        public byte[] getOutgoingBytes() {
            return CarTrendConnector.this.protocolFound ? new byte[0] : super.getOutgoingBytes();
        }

        @Override
        public boolean responseAlwaysRequired() {
            return true;
        }

        @Override
        public boolean isSuccessful() {
            String content = getStringResult();
            return content.contains("ONNECTED");
        }
    }

    private static class EmptyCommand extends GenericCommand {

        protected EmptyCommand() {
            super("");
        }

        @Override
        public byte[] getOutgoingBytes() {
            return new byte[0];
        }

        @Override
        public boolean responseAlwaysRequired() {
            return true;
        }

        @Override
        public boolean isSuccessful() {
            String content = getStringResult();
            return content.contains("?");
        }
    }

    private static class IdentifyCommand extends GenericCommand {

        protected IdentifyCommand() {
            super("@");
        }

        @Override
        public boolean isSuccessful() {
            String content = getStringResult();
            return content.contains("MS4200");
        }
    }

    private static class GenericCommand extends StringResultCommand {

        private final String name;

        protected GenericCommand(String content) {
            super(content);
            this.name = content;
        }

        @Override
        public boolean responseAlwaysRequired() {
            return true;
        }

        @Override
        public String getCommandName() {
            return name;
        }

        @Override
        public byte[] getOutgoingBytes() {

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                logger.warn(e.getMessage(), e);
            }

            return super.getOutgoingBytes();
        }
    }

}
