package org.envirocar.app.handler.userstatistics;

import com.google.common.base.MoreObjects;

/**
 * @author dewall
 */
public class UserStatisticsUpdateEvent {

    public final int numTracks;
    public final double totalDistance;
    public final long totalDuration;

    /**
     * Constructor.
     *
     * @param numTracks
     * @param totalDistance
     * @param totalDuration
     */
    public UserStatisticsUpdateEvent(int numTracks, double totalDistance, long totalDuration) {
        this.numTracks = numTracks;
        this.totalDistance = totalDistance;
        this.totalDuration = totalDuration;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Number of Tracks", numTracks)
                .add("Total Distance", totalDistance)
                .add("Total Duration", totalDuration)
                .toString();
    }
}
