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

import android.util.Base64;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.commands.CycleCommandProfile;
import org.envirocar.obd.commands.request.BasicCommand;
import org.envirocar.obd.commands.request.PIDCommand;
import org.envirocar.obd.exception.AdapterFailedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

public class CarTrendAdapter extends SyncAdapter {

    private static final Logger logger = Logger.getLogger(CarTrendAdapter.class);
    private static final int MAX_METADATA_COUNT = 25;
    private static final byte[] LOG_RESPONSE_SEPARATOR = "§|§".getBytes();
    private static final long EXPECTED_INIT_PERIOD = 35000;

    private int requiredCount;
    private boolean protocolFound;
    private boolean identifySuccess;
    private int metadataResponseCount;
    private boolean connectionEstablished;
    private int dataStartPosition = -1;
    private Queue<BasicCommand> initializeRing;
    private int ringSize;
    private int initialCount;
    private ByteArrayOutputStream initialPhaseResponseLog = new ByteArrayOutputStream();

    private String matchedProtocol = null;

    public CarTrendAdapter(CycleCommandProfile cmp) {
        super(cmp);
    }

    @Override
    protected BasicCommand pollNextInitializationCommand() {
        if (this.initializeRing == null) {
            this.initializeRing = new ArrayDeque<>();
//            this.initializeRing.add(new EmptyCommand());
            this.initializeRing.add(new IdentifyCommand());
//            this.initializeRing.add(new EmptyCommand());
            this.initializeRing.add(new ProtocolCommand("S"));
            this.initializeRing.add(new ProtocolCommand("1"));
            this.initializeRing.add(new ProtocolCommand("2"));
            this.initializeRing.add(new ProtocolCommand("3"));
            this.initializeRing.add(new ProtocolCommand("4"));
            this.initializeRing.add(new ProtocolCommand("5"));
            this.initializeRing.add(new ProtocolCommand("6"));
            this.initializeRing.add(new ConfigCommand("@E0"));
            this.initializeRing.add(new ConfigCommand("@H0"));

            this.ringSize = this.initializeRing.size();
        }

        if (++initialCount == ringSize) {
            logger.info("One cycle of config commands sent, trying another round");
            ringSize = this.initializeRing.size();
            initialCount = 0;
        }

        BasicCommand next = this.initializeRing.poll();

        if (next instanceof ProtocolCommand && !this.connectionEstablished) {
            /**
             * re-add protocol selection
             */
            this.initializeRing.offer(next);
        }
        else if (next instanceof ProtocolCommand && this.matchedProtocol != null) {
            // skip other protocols as we might set an unsupported one
            // this relies on the order in the ring. all protocol commands must be
            // in a block
            do {
                next = this.initializeRing.poll();
            } while (next instanceof ProtocolCommand);
        }

        logger.debug("Ring size: " + this.initializeRing.size());
        return next;
    }

    @Override
    protected List<PIDCommand> providePendingCommands() {
        return super.defaultCycleCommands();
    }

    @Override
    protected boolean analyzeMetadataResponse(byte[] response, BasicCommand sentCommand) throws AdapterFailedException {
        try {
            initialPhaseResponseLog.write(response);
            initialPhaseResponseLog.write(LOG_RESPONSE_SEPARATOR);
        } catch (IOException e) {
            logger.warn("Error writing metadata response to initial log", e);
        }

        logger.info("Parsing meta response: "+ Base64.encodeToString(response, Base64.DEFAULT)+
                "; sentCommand="+Base64.encodeToString(sentCommand.getOutputBytes(), Base64.DEFAULT));

        if (response == null || response.length == 0) {
            return false;
        }

        String asString = new String(response).toLowerCase();

        if (asString.contains("ms4200")) {
            identifySuccess = true;
            logger.info("Received Identity response. This should be a CarTrend: "+asString);
        }

        if (identifySuccess && asString.contains("onnected")) {
            this.connectionEstablished = true;
            if (sentCommand instanceof GenericCommand) {
                GenericCommand gen = (GenericCommand) sentCommand;
                this.matchedProtocol = gen.getName();
                logger.debug(String.format("Connected on Protocol %s. Adapter responded '%s'",
                    new String(gen.getName()), new String(response)));
            }
            
        }

        if (sentCommand instanceof ProtocolCommand && asString.contains("error") && asString.contains("unable")) {
            /**
             * the adapter understood the command but could not connect --> it is still a certified
             * connection as no '?' was returned
             */
            identifySuccess = true;
        }

        if (++this.metadataResponseCount > MAX_METADATA_COUNT && !this.connectionEstablished) {
            throw new AdapterFailedException("Too many tries. Could not establish data link");
        }

        return this.connectionEstablished;
    }

    @Override
    protected byte[] preProcess(byte[] bytes) throws AdapterFailedException {
        if (dataStartPosition == -1) {
            String data = new String(bytes);
            /**
             * search for "41" (= status ok)
             */
            dataStartPosition = data.indexOf("41");
            logger.info(String.format("Identified start position %s by response '%s'",
                    dataStartPosition, new String(bytes)));

            if (dataStartPosition == -1) {
                //still -1, throw exception
                throw new AdapterFailedException("Could not determine start position of CarTrend response");
            }
        }

        if (dataStartPosition != -1 && dataStartPosition < bytes.length) {
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

    @Override
    public boolean hasCertifiedConnection() {
        return this.identifySuccess;
    }

    @Override
    public long getExpectedInitPeriod() {
        return EXPECTED_INIT_PERIOD;
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

        @Override
        public String toString() {
            return this.name;
        }

        public String getName() {
            return this.name;
        }
    }

    @Override
    public String getStateMessage() {
        return String.format("All initial responses: %s", Base64.encodeToString(this.initialPhaseResponseLog.toByteArray(), Base64.DEFAULT));
    }
}
