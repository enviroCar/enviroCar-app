package org.envirocar.obd.processing;

import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.MeasurementImpl;
import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.response.DataResponse;
import org.envirocar.obd.commands.response.entity.LambdaProbeCurrentResponse;
import org.envirocar.obd.commands.response.entity.LambdaProbeVoltageResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;

public class InterpolationMeasurementProvider implements MeasurementProvider {

    private Map<PID, List<DataResponse>> bufferedResponses = new HashMap<>();
    private long firstTimestampToBeConsidered;
    private Map<PID, Measurement.PropertyKey> pidPropertyMap = new HashMap<>();

    public InterpolationMeasurementProvider() {
        pidPropertyMap.put(PID.SPEED, Measurement.PropertyKey.SPEED);
        pidPropertyMap.put(PID.MAF, Measurement.PropertyKey.MAF);
        pidPropertyMap.put(PID.CALCULATED_ENGINE_LOAD, Measurement.PropertyKey.CALCULATED_MAF);
        pidPropertyMap.put(PID.RPM, Measurement.PropertyKey.RPM);
        pidPropertyMap.put(PID.INTAKE_MAP, Measurement.PropertyKey.INTAKE_PRESSURE);
        pidPropertyMap.put(PID.INTAKE_AIR_TEMP, Measurement.PropertyKey.INTAKE_TEMPERATURE);
        pidPropertyMap.put(PID.O2_LAMBDA_PROBE_1_VOLTAGE, Measurement.PropertyKey.LAMBDA_VOLTAGE);
    }

    @Override
    public Observable<Measurement> measurements(long samplingRate) {
        return Observable.create(new Observable.OnSubscribe<Measurement>() {
            @Override
            public void call(Subscriber<? super Measurement> subscriber) {
                while (!subscriber.isUnsubscribed()) {
                    synchronized (InterpolationMeasurementProvider.this) {
                        /**
                         * wait the sampling rate
                         */
                        try {
                            InterpolationMeasurementProvider.this.wait(samplingRate);
                        } catch (InterruptedException e) {
                            subscriber.onError(e);
                        }

                        Measurement m = createMeasurement();
                        
                        if (m != null) {
                            subscriber.onNext(m);
                        }
                    }
                }
            }
        });
    }

    private synchronized Measurement createMeasurement() {
        long latestTimestampToBeConsidered = System.currentTimeMillis();
        /**
         * use the middle of the time window
         */
        long targetTimestamp = firstTimestampToBeConsidered + ((latestTimestampToBeConsidered - firstTimestampToBeConsidered) / 2);

        Measurement m = new MeasurementImpl();
        m.setTime(targetTimestamp);

        for (PID pid : this.bufferedResponses.keySet()) {
            appendToMeasurement(pid, this.bufferedResponses.get(pid), m, targetTimestamp);
        }

        /**
         * clear the buffer of DataResponses to be considered
         */
        clearBuffer();
        return m;
    }

    private void appendToMeasurement(PID pid, List<DataResponse> dataResponses, Measurement m, long targetTimestamp) {
        switch (pid) {
            case FUEL_SYSTEM_STATUS:
                m.setProperty(mapPidToProperty(pid), first(dataResponses));
            case CALCULATED_ENGINE_LOAD:
            case SHORT_TERM_FUEL_TRIM_BANK_1:
            case LONG_TERM_FUEL_TRIM_BANK_1:
            case FUEL_PRESSURE:
            case INTAKE_MAP:
            case RPM:
            case SPEED:
            case INTAKE_AIR_TEMP:
            case MAF:
            case TPS:
                m.setProperty(mapPidToProperty(pid), interpolate(dataResponses, targetTimestamp));
                break;
            case O2_LAMBDA_PROBE_1_VOLTAGE:
            case O2_LAMBDA_PROBE_2_VOLTAGE:
            case O2_LAMBDA_PROBE_3_VOLTAGE:
            case O2_LAMBDA_PROBE_4_VOLTAGE:
            case O2_LAMBDA_PROBE_5_VOLTAGE:
            case O2_LAMBDA_PROBE_6_VOLTAGE:
            case O2_LAMBDA_PROBE_7_VOLTAGE:
            case O2_LAMBDA_PROBE_8_VOLTAGE:
                appendLambdaVoltage(dataResponses, m, targetTimestamp);
                break;
            case O2_LAMBDA_PROBE_1_CURRENT:
            case O2_LAMBDA_PROBE_2_CURRENT:
            case O2_LAMBDA_PROBE_3_CURRENT:
            case O2_LAMBDA_PROBE_4_CURRENT:
            case O2_LAMBDA_PROBE_5_CURRENT:
            case O2_LAMBDA_PROBE_6_CURRENT:
            case O2_LAMBDA_PROBE_7_CURRENT:
            case O2_LAMBDA_PROBE_8_CURRENT:
                appendLambdaCurrent(dataResponses, m, targetTimestamp);
                break;
        }

    }

    private void appendLambdaVoltage(List<DataResponse> dataResponses, Measurement m, long targetTimestamp) {

    }

    private void appendLambdaCurrent(List<DataResponse> dataResponses, Measurement m, long targetTimestamp) {
    }

    private Double first(List<DataResponse> dataResponses) {
        return dataResponses.isEmpty() ? null : dataResponses.get(0).getValue().doubleValue();
    }

    protected Double interpolate(List<DataResponse> dataResponses, long targetTimestamp) {
        if (dataResponses.size() <= 1) {
            return first(dataResponses);
        }

        /**
         * find the closest two measurements
         */
        int midIndex = dataResponses.size() / 2;
        DataResponse start = null, end = null;
        int i = midIndex;
        while (i >= 0) {
            if (dataResponses.get(i).getTimestamp() <= targetTimestamp) {
                start = dataResponses.get(i);
                break;
            }

            i--;
        }

        while (i < dataResponses.size()) {
            if (dataResponses.get(i).getTimestamp() > targetTimestamp) {
                end = dataResponses.get(i);
                break;
            }

            i++;
        }

        return interpolateTwo(start, end, targetTimestamp);
    }

    protected Double interpolateTwo(DataResponse start, DataResponse end, long targetTimestamp) {
        if (start == null && end == null) {
            return null;
        }
        if (start == null) {
            return end.getValue().doubleValue();
        }
        else if (end == null) {
            return start.getValue().doubleValue();
        }

        float duration = (float) (end.getTimestamp() - start.getTimestamp());

        float endWeight = (targetTimestamp - start.getTimestamp()) / duration;
        float startWeight = (end.getTimestamp() - targetTimestamp) / duration;

        return start.getValue().doubleValue() * startWeight + end.getValue().doubleValue() * endWeight;
    }

    private Measurement.PropertyKey mapPidToProperty(PID pid) {
        return pidPropertyMap.get(pid);
    }

    private void clearBuffer() {
        for (List<DataResponse> drl : this.bufferedResponses.values()) {
            drl.clear();
        }

        /**
         * reset the first timestamp
         */
        this.firstTimestampToBeConsidered = 0;
    }

    @Override
    public synchronized void consider(DataResponse dr) {
        if (this.firstTimestampToBeConsidered == 0) {
            this.firstTimestampToBeConsidered = dr.getTimestamp();
        }
        
        PID pid = dr.getPid();
        if (bufferedResponses.containsKey(pid)) {
            bufferedResponses.get(pid).add(dr);
        }
        else {
            List<DataResponse> list = new ArrayList<>();
            list.add(dr);
            bufferedResponses.put(pid, list);
        }
    }
}