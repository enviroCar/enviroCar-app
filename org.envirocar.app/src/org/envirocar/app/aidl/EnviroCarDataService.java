/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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
package org.envirocar.app.aidl;

import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import com.squareup.otto.Subscribe;

import org.envirocar.aidl.ECMeasurement;
import org.envirocar.aidl.ECRawObdValue;
import org.envirocar.aidl.IECRecordingService;
import org.envirocar.app.events.GPSSpeedChangeEvent;
import org.envirocar.app.events.StartingTimeEvent;
import org.envirocar.app.injection.BaseInjectorService;
import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.events.recording.RecordingNewMeasurementEvent;
import org.envirocar.core.events.TrackFinishedEvent;
import org.envirocar.core.events.gps.GpsLocationChangedEvent;
import org.envirocar.core.logging.Logger;
import org.envirocar.obd.events.TrackRecordingServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dewall
 */
public class EnviroCarDataService extends BaseInjectorService {
    private static final Logger LOG = Logger.getLogger(EnviroCarDataService.class);

    private boolean isRecordingTrack = false;

    private boolean isOBDConnected;

    private String isRecordingSince;

    private ECMeasurement lastMeasurement;

    @Override
    protected void injectDependencies(BaseApplicationComponent appComponent) {
        appComponent.inject(this);
        this.bus.register(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return binder;
    }


    @Subscribe
    public void onReceiveTrackFinishedEvent(TrackFinishedEvent event) {
        LOG.info("Received Track Finished Event");
    }


    @Subscribe
    public void onReceiveLocationUpdate(GpsLocationChangedEvent event){
        LOG.info("[] Received new gps speed event " + event.mLocation.toString());

        Location m = event.mLocation;

        // Creating a new measurement for testing purposes.
        Map<String, String> properties = new HashMap<>();
        properties.put("Speed", m.getSpeed() + "");
        ECMeasurement res = new ECMeasurement(
                m.getLatitude(),
                m.getLongitude(),
                m.getTime(),
                properties
        );

//        this.lastMeasurement = res;
    }

    @Subscribe
    public void onReceiveRecordingStateChangedEvent(TrackRecordingServiceStateChangedEvent event){
        LOG.info("Received event {}".format(event.toString()));
        if(event.mState.equals(BluetoothServiceState.SERVICE_STARTED)) {
            this.isRecordingTrack = true;
        } else {
            this.isRecordingTrack = false;
        }
    }

    @Subscribe
    public void onReceive(GPSSpeedChangeEvent event){
        LOG.info("[] Received new gps speed event " + event.mGPSSpeed);
    }

    @Subscribe
    public void onReceiveStartingTimeEvent(StartingTimeEvent event) {
        LOG.debug("Receieved event {}".format(event.toString()));
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String formatted = dateFormat.format(new Date(event.mStartingTime));
        this.isRecordingSince = formatted;
    }

    @Subscribe
    public void onReceiveNewMeasurementEvent(RecordingNewMeasurementEvent event) {
        LOG.info("[] Received new measurement " + event.mMeasurement.toString());

        Measurement m = event.mMeasurement;

        Map<String, String> properties = new HashMap<>();
        Map<Measurement.PropertyKey, Double> allProperties = m.getAllProperties();
        for (Map.Entry<Measurement.PropertyKey, Double> entry : allProperties.entrySet()) {
            properties.put(entry.getKey().toString(), entry.getValue().toString());
        }

        ECMeasurement res = new ECMeasurement(
                m.getLatitude(),
                m.getLongitude(),
                m.getTime(),
                properties
        );

        this.lastMeasurement = res;
    }

    private final IECRecordingService.Stub binder = new IECRecordingService.Stub() {

        @Override
        public boolean isRecordingTrack() throws RemoteException {
            return EnviroCarDataService.this.isRecordingTrack;
        }

        @Override
        public boolean isOBDConnected() throws RemoteException {
            return EnviroCarDataService.this.isOBDConnected;
        }

        @Override
        public String isRecordingSince() throws RemoteException {
            return EnviroCarDataService.this.isRecordingSince;
        }

        @Override
        public ECMeasurement getLatestMeasurement() throws RemoteException {
            return lastMeasurement;
        }

        @Override
        public String[] listSupportedPhenomenons() throws RemoteException {
            return new String[0];
        }

        @Override
        public ECRawObdValue getLatestObdValue(String s) throws RemoteException {
            return null;
        }

    };

}
