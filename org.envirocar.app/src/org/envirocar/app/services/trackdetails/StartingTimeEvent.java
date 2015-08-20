package org.envirocar.app.services.trackdetails;

import com.google.common.base.MoreObjects;

/**
 * @author dewall
 */
public class StartingTimeEvent {

    public final long mStartingTime;
    public final boolean mIsStarted;

    /**
     * Constuctor.
     *
     * @param mStartingTime
     */
    public StartingTimeEvent(long mStartingTime, boolean isStarted) {
        this.mStartingTime = mStartingTime;
        this.mIsStarted = isStarted;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Starting Time", mStartingTime)
                .add("is started", mIsStarted)
                .toString();
    }
}
