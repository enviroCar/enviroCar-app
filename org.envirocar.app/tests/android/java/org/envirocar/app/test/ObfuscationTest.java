/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
//package org.envirocar.app.test;
//
//import android.test.InstrumentationTestCase;
//
//import org.envirocar.core.entity.Car;
//import org.envirocar.core.entity.CarImpl;
//import org.envirocar.core.entity.Measurement;
//import org.envirocar.core.entity.MeasurementImpl;
//import org.envirocar.core.entity.Track;
//import org.envirocar.core.entity.TrackImpl;
//import org.envirocar.core.exception.NoMeasurementsException;
//import org.envirocar.core.exception.TrackAlreadyFinishedException;
//import org.envirocar.core.utils.TrackUtils;
//import org.envirocar.remote.serde.TrackSerde;
//import org.hamcrest.CoreMatchers;
//import org.json.JSONException;
//import org.junit.Assert;
//import org.junit.Test;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//public class ObfuscationTest extends InstrumentationTestCase {
//
//    private static int TARGET_LENGTH = 10;
//    private MeasurementImpl first;
//    private MeasurementImpl last;
//    private long start;
//    private long end;
//    private List<Measurement> inter;
//
//    @Test
//    public void testObfuscation() throws JSONException, TrackAlreadyFinishedException, NoMeasurementsException {
//        start = System.currentTimeMillis();
//        end = System.currentTimeMillis() + 1000*60*100;
//
//        first = new MeasurementImpl(51.0, 7.0);
//        first.setTime(start);
//        last = new MeasurementImpl(51.03, 7.03);
//        last.setTime(end);
//
  //      all intermediates should make it!
//        inter = createIntermediates();
//
//        Track t = createTrack();
//
//        TrackSerde s = new TrackSerde();
//        String ts = s.serialize(t, Track.class, null).toString();
//
//        List<Measurement> result = TrackUtils.getObfuscatedTrack(t).getMeasurements();
//
//        Assert.assertTrue("Unexpected element count", result.size() == TARGET_LENGTH);
//        for (Measurement m : inter) {
//            Assert.assertThat(result.contains(m), CoreMatchers.is(true));
//        }
//    }
//
//    @Test
//    public void testObfuscationException() throws JSONException, TrackAlreadyFinishedException {
//        start = System.currentTimeMillis();
//        end = System.currentTimeMillis() + 1000*60;
//
//        first = new MeasurementImpl(51.0, 7.0);
//        first.setTime(start);
//        last = new MeasurementImpl(51.03, 7.03);
//        last.setTime(end);
//
//        Track t = new TrackImpl();
//        t.setCar(new CarImpl("id", "man", "mod", Car.FuelType.DIESEL, 1234, 123));
//
//        t.setMeasurements(Arrays.asList(new MeasurementImpl[] {first, last}));
//
//        Exception exception = null;
//        try {
//            TrackUtils.getObfuscatedTrack(t);
//        }
//        catch (NoMeasurementsException e) {
//            exception = e;
//        }
//
//        Assert.assertNotNull(exception);
//    }
//
//    private Track createTrack() throws TrackAlreadyFinishedException {
//        Track result = new TrackImpl();
//        result.setCar(new CarImpl("id", "man", "mod", Car.FuelType.DIESEL, 1234, 123));
//
//        List<Measurement> measurements = createMeasurements();
//        result.setMeasurements(measurements);
//
//        return result;
//    }
//
//    private List<Measurement> createMeasurements() {
//        List<Measurement> result = new ArrayList<Measurement>();
//
//        result.add(first);
   //     spatial near, this should be removed
//        result.add(createMeasurementNear(first, start + 60 * 1000 + 1));
//
//        for (Measurement m : inter) {
//            result.add(m);
//        }
//
 //       this one should be removed due to time
//        MeasurementImpl farDistanceCloseTime = new MeasurementImpl(51.03, 7.03);
//        farDistanceCloseTime.setTime(end-(60*1000+1));
//        result.add(farDistanceCloseTime);
//        result.add(last);
//        return result;
//    }
//
//    private List<Measurement> createIntermediates() {
//        List<Measurement> result = new ArrayList<>();
//        double deltaLat = first.getLatitude() - last.getLatitude();
//        double deltaLong = first.getLongitude() - last.getLongitude();
//
//        long targetTime = first.getTime() + 61*1000;
//
//        /**
//         * create first half of non-obfuscated (far enough in time and space)
//         */
//        for (int i = 0; i < TARGET_LENGTH/2; i++) {
//            targetTime += 10*1000;
//            Measurement m = new MeasurementImpl(first.getLatitude() - (deltaLat/2+i/10000f), first.getLongitude() - (deltaLong/2+i/10000f));
//            m.setTime(targetTime);
//            result.add(m);
//        }
//
//        targetTime += 10*1000;
//        /**
//         * add another one at the very start - this should be included as well!
//         */
//        result.add(createMeasurementNear(first, targetTime));
//
//        /**
//         * add the second half, all far enough (TARGET_LENGTH - 1 due to the one above)
//         */
//        for (int i = TARGET_LENGTH/2; i < TARGET_LENGTH-1; i++) {
//            targetTime += 10*1000;
//            Measurement m = new MeasurementImpl(first.getLatitude() - (deltaLat/2+i/10000f), first.getLongitude() - (deltaLong/2+i/10000f));
//            m.setTime(targetTime);
//            result.add(m);
//        }
//
//        return result;
//    }
//
//    private Measurement createMeasurementNear(Measurement first, long time) {
//        Measurement result = new MeasurementImpl(first.getLatitude()+0.0001, first.getLongitude()+0.0001);
//        result.setTime(time);
//        return result;
//    }
//
//}