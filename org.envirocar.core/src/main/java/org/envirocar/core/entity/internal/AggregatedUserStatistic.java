package org.envirocar.core.entity.internal;

/**
 * @author dewall
 */
public class AggregatedUserStatistic {

    private int numTracks;
    private double totalDuration;
    private double totalDistance;

    public AggregatedUserStatistic() {
        this(0, 0 , 0);
    }

    public AggregatedUserStatistic(int numTracks, double totalDuration, double totalDistance) {
        this.numTracks = numTracks;
        this.totalDuration = totalDuration;
        this.totalDistance = totalDistance;
    }

    public int getNumTracks() {
        return numTracks;
    }

    public double getTotalDuration() {
        return totalDuration;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setNumTracks(int numTracks) {
        this.numTracks = numTracks;
    }

    public void setTotalDuration(double totalDuration) {
        this.totalDuration = totalDuration;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }
}
