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
package org.envirocar.core.utils;

import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class TrackUtils {
    private static final Logger LOG = Logger.getLogger(TrackUtils.class);

    private static final double OBFUSCATION_DISTANCE_KM = 0.25;
    private static final int OBFUSCATION_TIME_MS = 60000;

    /**
     * resolve all not obfuscated measurements of a track.
     * <p>
     * This returns all measurements, if obfuscation is disabled. Otherwise
     * measurements within the first and last minute and those within the start/end
     * radius of 250 m are ignored (only if they are in the beginning/end of the track).
     *
     * @param track
     * @return
     */
    public static Track getObfuscatedTrack(Track track) throws NoMeasurementsException {
        Track result = track.carbonCopy();
        result.setMeasurements(getNonObfuscatedMeasurements(track));
        return result;
    }

    private static List<Measurement> getNonObfuscatedMeasurements(Track track) throws NoMeasurementsException {
        List<Measurement> measurements = track.getMeasurements();

        List<Measurement> nonPrivateMeasurements = new ArrayList<Measurement>();
        try {
            int first = determineFirstNonObfuscatedIndex(measurements, track);
            int last = determineLastNonObfuscatedIndex(measurements, track);

            if (first == -1 || last == -1) {
                LOG.warn("Could not determine first/last non-obfuscated measurements.");
                throw new NoMeasurementsException("No obfuscated measurements available.");
            }

            for (int i = first; i <= last; i++) {
                nonPrivateMeasurements.add(measurements.get(i));
            }

            return nonPrivateMeasurements;
        } catch (NoMeasurementsException e) {
            LOG.warn("Could not obfuscate track");
            throw e;
        }
    }

    private static int determineFirstNonObfuscatedIndex(List<Measurement> measurements, Track track) throws NoMeasurementsException {
        for (int i = 0; i < measurements.size(); i++) {
            Measurement m = measurements.get(i);
            if (!isTemporalObfuscated(m, track) && !isSpatialObfuscated(m, track)) {
                return i;
            }
        }

        return -1;
    }

    private static int determineLastNonObfuscatedIndex(List<Measurement> measurements, Track track) throws NoMeasurementsException {
        for (int i = measurements.size()-1; i >= 0; i--) {
            Measurement m = measurements.get(i);
            if (!isTemporalObfuscated(m, track) && !isSpatialObfuscated(m, track)) {
                return i;
            }
        }

        return -1;
    }

    public static final boolean isSpatialObfuscated(Measurement measurement, Track track) throws NoMeasurementsException {
        return (LocationUtils.getDistance(track.getFirstMeasurement(), measurement) <= OBFUSCATION_DISTANCE_KM)
                || (LocationUtils.getDistance(track.getLastMeasurement(), measurement) <= OBFUSCATION_DISTANCE_KM);
    }

    public static final boolean isTemporalObfuscated(Measurement measurement, Track track) throws NoMeasurementsException {
        return (measurement.getTime() - track.getStartTime() <= OBFUSCATION_TIME_MS ||
                track.getEndTime() - measurement.getTime() <= OBFUSCATION_TIME_MS);
    }
}
