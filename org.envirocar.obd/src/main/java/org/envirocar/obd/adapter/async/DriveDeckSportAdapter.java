/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.obd.adapter.async;

import android.util.Base64;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.PIDSupported;
import org.envirocar.obd.commands.request.BasicCommand;
import org.envirocar.obd.commands.response.DataResponse;
import org.envirocar.obd.exception.AdapterSearchingException;
import org.envirocar.obd.exception.InvalidCommandResponseException;
import org.envirocar.obd.exception.NoDataReceivedException;
import org.envirocar.obd.exception.UnmatchedResponseException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;

public class DriveDeckSportAdapter extends AsyncAdapter {

    private static final Logger logger = Logger.getLogger(DriveDeckSportAdapter.class);

    private static enum Protocol {
        CAN11500, CAN11250, CAN29500, CAN29250, KWP_SLOW, KWP_FAST, ISO9141
    }

    private static final char CARRIAGE_RETURN = '\r';
    private static final char END_OF_LINE_RESPONSE = '>';
    private static final char RESPONSE_PREFIX_CHAR = 'B';
    private static final char CYCLIC_TOKEN_SEPARATOR_CHAR = '<';
    private static final long SEND_CYCLIC_COMMAND_DELTA = 10000;

    private Protocol protocol;
    private String vin;
    private CycleCommand cycleCommand;
    public long lastCyclicCommandSent;
    private Set<String> loggedPids = new HashSet<>();
    private org.envirocar.obd.commands.response.ResponseParser parser = new org.envirocar.obd.commands.response.ResponseParser();
    private Queue<BasicCommand> pendingCommands;


    public DriveDeckSportAdapter() {
        super(CARRIAGE_RETURN, END_OF_LINE_RESPONSE);
        createCommand();
    }

    private void createCommand() {
        List<CycleCommand.DriveDeckPID> pidList = new ArrayList<>();
        pidList.add(CycleCommand.DriveDeckPID.SPEED);
        pidList.add(CycleCommand.DriveDeckPID.MAF);
        pidList.add(CycleCommand.DriveDeckPID.RPM);
        pidList.add(CycleCommand.DriveDeckPID.IAP);
        pidList.add(CycleCommand.DriveDeckPID.IAT);
        pidList.add(CycleCommand.DriveDeckPID.ENGINE_LOAD);
        pidList.add(CycleCommand.DriveDeckPID.TPS);
        pidList.add(CycleCommand.DriveDeckPID.O2_LAMBDA_PROBE_1_VOLTAGE);
        pidList.add(CycleCommand.DriveDeckPID.O2_LAMBDA_PROBE_1_CURRENT);
        this.cycleCommand = new CycleCommand(pidList);

        this.pendingCommands = new ArrayDeque<>();
        this.pendingCommands.offer(new CarriageReturnCommand());
        this.pendingCommands.offer(this.cycleCommand);
    }

    private void processDiscoveredControlUnits(String substring) {
        logger.info("Discovered CUs... ");
    }

    //TODO: not used atm, implement!
    protected void processSupportedPID(byte[] bytes, int start, int count) throws InvalidCommandResponseException, NoDataReceivedException,
            UnmatchedResponseException, AdapterSearchingException {

        String group = new String(bytes, start + 6, 2);

        if (group.equals("00")) {
            /*
             * this is the first group containing the PIDs of major interest
			 */
            PIDSupported pidCmd = new PIDSupported();
            byte[] rawBytes = new byte[12];
            rawBytes[0] = '4';
            rawBytes[1] = '1';
            rawBytes[2] = (byte) pidCmd.getGroup().charAt(0);
            rawBytes[3] = (byte) pidCmd.getGroup().charAt(1);
            int target = 4;
            String hexTmp;
            for (int i = 9; i < 14; i++) {
                if (i == 11) continue;
                hexTmp = oneByteToHex(bytes[i + start]);
                rawBytes[target++] = (byte) hexTmp.charAt(0);
                rawBytes[target++] = (byte) hexTmp.charAt(1);
            }

            pidCmd.parseRawData(rawBytes);

            logger.info("Supported PIDs: "+ Arrays.toString(pidCmd.getSupportedPIDs().toArray()));
        }
    }

    private String oneByteToHex(byte b) {
        String result = Integer.toString(b & 0xff, 16).toUpperCase(Locale.US);
        if (result.length() == 1) result = "0".concat(result);
        return result;
    }

    private void processVIN(String vinInt) {
        this.vin = vinInt;
        logger.info("VIN is: " + this.vin);
    }

    private void determineProtocol(String protocolInt) {
        if (protocolInt == null || protocolInt.trim().isEmpty()) {
            return;
        }

        int prot;
        try {
            prot = Integer.parseInt(protocolInt);
        } catch (NumberFormatException e) {
            logger.warn("NFE: " + e.getMessage());
            return;
        }

        switch (prot) {
            case 1:
                protocol = Protocol.CAN11500;
                break;
            case 2:
                protocol = Protocol.CAN11250;
                break;
            case 3:
                protocol = Protocol.CAN29500;
                break;
            case 4:
                protocol = Protocol.CAN29250;
                break;
            case 5:
                protocol = Protocol.KWP_SLOW;
                break;
            case 6:
                protocol = Protocol.KWP_FAST;
                break;
            case 7:
                protocol = Protocol.ISO9141;
                break;
            default:
                return;
        }

        logger.info("Protocol is: " + protocol.toString());
    }


    @Override
    public boolean supportsDevice(String deviceName) {
        return deviceName != null && deviceName.toLowerCase().contains("drivedeck") && deviceName.toLowerCase().contains("w4");
    }

    @Override
    public boolean hasVerifiedConnection() {
        return protocol != null;
    }


    private DataResponse parsePIDResponse(String pid, byte[] rawBytes) throws InvalidCommandResponseException, NoDataReceivedException,
            UnmatchedResponseException, AdapterSearchingException {

		/*
         * resulting HEX values are 0x0d additive to the
		 * default PIDs of OBD. e.g. RPM = 0x19 = 0x0c + 0x0d
		 */
        PID result = null;
        if (pid.equals("41")) {
            //Speed
            result = PID.SPEED;
        } else if (pid.equals("42")) {
            //MAF
            result = PID.MAF;
        } else if (pid.equals("52")) {
            //IAP
            result = PID.INTAKE_AIR_TEMP;
        } else if (pid.equals("49")) {
            //IAT
            result = PID.INTAKE_MAP;
        } else if (pid.equals("40") || pid.equals("51")) {
            //RPM
            result = PID.RPM;
        } else if (pid.equals("4D")) {
            //TODO the current manual does not provide info on how to
            //determine which probe value is returned.
//            result = O2LambdaProbe.fromPIDEnum(org.envirocar.obd.commands.PIDUtil.PID
//                    .O2_LAMBDA_PROBE_1_VOLTAGE);
        } else if (pid.equals("DUMMY")) {
            //TODO: implement Engine Load, TPS, others
        }

        oneTimePIDLog(pid, rawBytes);

        if (result != null) {
            byte[] rawData = createRawData(rawBytes, result.getHexadecimalRepresentation());
            DataResponse parsed = parser.parse(rawData);
            return parsed;
        }

        return null;
    }

    private void oneTimePIDLog(String pid, byte[] rawBytes) {
        if (pid == null || rawBytes == null || rawBytes.length == 0)
            return;

        if (!loggedPids.contains(pid)) {
            logger.info("First response for PID: " + pid + "; Base64: " +
                    Base64.encodeToString(rawBytes, Base64.DEFAULT));
            loggedPids.add(pid);
        }
    }

    private byte[] createRawData(byte[] rawBytes, String type) {
        byte[] result = new byte[4 + rawBytes.length * 2];
        byte[] typeBytes = type.getBytes();
        result[0] = (byte) '4';
        result[1] = (byte) '1';
        result[2] = typeBytes[0];
        result[3] = typeBytes[1];
        for (int i = 0; i < rawBytes.length; i++) {
            String hex = oneByteToHex(rawBytes[i]);
            result[(i * 2) + 4] = (byte) hex.charAt(0);
            result[(i * 2) + 1 + 4] = (byte) hex.charAt(1);
        }
        return result;
    }


    @Override
    protected BasicCommand pollNextCommand() {
        BasicCommand result = this.pendingCommands.poll();

        /**
         * send the cycle command once in a while to keep the connection alive
         * TODO: is this required?
         */
        if (result == null && System.currentTimeMillis() - lastCyclicCommandSent > SEND_CYCLIC_COMMAND_DELTA) {
            lastCyclicCommandSent = System.currentTimeMillis();
            result = this.cycleCommand;
        }
        else {
            /**
             * this is probably an init command, wait a bit
             */
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                logger.warn(e.getMessage(), e);
            }
        }

        return result;
    }

    @Override
    protected DataResponse processResponse(byte[] bytes) throws InvalidCommandResponseException, NoDataReceivedException, UnmatchedResponseException, AdapterSearchingException {

        if (bytes.length <= 0) {
            return null;
        }

        char type = (char) bytes[0];

        if (type == RESPONSE_PREFIX_CHAR) {
            if ((char) bytes[4] == CYCLIC_TOKEN_SEPARATOR_CHAR) {
                return null;
            }

            String pid = new String(bytes, 1, 2);

				/*
                 * METADATA Stuff
				 */
            if (pid.equals("14")) {
                logger.debug("Status: CONNECTING");
            } else if (pid.equals("15")) {
                processVIN(new String(bytes, 3, bytes.length - 3));
            } else if (pid.equals("70")) {
					/*
					 * short term fix for #192: disable
					 */
                //					processSupportedPID(bytes, start, count);
            } else if (pid.equals("71")) {
                processDiscoveredControlUnits(new String(bytes, 3, bytes.length - 3));
            } else if (pid.equals("31")) {
                // engine on
                logger.debug("Engine: On");
            } else if (pid.equals("32")) {
                // engine off (= RPM < 500)
                logger.debug("Engine: Off");
            } else {
                if (bytes.length < 6) {
                    throw new NoDataReceivedException("the response did only contain " + bytes.length + " bytes. For PID " +
                            "responses 6 are minimum");
                }

					/*
					 * A PID response
					 */
                logger.verbose("Processing PID Response:" + pid);

                byte[] pidResponseValue = new byte[2];
                int target;
                for (int i =  4; i <= bytes.length; i++) {
                    target = i - 4;
                    if (target >= pidResponseValue.length) {
                        break;
                    }

                    if ((char) bytes[i] == CYCLIC_TOKEN_SEPARATOR_CHAR) {
                        break;
                    }

                    pidResponseValue[target] = bytes[i];
                }

                DataResponse result = parsePIDResponse(pid, pidResponseValue);

                return result;
            }

        } else if (type == 'C') {
            determineProtocol(new String(bytes,  1, bytes.length - 1));
        }

        return null;
    }

}
