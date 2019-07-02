/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;

import com.squareup.otto.Bus;

import org.envirocar.core.events.gps.GpsDOPEvent;
import org.envirocar.core.events.gps.GpsLocationChangedEvent;
import org.envirocar.core.events.gps.GpsSatelliteFixEvent;
import org.envirocar.core.events.gps.GpsStateChangedEvent;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.injection.InjectApplicationScope;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Singleton
public class LocationHandler {
    private static final Logger LOGGER = Logger.getLogger(LocationHandler.class);
    private static final int MAX_TIMEFRAME = 1000 * 60;

    private static final String GPGSA = "$GPGSA";
    private static final String NMEA_SEP = ",";

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            LOGGER.warn("New Location Update");
            mLastBestLocation = location;
            mBus.post(new GpsLocationChangedEvent(mLastBestLocation));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            LOGGER.info(String.format("onStatusChanged(): %s + %s", provider, "" + status));
        }

        @Override
        public void onProviderEnabled(String provider) {
            LOGGER.info(String.format("onProviderEnabled(): %s", provider));
        }

        @Override
        public void onProviderDisabled(String provider) {
            LOGGER.info(String.format("onProviderDisabled(): %s", provider));
        }
    };

    /**
     * Used for receiving NMEA sentences from the GPS.
     */
    GpsStatus.NmeaListener mNmeaListener = new GpsStatus.NmeaListener() {
        @Override
        public void onNmeaReceived(long timestamp, String nmea) {
            // eg2.: $GPGSA,A,3,19,28,14,18,27,22,31,39,,,,,1.7,1.0,1.3*35
            if (nmea.startsWith(GPGSA)) {
                boolean fix = true;
                if (nmea.charAt(7) == ',' || nmea.charAt(9) == '1') {
                    fix = false;    // no GPS fix, skip.
                }

                int checksumIndex = nmea.lastIndexOf("*");
                String[] values;
                if (checksumIndex > 0) {
                    values = nmea.substring(0, checksumIndex).split(NMEA_SEP);
                } else {
                    return;     // no checksum, skip.
                }

                int numberOfSats = resolveSatelliteCount(values);

                // fire an event on the GPS status (fix and number of sats)
                mBus.post(new GpsSatelliteFixEvent(numberOfSats, fix));

                Double pdop = null, hdop = null, vdop = null;
                if (values.length > 15) {
                    pdop = parseDopString(values[15]);
                }
                if (values.length > 16) {
                    hdop = parseDopString(values[16]);
                }
                if (values.length > 17) {
                    vdop = parseDopString(values[17]);
                }

                // Only if positional, horizontal, and vertical DOP is available, then
                // set the DOP accordingly.
                if (pdop != null || hdop != null || vdop != null) {
                    // Dultion of Precision (DOP) to specify multiplicative effect of
                    // navigation satellite geometry on positional measurement precision.
                    // fire an event on the GPS DOP
                    mBus.post(new GpsDOPEvent(pdop, hdop, vdop));
                }
            }
        }

        /**
         * Resolves the number of satellites.
         *
         * @param values
         * @return number of satellites.
         */
        private int resolveSatelliteCount(String[] values) {
            if (values == null || values.length < 3) {
                return 0;
            }

            int result = 0;
            for (int i = 3; i < 15; i++) {
                if (i > values.length - 1) {
                    break;
                }

                if (!values[i].trim().isEmpty()) {
                    result++;
                }
            }
            return result;
        }

        /**
         *
         * @param string
         * @return
         */
        private Double parseDopString(String string) {
            if (string == null || string.isEmpty()) return null;
            try {
                return Double.parseDouble(string.trim());
            } catch (RuntimeException e) {
                // TODO no exception catching?
            }
            return null;
        }
    };

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
     *
     */
    public void startLocating() {
        LOGGER.info("startLocating()");
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
        mLocationManager.addNmeaListener(mNmeaListener);
    }

    /**
     *
     */
    public void stopLocating() {
        LOGGER.info("stopLocating()");
        mLocationManager.removeUpdates(mLocationListener);
        mLocationManager.removeNmeaListener(mNmeaListener);
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
                // to handle the case where the user grants the permission. See the documentation
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
}
