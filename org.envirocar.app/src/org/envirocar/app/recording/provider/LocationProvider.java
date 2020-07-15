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

import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Build;
import android.os.Bundle;

import com.squareup.otto.Bus;

import org.envirocar.app.recording.RecordingScope;
import org.envirocar.core.events.gps.GpsDOPEvent;
import org.envirocar.core.events.gps.GpsLocationChangedEvent;
import org.envirocar.core.events.gps.GpsSatelliteFixEvent;
import org.envirocar.core.exception.PermissionException;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.PermissionUtils;

import java.lang.reflect.Method;

import javax.inject.Inject;

import io.reactivex.Completable;

/**
 * @author dewall
 */
@RecordingScope
public class LocationProvider {
    private static final Logger LOGGER = Logger.getLogger(LocationProvider.class);

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


    private void onNewNmeaUpdate(String nmea, long timestamp) {
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

    // Injected variables.
    private final Context mContext;
    private final Bus mBus;

    private LocationManager mLocationManager;

    // Location fields
    private Location mLastBestLocation;

    private Method oldAddNmeaMethod;

    /**
     * Constructor.
     *
     * @param context the context of the current scope.
     */
    @Inject
    public LocationProvider(@InjectApplicationScope Context context, Bus bus) {
        this.mContext = context;
        this.mBus = bus;
        this.mBus.register(this);

        // Sets the current Location updates to null.
        this.mLastBestLocation = null;

        // Get the LocationManager
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

    }

    @SuppressWarnings({"ResourceType"})
    public Completable startLocating() {
        LOGGER.info("startLocating()");
        return Completable.create(emitter -> {
            if (!PermissionUtils.hasLocationPermission(mContext))
                emitter.onError(new PermissionException("User has not activated Location permission"));

            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    final OnNmeaMessageListener nmeaListener = (nmea, timestamp) -> onNewNmeaUpdate(nmea, timestamp);
                    mLocationManager.addNmeaListener(nmeaListener);

                    emitter.setCancellable(() -> {
                        LOGGER.info("stopLocating()");
                        mLocationManager.removeUpdates(mLocationListener);
                        mLocationManager.removeNmeaListener(nmeaListener);
                    });
                } else {
                    final GpsStatus.NmeaListener nmeaListener = (timestamp, nmea) -> onNewNmeaUpdate(nmea, timestamp);

                    // get the old add nmea method via reflection.
                    if (oldAddNmeaMethod == null) {
                        oldAddNmeaMethod = LocationManager.class.getMethod("addNmeaListener", GpsStatus.NmeaListener.class);
                    }
                    oldAddNmeaMethod.invoke(mLocationManager, nmeaListener);
                    emitter.setCancellable(() -> {
                        LOGGER.info("stopLocating()");
                        mLocationManager.removeUpdates(mLocationListener);

                        try {
                            Method oldRemoveNmeaMethod = LocationManager.class.getMethod("removeNmeaListener", GpsStatus.NmeaListener.class);
                            oldRemoveNmeaMethod.invoke(mLocationManager, nmeaListener);
                        } catch (Exception e){
                            LOGGER.error("unable to remove nmea listener", e);
                        }
                    });
                }
            } catch (Exception e) {
                LOGGER.error("Error while initiating NMEA Listener");
                emitter.setCancellable(() -> {
                    LOGGER.info("stopLocating()");
                    mLocationManager.removeUpdates(mLocationListener);
                });
            }
        });
    }
}
