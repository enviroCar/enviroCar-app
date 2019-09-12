/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.recording.provider;

import android.location.Location;
import android.os.SystemClock;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import org.envirocar.app.events.AvrgSpeedUpdateEvent;
import org.envirocar.app.events.DistanceValueUpdateEvent;
import org.envirocar.app.events.GPSSpeedChangeEvent;
import org.envirocar.app.events.StartingTimeEvent;
import org.envirocar.app.events.TrackPathOverlayEvent;
import org.envirocar.app.recording.RecordingService;
import org.envirocar.app.recording.RecordingState;
import org.envirocar.app.views.trackdetails.MapLayer;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.events.recording.RecordingNewMeasurementEvent;
import org.envirocar.core.logging.Logger;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;


/**
 * @author dewall
 */
public class RecordingDetailsProvider implements LifecycleObserver {
    private static final Logger LOG = Logger.getLogger(RecordingDetailsProvider.class);

    private final Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();

    private MapLayer mTrackMapOverlay = new MapLayer();

    private int mNumMeasurements;
    private double mDistanceValue;
    private double mTotalSpeed;
    private int mAvrgSpeed;
    private double GPSSpeed;

    private long mStartingBaseTime;

    private Location mLastLocation;
    private Location mCurrentLocation;

    private final Bus eventBus;

    /**
     * Constructor.
     *
     * @param bus
     */
    public RecordingDetailsProvider(Bus bus) {
        this.eventBus = bus;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    protected void onCreate(){
        try {
            this.eventBus.register(this);
        } catch (IllegalArgumentException e){
            LOG.error("RecordingDetailsProvider was already registered.", e);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    protected void onDestroy(){
        try {
            this.eventBus.unregister(this);
        } catch (IllegalArgumentException e) {
            LOG.info("RecordingDetailsProvider was not registered on event bus.");
        }
        clear();
    }

    @Subscribe
    public void onReceiveNewMeasurementEvent(RecordingNewMeasurementEvent event) {
        LOG.debug(String.format("Received event: %s", event.toString()));

        if (mNumMeasurements == 0) {
            mStartingBaseTime = SystemClock.elapsedRealtime();
            eventBus.post(new StartingTimeEvent(mStartingBaseTime, true));
        }

        mNumMeasurements++;

        // update computed features
        updateDistance(event.mMeasurement);
        updateAverageSpeed(event.mMeasurement);
        updatePathOverlay(event.mMeasurement);
        if (RecordingService.RECORDING_STATE == RecordingState.RECORDING_RUNNING
                && event.mMeasurement.hasProperty(Measurement.PropertyKey.GPS_SPEED)) {
            GPSSpeed = event.mMeasurement.getProperty(Measurement.PropertyKey.GPS_SPEED);
            eventBus.post(produceGPSSpeedEvent());
        }
    }

    @Produce
    public GPSSpeedChangeEvent produceGPSSpeedEvent() {
        return new GPSSpeedChangeEvent(GPSSpeed);
    }

    @Produce
    public TrackPathOverlayEvent provideTrackPathOverlay() {
        return new TrackPathOverlayEvent(mTrackMapOverlay);
    }

    @Produce
    public DistanceValueUpdateEvent provideDistanceValue() {
        return new DistanceValueUpdateEvent(mDistanceValue);
    }

    @Produce
    public AvrgSpeedUpdateEvent provideAverageSpeed() {
        return new AvrgSpeedUpdateEvent(mAvrgSpeed);
    }

    @Produce
    public StartingTimeEvent provideStartingTime() {
        if (mStartingBaseTime == 0)
            return new StartingTimeEvent(SystemClock.elapsedRealtime(), false);
        return new StartingTimeEvent(mStartingBaseTime, true);
    }

    private void updatePathOverlay(Measurement measurement) {
        mMainThreadWorker.schedule(() -> {
            LOG.info("Map being updated with new points: " + measurement.getLatitude() + measurement.getLongitude());
            mTrackMapOverlay.addPoint(measurement.getLatitude(), measurement.getLongitude());
        });
    }

    /**
     * Updates the distance value based on the new measurements location and the location of the
     * previous location.
     *
     * @param measurement the measurement to compute the distance relative to the last measurement.
     */
    private void updateDistance(Measurement measurement) {
        if (mLastLocation == null) {
            mLastLocation = new Location("GPS");
            mLastLocation.setLatitude(measurement.getLatitude());
            mLastLocation.setLongitude(measurement.getLongitude());
        } else {
            mCurrentLocation = new Location("GPS");
            mCurrentLocation.setLatitude(measurement.getLatitude());
            mCurrentLocation.setLongitude(measurement.getLongitude());

            // Compute the distance between the last location and the new location.
            float[] res = new float[1];
            Location.distanceBetween(
                    mLastLocation.getLatitude(),
                    mLastLocation.getLongitude(),
                    mCurrentLocation.getLatitude(),
                    mCurrentLocation.getLongitude(),
                    res);

            // update the distance value
            if (res[0] > 0) {
                mDistanceValue += res[0] / 1000;
                eventBus.post(provideDistanceValue());
            }
            mLastLocation = mCurrentLocation;
            mCurrentLocation = null;
        }
    }

    /**
     * @param measurement
     */
    private void updateAverageSpeed(Measurement measurement) {
        if (RecordingService.RECORDING_STATE == RecordingState.RECORDING_RUNNING) {
            double speedValue = measurement.hasProperty(Measurement.PropertyKey.SPEED) ?
                    measurement.getProperty(Measurement.PropertyKey.SPEED) :
                    measurement.getProperty(Measurement.PropertyKey.GPS_SPEED);

            mTotalSpeed += speedValue;
            mAvrgSpeed = (int) mTotalSpeed / mNumMeasurements;
            eventBus.post(provideAverageSpeed());
        }
    }

    public void clear() {
        mMainThreadWorker.schedule(() -> {
            mTrackMapOverlay.clearPath();
            mNumMeasurements = 0;
            mDistanceValue = 0;
            mTotalSpeed = 0;
            mAvrgSpeed = 0;
            mStartingBaseTime = 0;
            mLastLocation = null;
            mCurrentLocation = null;
        });
    }

}
