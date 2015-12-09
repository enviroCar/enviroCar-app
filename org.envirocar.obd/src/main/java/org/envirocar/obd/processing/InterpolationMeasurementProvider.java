package org.envirocar.obd.processing;

import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.MeasurementImpl;
import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.Timestamped;
import org.envirocar.obd.commands.response.DataResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;

public class InterpolationMeasurementProvider extends AbstractMeasurementProvider {

    private Map<PID, List<DataResponse>> bufferedResponses = new HashMap<>();
    private long firstTimestampToBeConsidered;
    private long lastTimestampToBeConsidered;

    public InterpolationMeasurementProvider() {
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
        /**
         * use the middle of the time window
         */
        long targetTimestamp = firstTimestampToBeConsidered + ((lastTimestampToBeConsidered - firstTimestampToBeConsidered) / 2);

        Measurement m = new MeasurementImpl();
        m.setTime(targetTimestamp);

        for (PID pid : this.bufferedResponses.keySet()) {
            appendToMeasurement(pid, this.bufferedResponses.get(pid), m);
        }

        /**
         * clear the buffer of DataResponses to be considered
         */
        clearBuffer();
        setPosition(m, getAndClearPositionBuffer());
        return m;
    }

    private void setPosition(Measurement m, List<Position> positionBuffer) {
        if (positionBuffer == null || positionBuffer.isEmpty()) {
            return;
        }

        if (positionBuffer.size() == 1) {
            Position pos = positionBuffer.get(0);
            m.setLatitude(pos.getLatitude());
            m.setLongitude(pos.getLongitude());
        }
        else {
            long targetTimestamp = m.getTime();

            /**
             * find the closest two measurements
             */
            int startIndex = findStartIndex(positionBuffer, targetTimestamp);
            Position start = positionBuffer.get(startIndex);
            Position end = startIndex + 1 < positionBuffer.size() ? positionBuffer.get(startIndex + 1) : null;

            double lat = interpolateTwo(start.getLatitude(), end != null ? end.getLatitude() : null, targetTimestamp, start.getTimestamp(),
                    end != null ? end.getTimestamp() : 0L);
            double lon = interpolateTwo(start.getLongitude(), end != null ? end.getLongitude() : null, targetTimestamp, start.getTimestamp(),
                    end != null ? end.getTimestamp() : 0L);

            m.setLatitude(lat);
            m.setLongitude(lon);
        }

    }

    private void appendToMeasurement(PID pid, List<DataResponse> dataResponses, Measurement m) {
        long targetTimestamp = m.getTime();
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
                m.setProperty(mapPidToProperty(pid), interpolate(dataResponses, m.getTime()));
                break;
            case O2_LAMBDA_PROBE_1_VOLTAGE:
            case O2_LAMBDA_PROBE_2_VOLTAGE:
            case O2_LAMBDA_PROBE_3_VOLTAGE:
            case O2_LAMBDA_PROBE_4_VOLTAGE:
            case O2_LAMBDA_PROBE_5_VOLTAGE:
            case O2_LAMBDA_PROBE_6_VOLTAGE:
            case O2_LAMBDA_PROBE_7_VOLTAGE:
            case O2_LAMBDA_PROBE_8_VOLTAGE:
                appendLambdaVoltage(dataResponses, m);
                break;
            case O2_LAMBDA_PROBE_1_CURRENT:
            case O2_LAMBDA_PROBE_2_CURRENT:
            case O2_LAMBDA_PROBE_3_CURRENT:
            case O2_LAMBDA_PROBE_4_CURRENT:
            case O2_LAMBDA_PROBE_5_CURRENT:
            case O2_LAMBDA_PROBE_6_CURRENT:
            case O2_LAMBDA_PROBE_7_CURRENT:
            case O2_LAMBDA_PROBE_8_CURRENT:
                appendLambdaCurrent(dataResponses, m);
                break;
        }

    }

    private void appendLambdaVoltage(List<DataResponse> dataResponses, Measurement m) {
        appendComposite(dataResponses, m, m.getTime(),
                new Measurement.PropertyKey[]{Measurement.PropertyKey.LAMBDA_VOLTAGE_ER,
                        Measurement.PropertyKey.LAMBDA_VOLTAGE});
    }

    private void appendLambdaCurrent(List<DataResponse> dataResponses, Measurement m) {
        appendComposite(dataResponses, m, m.getTime(),
                new Measurement.PropertyKey[] {Measurement.PropertyKey.LAMBDA_CURRENT_ER,
                        Measurement.PropertyKey.LAMBDA_CURRENT});
    }

    /**
     * append a composite property to the measurement
     *
     * @param dataResponses the list of value responses
     * @param m the measurement
     * @param targetTimestamp the target timestamp used for interpolation
     * @param propertyKeys the list of property keys. has to be in the same order as the
     *                     values are provided via DataResponse#getCompositeValues
     */
    protected void appendComposite(List<DataResponse> dataResponses, Measurement m, long targetTimestamp, Measurement.PropertyKey[] propertyKeys) {
        /**
         * find the closest two measurements
         */
        int startIndex = findStartIndex(dataResponses, targetTimestamp);
        DataResponse start = dataResponses.get(startIndex);
        DataResponse end = startIndex + 1 < dataResponses.size() ? dataResponses.get(startIndex + 1) : null;

        int length = Math.min(propertyKeys.length, start.getCompositeValues().length);

        /**
         * iterate through the properties and interpolate them
         */
        for (int i = 0; i < length; i++) {
            Number startValue = start.getCompositeValues()[i];
            Number endValue = end != null ? end.getCompositeValues()[i] : null;
            double value = interpolateTwo(startValue, endValue, targetTimestamp, start.getTimestamp(),
                    end != null ? end.getTimestamp() : 0L);
            m.setProperty(propertyKeys[i], value);
        }
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
        int startIndex = findStartIndex(dataResponses, targetTimestamp);
        DataResponse start = dataResponses.get(startIndex);
        DataResponse end = startIndex + 1 < dataResponses.size() ? dataResponses.get(startIndex + 1) : null;

        return interpolateTwo(start.getValue(), end != null ? end.getValue() : null, targetTimestamp, start.getTimestamp(),
                end != null ? end.getTimestamp() : 0L);
    }

    private int findStartIndex(List<? extends Timestamped> dataResponses, long targetTimestamp) {
        int i = 0;
        while (i+1 < dataResponses.size()) {
            if (dataResponses.get(i).getTimestamp() <= targetTimestamp
                    && dataResponses.get(i+1).getTimestamp() >= targetTimestamp) {
                return i;
            }

            i++;
        }

        return 0;
    }

    /**
     *
     * @param start the start value
     * @param end the end value
     * @param targetTimestamp the target timestamp used for interpolation
     * @param startTimestamp the timestamp of the start
     * @param endTimestamp the timestamp of the lend
     * @return the interpolated value
     */
    protected Double interpolateTwo(Number start, Number end, long targetTimestamp,
                                    long startTimestamp, long endTimestamp) {
        if (start == null && end == null) {
            return null;
        }
        if (start == null) {
            return end.doubleValue();
        }
        else if (end == null) {
            return start.doubleValue();
        }

        float duration = (float) (endTimestamp - startTimestamp);

        float endWeight = (targetTimestamp - startTimestamp) / duration;
        float startWeight = (endTimestamp - targetTimestamp) / duration;

        return start.doubleValue() * startWeight + end.doubleValue() * endWeight;
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
        this.lastTimestampToBeConsidered = Math.max(this.lastTimestampToBeConsidered, dr.getTimestamp());

        if (this.firstTimestampToBeConsidered == 0) {
            this.firstTimestampToBeConsidered = dr.getTimestamp();
        }
        else {
            this.firstTimestampToBeConsidered = Math.min(this.firstTimestampToBeConsidered, dr.getTimestamp());
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