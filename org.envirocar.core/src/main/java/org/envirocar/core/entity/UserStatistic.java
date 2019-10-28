package org.envirocar.core.entity;

/**
 * @author dewall
 */
public interface UserStatistic extends BaseEntity<UserStatistic> {
    String KEY_TRACKCOUNT = "trackCount";
    String KEY_DURATION = "duration";
    String KEY_DISTANCE = "distance";

    /**
     * @return the track count
     */
    int getTrackCount();

    /**
     * Sets the track count.
     *
     * @param trackCount
     */
    void setTrackCount(int trackCount);

    /**
     * @return the distance.
     */
    double getDistance();

    /**
     * Sets the distance.
     *
     * @param distance the distance to set.
     */
    void setDistance(double distance);

    /**
     * @return the duration.
     */
    double getDuration();

    /**
     * Sets the duration.
     *
     * @param duration the duration to set.
     */
    void setDuration(double duration);
}
