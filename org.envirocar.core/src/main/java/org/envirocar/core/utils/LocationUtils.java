package org.envirocar.core.utils;

import android.location.Location;

import org.envirocar.core.entity.Measurement;

/**
 * @author dewall
 */
public class LocationUtils {

    private LocationUtils(){
    }

    /**
     * Returns the distance of two points in kilometers.
     *
     * @param lat1
     * @param lng1
     * @param lat2
     * @param lng2
     * @return distance in km
     */
    public static double getDistance(double lat1, double lng1, double lat2, double lng2) {
        float[] res = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, res);
        return res[0] / 1000.0d;
    }

    /**
     * Returns the distance of two measurements in kilometers.
     *
     * @param m1 first {@link Measurement}
     * @param m2 second {@link Measurement}
     * @return distance in km
     */
    public static double getDistance(Measurement m1, Measurement m2) {
        return getDistance(m1.getLatitude(), m1.getLongitude(), m2.getLatitude(), m2.getLongitude());
    }
}
