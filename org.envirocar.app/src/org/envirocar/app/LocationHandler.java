package org.envirocar.app;

import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.squareup.otto.Bus;
import com.squareup.otto.Produce;

import org.envirocar.app.events.GpsDOP;
import org.envirocar.app.events.GpsDOPEvent;
import org.envirocar.app.events.GpsSatelliteFix;
import org.envirocar.app.events.GpsSatelliteFixEvent;
import org.envirocar.app.events.LocationChangedEvent;
import org.envirocar.app.injection.InjectApplicationScope;
import org.envirocar.app.injection.Injector;
import org.envirocar.app.logging.Logger;

import javax.inject.Inject;

/**
 * @author dewall
 */
public class LocationHandler {
    private static final Logger LOGGER = Logger.getLogger(LocationHandler.class);
    private static final int MAX_TIMEFRAME = 1000 * 60;

    private static final String GPGSA = "$GPGSA";
    private static final String NMEA_SEP = ",";

    // Injected variables.
    @Inject
    @InjectApplicationScope
    protected Context mContext;
    @Inject
    protected Bus mBus;

    private LocationManager mLocationManager;

    // Location fields
    private Location mLastLocationUpdate;
    private Location mLastBestLocation;

    // the last satellite fix of GPS events.
    private GpsSatelliteFix mLastGpsSatelliteFix;

    // Dultion of Precision (DOP) to specify multiplicative effect of
    // navigation satellite geometry on positional measurement precision.
    private GpsDOP mLastGpsDOP;

    protected final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            LOGGER.warn("New Location Update");
            mLastBestLocation = location;
            mBus.post(new LocationChangedEvent(mLastBestLocation));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            LOGGER.info(String.format("onStatusChanged(): %s", provider));
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
    private final GpsStatus.NmeaListener mNmeaListener = new GpsStatus.NmeaListener() {
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

                // Set the last satellite fix for GPS.
                mLastGpsSatelliteFix = new GpsSatelliteFix(numberOfSats, fix);
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
                    // set the new last GPS dop.
                    mLastGpsDOP = new GpsDOP(pdop, hdop, vdop);
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

    /**
     * Constructor.
     *
     * @param context the context of the current scope.
     */
    public LocationHandler(Context context) {
        // Inject ourselves and register on the bus.
        ((Injector) context).injectObjects(this);
        this.mBus.register(this);

        // Sets the current Location updates to null.
        this.mLastLocationUpdate = null;
        this.mLastBestLocation = null;

        // Get the LocationManager
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        // and initialize the last known location.
        initLastKnownLocation();
    }

    /**
     *
     */
    public void startLocating() {
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
                mLocationListener);
        mLocationManager.addNmeaListener(mNmeaListener);
    }

    /**
     *
     */
    public void stopLocating() {
        mLocationManager.removeUpdates(mLocationListener);
        mLocationManager.removeNmeaListener(mNmeaListener);
    }

//    @Produce
    public LocationChangedEvent produceLocationChangedEvent() {
        if (mLastBestLocation == null)
            return null;
        return new LocationChangedEvent(mLastBestLocation);
    }

//    @Produce
    public GpsDOPEvent produceGpsDOPEvent() {
        if (mLastGpsDOP == null)
            return null;
        return new GpsDOPEvent(mLastGpsDOP);
    }

//    @Produce
    public GpsSatelliteFixEvent produceGpsSatelliteFixEvent() {
        if (mLastGpsSatelliteFix == null)
            return null;
        return new GpsSatelliteFixEvent(mLastGpsSatelliteFix);
    }

    /**
     * Initializes the last known location on start of the application. First,
     * it tries to receive the last known GPS location. If this is null, the
     * last known network location is considered.
     */
    private void initLastKnownLocation() {
        // Sets the last known location as an initial value
        if (mLastBestLocation == null) {
            mLastBestLocation = mLocationManager
                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        if (mLastBestLocation == null) {
            mLastBestLocation = mLocationManager
                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
    }

}
