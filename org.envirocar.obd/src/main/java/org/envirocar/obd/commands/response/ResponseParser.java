package org.envirocar.obd.commands.response;

import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.PIDUtil;
import org.envirocar.obd.commands.exception.AdapterSearchingException;
import org.envirocar.obd.commands.exception.NoDataReceivedException;
import org.envirocar.obd.commands.response.entity.EngineLoadResponse;
import org.envirocar.obd.commands.response.entity.EngineRPMResponse;
import org.envirocar.obd.commands.response.entity.FuelPressureResponse;
import org.envirocar.obd.commands.response.entity.IntakeAirPressureResponse;
import org.envirocar.obd.commands.response.entity.IntakeManifoldAbsolutePressureResponse;
import org.envirocar.obd.commands.response.entity.LambdaProbeCurrentResponse;
import org.envirocar.obd.commands.response.entity.LambdaProbeVoltageResponse;
import org.envirocar.obd.commands.response.entity.MAFResponse;
import org.envirocar.obd.commands.response.entity.SpeedResponse;
import org.envirocar.obd.commands.response.entity.ThrottlePositionResponse;
import org.envirocar.obd.protocol.exception.InvalidCommandResponseException;

public class ResponseParser {

    private static final CharSequence SEARCHING = "SEARCHING";
    private static final CharSequence STOPPED = "STOPPED";
    private static final CharSequence NODATA = "NODATA";
    private static final String STATUS_OK = "41";

    public ResponseParser() {

    }

    public DataResponse parse(byte[] data) throws AdapterSearchingException, NoDataReceivedException,
            InvalidCommandResponseException {

        /**
         * we received a char array as hexadecimal -->
         * two chars represent one byte
         */
        int index = 0;
        int length = 2;

        String dataString = new String(data);

        //cartrend: 7E803410D00AAAAAAAA
        //= 410D00AAAAAAAA
        if (dataString.startsWith("7E803")) {
            dataString = dataString.substring(5, dataString.length());
        }

        if (isSearching(dataString)) {
            throw new AdapterSearchingException();
        }
        else if (isNoDataCommand(dataString)) {
            throw new NoDataReceivedException();
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
                if (error) {
                    throw new InvalidCommandResponseException(pid);
                }
            }

            else {
                /*
                 * this is a hex number
                 */
                buffer[index/2] = Integer.parseInt(tmp, 16);
                if (buffer[index/2] < 0) {
                    throw new InvalidCommandResponseException(pid);
                }
            }

            index += length;
        }

        return createDataResponse(pid, buffer);
    }

    private DataResponse createDataResponse(PID pid, int[] data) {
        switch (pid) {
            case FUEL_SYSTEM_STATUS:
                //TODO: Implement!
                break;
            case CALCULATED_ENGINE_LOAD:
                return new EngineLoadResponse((data[0] * 100.0f) / 255.0f);
            case FUEL_PRESSURE:
                return new FuelPressureResponse(data[0] * 3);
            case INTAKE_MAP:
                return new IntakeManifoldAbsolutePressureResponse(data[2]);
            case RPM:
                return new EngineRPMResponse((data[0] * 256 + data[1]) / 4);
            case SPEED:
                return new SpeedResponse(data[2]);
            case INTAKE_AIR_TEMP:
                return new IntakeAirPressureResponse(data[0] - 40);
            case MAF:
                return new MAFResponse((data[0] * 256 + data[1]) / 100.0f);
            case TPS:
                return new ThrottlePositionResponse((data[0] * 100) / 255);
            case O2_LAMBDA_PROBE_1_VOLTAGE:
            case O2_LAMBDA_PROBE_2_VOLTAGE:
            case O2_LAMBDA_PROBE_3_VOLTAGE:
            case O2_LAMBDA_PROBE_4_VOLTAGE:
            case O2_LAMBDA_PROBE_5_VOLTAGE:
            case O2_LAMBDA_PROBE_6_VOLTAGE:
            case O2_LAMBDA_PROBE_7_VOLTAGE:
            case O2_LAMBDA_PROBE_8_VOLTAGE:
                return new LambdaProbeVoltageResponse(
                        ((data[2]*256d) + data[3] )/ 8192d,
                        ((data[0]*256d) + data[1]) / 32768d);
            case O2_LAMBDA_PROBE_1_CURRENT:
            case O2_LAMBDA_PROBE_2_CURRENT:
            case O2_LAMBDA_PROBE_3_CURRENT:
            case O2_LAMBDA_PROBE_4_CURRENT:
            case O2_LAMBDA_PROBE_5_CURRENT:
            case O2_LAMBDA_PROBE_6_CURRENT:
            case O2_LAMBDA_PROBE_7_CURRENT:
            case O2_LAMBDA_PROBE_8_CURRENT:
                return new LambdaProbeCurrentResponse(
                        ((data[2]*256d) + data[3])/256d - 128,
                        ((data[0]*256d) + data[1]) / 32768d);
        }

        return null;
    }

    private boolean isSearching(String dataString) {
        return dataString.contains(SEARCHING) || dataString.contains(STOPPED);
    }

    private boolean isNoDataCommand(String dataString) {
        return dataString == null || dataString.contains(NODATA);
    }


}
