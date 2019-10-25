/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.obd.commands.response;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.PIDUtil;
import org.envirocar.obd.commands.response.entity.EngineLoadResponse;
import org.envirocar.obd.commands.response.entity.EngineRPMResponse;
import org.envirocar.obd.commands.response.entity.FuelPressureResponse;
import org.envirocar.obd.commands.response.entity.GenericDataResponse;
import org.envirocar.obd.commands.response.entity.IntakeAirTemperatureResponse;
import org.envirocar.obd.commands.response.entity.IntakeManifoldAbsolutePressureResponse;
import org.envirocar.obd.commands.response.entity.LambdaProbeCurrentResponse;
import org.envirocar.obd.commands.response.entity.LambdaProbeVoltageResponse;
import org.envirocar.obd.commands.response.entity.LongTermFuelTrimResponse;
import org.envirocar.obd.commands.response.entity.MAFResponse;
import org.envirocar.obd.commands.response.entity.ShortTermFuelTrimResponse;
import org.envirocar.obd.commands.response.entity.SpeedResponse;
import org.envirocar.obd.commands.response.entity.ThrottlePositionResponse;
import org.envirocar.obd.exception.AdapterSearchingException;
import org.envirocar.obd.exception.InvalidCommandResponseException;
import org.envirocar.obd.exception.NoDataReceivedException;
import org.envirocar.obd.exception.UnmatchedResponseException;

public class ResponseParser {

    private static final Logger LOGGER = Logger.getLogger(ResponseParser.class);

    private static final CharSequence SEARCHING = "SEARCHING";
    private static final CharSequence STOPPED = "STOPPED";
    private static final CharSequence NO_DATA = "NODATA";
    public static final String STATUS_OK = "41";

    public ResponseParser() {

    }

    public DataResponse parse(byte[] data) throws AdapterSearchingException, NoDataReceivedException,
            InvalidCommandResponseException, UnmatchedResponseException {

        /**
         * we received a char array as hexadecimal -->
         * two chars represent one byte
         */
        int index = 0;
        int length = 2;

        String dataString = new String(data);

        //cartrend: 7E803410D00AAAAAAAA
        //= 410D00AAAAAAAA
//        if (dataString.startsWith("7E803")) {
//            dataString = dataString.substring(5, dataString.length());
//        }

        if (isSearching(dataString)) {
            throw new AdapterSearchingException();
        } else if (isNoDataCommand(dataString)) {
            throw new NoDataReceivedException("NODATA was received");
        }

        int[] buffer = new int[data.length / 2];
        boolean error = false;

        PID pid = null;
        while (index + length <= data.length) {
            String tmp = new String(data, index, length);

            // this is the status
            if (index == 0) {
                if (!tmp.equals(STATUS_OK)) {
                    error = true;
                }
            }
            // this is the ID byte
            else if (index == 2) {
                pid = PIDUtil.fromString(tmp);
                if (error || pid == null) {
                    throw new InvalidCommandResponseException(pid == null ? tmp : pid.toString());
                }
            } else {
                if (error || pid == null){
                    throw new InvalidCommandResponseException(pid == null ? tmp : pid.toString());
                }

                /*
                 * this is a hex number
                 */
                buffer[index / 2] = Integer.parseInt(tmp, 16);
                if (buffer[index / 2] < 0) {
                    throw new InvalidCommandResponseException(pid.toString());
                }
            }

            index += length;
        }

        try {
            return createDataResponse(pid, buffer, data);
        } catch (Exception e){
            throw new UnmatchedResponseException(e);
        }
    }

    private DataResponse createDataResponse(PID pid, int[] processedData, byte[] rawData) {
        switch (pid) {
//            case FUEL_SYSTEM_STATUS:
//                return FuelSystemStatusResponse.fromRawData(rawData);
            case CALCULATED_ENGINE_LOAD:
                return new EngineLoadResponse((processedData[2] * 100.0f) / 255.0f);
            case FUEL_PRESSURE:
                return new FuelPressureResponse(processedData[2] * 3);
            case INTAKE_MAP:
                return new IntakeManifoldAbsolutePressureResponse(processedData[2]);
            case RPM:
                return new EngineRPMResponse((processedData[2] * 256 + processedData[3]) / 4);
            case SPEED:
                return new SpeedResponse(processedData[2]);
            case INTAKE_AIR_TEMP:
                return new IntakeAirTemperatureResponse(processedData[2] - 40);
            case MAF:
                return new MAFResponse((processedData[2] * 256 + processedData[3]) / 100.0f);
            case TPS:
                return new ThrottlePositionResponse((processedData[2] * 100) / 255);
            case SHORT_TERM_FUEL_TRIM_BANK_1:
                return new ShortTermFuelTrimResponse((processedData[2] - 128) * (100d / 128d), 1);
            case LONG_TERM_FUEL_TRIM_BANK_1:
                return new LongTermFuelTrimResponse((processedData[2] - 128) * (100d / 128d), 1);
            case O2_LAMBDA_PROBE_1_VOLTAGE:
            case O2_LAMBDA_PROBE_2_VOLTAGE:
            case O2_LAMBDA_PROBE_3_VOLTAGE:
            case O2_LAMBDA_PROBE_4_VOLTAGE:
            case O2_LAMBDA_PROBE_5_VOLTAGE:
            case O2_LAMBDA_PROBE_6_VOLTAGE:
            case O2_LAMBDA_PROBE_7_VOLTAGE:
            case O2_LAMBDA_PROBE_8_VOLTAGE:
                LambdaProbeVoltageResponse lambda = new LambdaProbeVoltageResponse(
                        ((processedData[4] * 256d) + processedData[5]) / 8192d,
                        ((processedData[2] * 256d) + processedData[3]) / 32768d);
                return lambda;
            case O2_LAMBDA_PROBE_1_CURRENT:
            case O2_LAMBDA_PROBE_2_CURRENT:
            case O2_LAMBDA_PROBE_3_CURRENT:
            case O2_LAMBDA_PROBE_4_CURRENT:
            case O2_LAMBDA_PROBE_5_CURRENT:
            case O2_LAMBDA_PROBE_6_CURRENT:
            case O2_LAMBDA_PROBE_7_CURRENT:
            case O2_LAMBDA_PROBE_8_CURRENT:
                return new LambdaProbeCurrentResponse(
                        ((processedData[4] * 256d) + processedData[5]) / 256d - 128,
                        ((processedData[2] * 256d) + processedData[3]) / 32768d);
        }

        return new GenericDataResponse(pid, processedData, rawData);
    }

    private boolean isSearching(String dataString) {
        return dataString.contains(SEARCHING) || dataString.contains(STOPPED);
    }

    private boolean isNoDataCommand(String dataString) {
        return dataString == null || dataString.contains(NO_DATA);
    }


}
