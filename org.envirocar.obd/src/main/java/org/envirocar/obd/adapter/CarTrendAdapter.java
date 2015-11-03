package org.envirocar.obd.adapter;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.commands.BasicCommand;
import org.envirocar.obd.commands.PIDCommand;
import org.envirocar.obd.protocol.exception.AdapterFailedException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

public class CarTrendAdapter extends SequentialAdapter {

    private static final Logger logger = Logger.getLogger(CarTrendAdapter.class);
    private static final int MAX_METADATA_COUNT = 25;
    private int requiredCount;
    private boolean protocolFound;
    private boolean identifySuccess;
    private int metadataResponseCount;
    private boolean connectionEstablished;
    private int dataStartPosition = -1;
    private Queue<BasicCommand> initializeRing;

    @Override
    protected BasicCommand pollNextInitializationCommand() {
        if (this.initializeRing == null) {
            this.initializeRing = new ArrayDeque<>();
            this.initializeRing.add(new EmptyCommand());
            this.initializeRing.add(new IdentifyCommand());
            this.initializeRing.add(new EmptyCommand());
            this.initializeRing.add(new ProtocolCommand("S"));
            this.initializeRing.add(new ProtocolCommand("1"));
            this.initializeRing.add(new ProtocolCommand("2"));
            this.initializeRing.add(new ProtocolCommand("3"));
            this.initializeRing.add(new ProtocolCommand("4"));
            this.initializeRing.add(new ProtocolCommand("5"));
            this.initializeRing.add(new ProtocolCommand("6"));
            this.initializeRing.add(new ConfigCommand("@E0"));
            this.initializeRing.add(new ConfigCommand("@H0"));
        }

        BasicCommand next = this.initializeRing.poll();

        if (next instanceof ProtocolCommand) {
            /**
             * re-add protocol selection
             */
            this.initializeRing.offer(next);
        }

        return next;
    }

    @Override
    protected List<PIDCommand> providePendingCommands() {
        return super.defaultCycleCommands();
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
        if (dataStartPosition == -1) {
            String data = new String(bytes);
            /**
             * search for "41" (= status ok)
             */
            dataStartPosition = data.indexOf("41");
        }

        if (dataStartPosition < bytes.length) {
            return Arrays.copyOfRange(bytes, dataStartPosition, bytes.length);
        }
        else {
            return bytes;
        }
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

    private static class GenericCommand implements BasicCommand {

        private final String name;

        protected GenericCommand(String content) {
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

        @Override
        public boolean awaitsResults() {
            return true;
        }
    }

}
