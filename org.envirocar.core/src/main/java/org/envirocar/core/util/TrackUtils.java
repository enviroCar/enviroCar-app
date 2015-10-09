package org.envirocar.core.util;

import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class TrackUtils {
    private static final Logger LOG = Logger.getLogger(TrackUtils.class);

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
    public static Track getObfuscatedTrack(Track track) {
        Track result = track.carbonCopy();
        result.setMeasurements(getNonObfuscatedMeasurements(track));
        return track;
    }

    private static List<Measurement> getNonObfuscatedMeasurements(Track track) {
        List<Measurement> measurements = track.getMeasurements();

        boolean wasAtLeastOneTimeNotObfuscated = false;
        ArrayList<Measurement> privateCandidates = new ArrayList<Measurement>();
        ArrayList<Measurement> nonPrivateMeasurements = new ArrayList<Measurement>();
        for (Measurement measurement : measurements) {
            try {
                // Filter measurements by their temporal distance to the starting measurement.
                if (isTemporalObfuscationCandidate(measurement, track)) {
                    continue;
                }

                // Filter measurements by their distance to the starting point
                // TODO this is by far the worst implementation somebody could do...
                if (isSpatialObfuscationCandidate(measurement, track)) {
                    if (wasAtLeastOneTimeNotObfuscated) {
                        privateCandidates.add(measurement);
                        nonPrivateMeasurements.add(measurement);
                    }
                    continue;
                }

                /*
                 * we may have found obfuscation candidates in the middle of the track
                 * (may cross start or end point) in a PRIOR iteration
                 * of this loop. these candidates can be removed now as we are again
                 * out of obfuscation scope
                 */
                if (wasAtLeastOneTimeNotObfuscated) {
                    privateCandidates.clear();
                } else {
                    wasAtLeastOneTimeNotObfuscated = true;
                }

                nonPrivateMeasurements.add(measurement);
            } catch (NoMeasurementsException e) {
                LOG.warn(e.getMessage(), e);
            }

        }
        /*
         * the private candidates which have made it until here
         * shall be ignored
         */
        nonPrivateMeasurements.removeAll(privateCandidates);
        return nonPrivateMeasurements;
    }

    private static boolean isSpatialObfuscationCandidate(Measurement measurement, Track track)
            throws NoMeasurementsException {
        return (Util.getDistance(track.getFirstMeasurement(), measurement) <= 0.25)
                || (Util.getDistance(track.getLastMeasurement(), measurement) <= 0.25);
    }

    private static boolean isTemporalObfuscationCandidate(Measurement measurement, Track track)
            throws NoMeasurementsException {
        return (measurement.getTime() - track.getStartTime() <= 60000 ||
                track.getEndTime() - measurement.getTime() <= 60000);
    }
}
