package org.envirocar.core.entity;

/**
 * @author dewall
 */
public class UserStatisticImpl implements UserStatistic {

    private int trackCount;
    private double distance;
    private double duration;

    public UserStatisticImpl(){
    }

    public UserStatisticImpl(int trackCount, double distance, double duration) {
        this.trackCount = trackCount;
        this.distance = distance;
        this.duration = duration;
    }

    @Override
    public int getTrackCount() {
        return trackCount;
    }

    @Override
    public void setTrackCount(int trackCount) {
        this.trackCount = trackCount;
    }

    @Override
    public double getDistance() {
        return distance;
    }

    @Override
    public void setDistance(double distance) {
        this.distance = distance;
    }

    @Override
    public double getDuration() {
        return duration;
    }

    @Override
    public void setDuration(double duration) {
        this.duration = duration;
    }

    @Override
    public UserStatistic carbonCopy() {
        UserStatisticImpl other = new UserStatisticImpl();
        other.distance = distance;
        other.duration = duration;
        return other;
    }
}
