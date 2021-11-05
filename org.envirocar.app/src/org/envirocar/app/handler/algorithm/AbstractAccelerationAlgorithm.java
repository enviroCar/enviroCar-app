package org.envirocar.app.handler.algorithm;

public class AbstractAccelerationAlgorithm {

    private static final double CONV_FACTOR = 3.6;

    /**
     * Calculates the acceleration in m/s² from two speed values v1 and v2 at time t1 and t2.
     *
     * @param start Speed at time t1
     * @param end Speed at time t2
     * @param startTime Time t1
     * @param endTime Time t2
     * @return Acceleration in m/s²
     */
    public Double calculateAcceleration(Number start, Number end, long startTime, long endTime) {
        if (start == null || end == null) {
            return null;
        }

        double dV = (end.doubleValue() - start.doubleValue());
        double dT = (endTime -  startTime);
        return (dV / CONV_FACTOR - dT / 1000);
    }
}
