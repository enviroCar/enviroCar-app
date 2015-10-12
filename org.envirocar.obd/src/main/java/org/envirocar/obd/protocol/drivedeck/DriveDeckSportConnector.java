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
package org.envirocar.obd.protocol.drivedeck;

import android.util.Base64;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.commands.CommonCommand;
import org.envirocar.obd.commands.CommonCommand.CommonCommandState;
import org.envirocar.obd.commands.IntakePressure;
import org.envirocar.obd.commands.IntakeTemperature;
import org.envirocar.obd.commands.MAF;
import org.envirocar.obd.commands.NumberResultCommand;
import org.envirocar.obd.commands.O2LambdaProbe;
import org.envirocar.obd.commands.PIDSupported;
import org.envirocar.obd.commands.RPM;
import org.envirocar.obd.commands.Speed;
import org.envirocar.obd.protocol.AbstractAsynchronousConnector;
import org.envirocar.obd.protocol.OBDConnector;
import org.envirocar.obd.protocol.ResponseParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DriveDeckSportConnector extends AbstractAsynchronousConnector {

    private static final Logger logger = Logger.getLogger(DriveDeckSportConnector.class);
    private static final char CARRIAGE_RETURN = '\r';
    static final char END_OF_LINE_RESPONSE = '>';
    private static final long SEND_CYCLIC_COMMAND_DELTA = 2500;
    private Protocol protocol;
    private String vin;
    private CycleCommand cycleCommand;
    private ResponseParser responseParser = new LocalResponseParser();
    private OBDConnector.ConnectionState state = OBDConnector.ConnectionState.DISCONNECTED;
    public long lastResult;
    private Set<String> loggedPids = new HashSet<String>();

    private int mLastVal = 0;

    private static enum Protocol {
        CAN11500, CAN11250, CAN29500, CAN29250, KWP_SLOW, KWP_FAST, ISO9141
    }

    public DriveDeckSportConnector() {
        createCycleCommand();
        logger.info("Static CycleCommand: " + new String(cycleCommand.getOutgoingBytes()));
    }

    private void createCycleCommand() {
        List<CycleCommand.PID> pidList = new ArrayList<CycleCommand.PID>();
        pidList.add(CycleCommand.PID.SPEED);
        pidList.add(CycleCommand.PID.MAF);
        pidList.add(CycleCommand.PID.RPM);
        pidList.add(CycleCommand.PID.IAP);
        pidList.add(CycleCommand.PID.IAT);
        //		pidList.add(PID.SHORT_TERM_FUEL_TRIM);
        //		pidList.add(PID.LONG_TERM_FUEL_TRIM);
        pidList.add(CycleCommand.PID.O2_LAMBDA_PROBE_1_VOLTAGE);
        pidList.add(CycleCommand.PID.O2_LAMBDA_PROBE_1_CURRENT);
        this.cycleCommand = new CycleCommand(pidList);
    }

    @Override
    public OBDConnector.ConnectionState connectionState() {
        return this.state;
    }

    private void processDiscoveredControlUnits(String substring) {
        logger.info("Discovered CUs... ");
    }

    protected void processSupportedPID(byte[] bytes, int start, int count) {
        String group = new String(bytes, start + 6, 2);

        if (group.equals("00")) {
            /*
             * this is the first group containing the PIDs of major interest
			 */
            PIDSupported pidCmd = new PIDSupported();
            byte[] rawBytes = new byte[12];
            rawBytes[0] = '4';
            rawBytes[1] = '1';
            rawBytes[2] = (byte) pidCmd.getResponseTypeID().charAt(0);
            rawBytes[3] = (byte) pidCmd.getResponseTypeID().charAt(1);
            int target = 4;
            String hexTmp;
            for (int i = 9; i < 14; i++) {
                if (i == 11) continue;
                hexTmp = oneByteToHex(bytes[i + start]);
                rawBytes[target++] = (byte) hexTmp.charAt(0);
                rawBytes[target++] = (byte) hexTmp.charAt(1);
            }

            pidCmd.setRawData(rawBytes);
            pidCmd.parseRawData();

            if (pidCmd.getCommandState() == CommonCommandState.FINISHED) {
                logger.info(pidCmd.getSupportedPIDs().toArray().toString());
            }
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

        updateConnectionState();
    }

    private void updateConnectionState() {
        if (state == ConnectionState.VERIFIED) {
            return;
        }

        if (protocol != null || vin != null) {
            state = ConnectionState.CONNECTED;
        }
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

        updateConnectionState();
    }


    @Override
    public boolean supportsDevice(String deviceName) {
        return deviceName.contains("DRIVEDECK") && deviceName.contains("W4");
    }


    private CommonCommand parsePIDResponse(String pid,
                                           byte[] rawBytes, long now, byte[] debugArray) {

		/*
         * resulting HEX values are 0x0d additive to the
		 * default PIDs of OBD. e.g. RPM = 0x19 = 0x0c + 0x0d
		 */
        CommonCommand result = null;
        if (pid.equals("41")) {
            //Speed
            result = new Speed();
        } else if (pid.equals("42")) {
            //MAF
            result = new MAF();
        } else if (pid.equals("52")) {
            //IAP
            result = new IntakeTemperature();
        } else if (pid.equals("49")) {
            //IAT
            result = new IntakePressure();
        } else if (pid.equals("40") || pid.equals("51")) {
            //RPM
            result = new RPM();
        } else if (pid.equals("4D")) {
            //TODO the current manual does not provide info on how to
            //determine which probe value is returned.
            result = O2LambdaProbe.fromPIDEnum(org.envirocar.obd.commands.PIDUtil.PID
                    .O2_LAMBDA_PROBE_1_VOLTAGE);
        }

        oneTimePIDLog(pid, rawBytes);

        if (result != null) {
            byte[] rawData = createRawData(rawBytes, result.getResponseTypeID());
            result.setRawData(rawData);
            result.parseRawData();

            if (result instanceof NumberResultCommand && result instanceof Speed) {
                NumberResultCommand numberResult = (NumberResultCommand) result;
                if (numberResult.getNumberResult() != null) {
                    int val = numberResult.getNumberResult().intValue();
                    if (val - mLastVal > 49) {
                        logger.warn(String.format(
                                "Received a speed value of %s. this is probably an " +
                                        "erroneous response. Base64 encoded value: %s",
                                Integer.toString(val), "" + Arrays.toString(Base64.encode
                                        (debugArray, Base64.DEFAULT))));
                    }
                    mLastVal = val;
                }
            }


            if (result.getCommandState() == CommonCommandState.EXECUTION_ERROR ||
                    result.getCommandState() == CommonCommandState.SEARCHING) {
                return null;
            }

            result.setCommandState(CommonCommandState.FINISHED);
            result.setResultTime(now);
            this.state = ConnectionState.VERIFIED;
        }

        return result;
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
    protected List<CommonCommand> getRequestCommands() {
        if (System.currentTimeMillis() - lastResult > SEND_CYCLIC_COMMAND_DELTA) {
            return Collections.singletonList((CommonCommand) cycleCommand);
        } else {
            return Collections.emptyList();
        }
    }


    @Override
    protected char getRequestEndOfLine() {
        return CARRIAGE_RETURN;
    }


    @Override
    protected ResponseParser getResponseParser() {
        return responseParser;
    }

    @Override
    protected List<CommonCommand> getInitializationCommands() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            logger.warn(e.getMessage(), e);
        }

        return Collections.singletonList((CommonCommand) new CarriageReturnCommand());
    }

    @Override
    public int getMaximumTriesForInitialization() {
        return 15;
    }


    public class LocalResponseParser implements ResponseParser {
        @Override
        public CommonCommand processResponse(byte[] bytes, int start, int count) {
            if (count <= 0) return null;

            char type = (char) bytes[start + 0];

            if (type == CycleCommand.RESPONSE_PREFIX_CHAR) {
                if ((char) bytes[start + 4] == CycleCommand.TOKEN_SEPARATOR_CHAR) return null;

                String pid = new String(bytes, start + 1, 2);

				/*
				 * METADATA Stuff
				 */
                if (pid.equals("14")) {
                    logger.debug("Status: CONNECTING");
                } else if (pid.equals("15")) {
                    processVIN(new String(bytes, start + 3, count - 3));
                } else if (pid.equals("70")) {
					/*
					 * short term fix for #192: disable
					 */
                    //					processSupportedPID(bytes, start, count);
                } else if (pid.equals("71")) {
                    processDiscoveredControlUnits(new String(bytes, start + 3, count - 3));
                } else if (pid.equals("31")) {
                    // engine on
                    logger.debug("Engine: On");
                } else if (pid.equals("32")) {
                    // engine off (= RPM < 500)
                    logger.debug("Engine: Off");
                } else {
					/*
					 * A PID response
					 */
                    long now = System.currentTimeMillis();
                    logger.verbose("Processing PID Response:" + pid);

                    byte[] pidResponseValue = new byte[2];
                    int target;
                    for (int i = start + 4; i <= count + start; i++) {
                        target = i - (start + 4);
                        if (target >= pidResponseValue.length)
                            break;

                        if ((char) bytes[i] == CycleCommand.TOKEN_SEPARATOR_CHAR)
                            break;

                        pidResponseValue[target] = bytes[i];
                    }

                    //TODO remove last arg (copy array) as its for deugging
                    CommonCommand result = parsePIDResponse(pid, pidResponseValue, now, Arrays
                            .copyOfRange(bytes, start, count));

                    if (result != null) {
                        lastResult = now;
                    }

                    return result;
                }

            } else if (type == 'C') {
                determineProtocol(new String(bytes, start + 1, count - 1));
            }

            return null;
        }

        @Override
        public char getEndOfLine() {
            return END_OF_LINE_RESPONSE;
        }


    }


    @Override
    protected long getSleepTimeBetweenCommands() {
        return 0;
    }

    @Override
    public long getPreferredRequestPeriod() {
        return 500;
    }

}
