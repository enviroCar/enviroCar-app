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
package org.envirocar.app.handler;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.app.ActivityCompat;

import com.hwangjr.rxbus.Bus;
import com.hwangjr.rxbus.annotation.Produce;

import org.envirocar.core.events.gps.GpsStateChangedEvent;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.logging.Logger;

import javax.inject.Inject;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class LocationHandler {
    private static final Logger LOGGER = Logger.getLogger(LocationHandler.class);
    private static final int MAX_TIMEFRAME = 1000 * 60;

    // Injected variables.
    private final Context mContext;
    private final Bus mBus;

    private LocationManager mLocationManager;

    // Location fields
    private Location mLastBestLocation;

    /**
     * Constructor.
     *
     * @param context the context of the current scope.
     */
    @Inject
    public LocationHandler(@InjectApplicationScope Context context, Bus bus) {
        this.mContext = context;
        this.mBus = bus;
        try {
            this.mBus.register(this);
        } catch (Exception e){

        }
        // Sets the current Location updates to null.
        this.mLastBestLocation = null;

        // Get the LocationManager
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        // and initialize the last known location.
        initLastKnownLocation();

        // Register a new broadcast receiver for state transitions related to GPS.
        IntentFilter filter = new IntentFilter();
        filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        BroadcastReceiver mGPSStateReceiver = new GpsStateReceiver(isGPSEnabled());
        context.registerReceiver(mGPSStateReceiver, filter);
    }

    /**
     * @return true if GPS is enabled.
     */
    public boolean isGPSEnabled() {
        return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Initializes the last known location on start of the application. First,
     * it tries to receive the last known GPS location. If this is null, the
     * last known network location is considered.
     */
    private void initLastKnownLocation() {
        // Sets the last known location as an initial value
        if (mLastBestLocation == null) {
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the getUserStatistic grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mLastBestLocation = mLocationManager
                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        if (mLastBestLocation == null) {
            mLastBestLocation = mLocationManager
                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
    }

    /**
     * GPSStateReceiver
     */
    private final class GpsStateReceiver extends BroadcastReceiver {
        private boolean previousState;

        /**
         * Constructor.
         *
         * @param currentState tbe current state of the GPS module.
         */
        public GpsStateReceiver(boolean currentState) {
            this.previousState = currentState;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                // get the current gps state.
                boolean isActivated = isGPSEnabled();
                // if the previous state is different to the current state, then fire a new event.
                if (previousState != isActivated) {
                    mBus.post(new GpsStateChangedEvent(isActivated));
                    previousState = isActivated;
                }
            }
        }
    }

    @Produce
    public GpsStateChangedEvent produceGpsStateChangedEvent() {
        return new GpsStateChangedEvent(isGPSEnabled());
    }
}
