///*
// * enviroCar 2013
// * Copyright (C) 2013
// * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
// *
// * This program is free software; you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation; either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software Foundation,
// * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
// *
// */
//package org.envirocar.app.application;
//
//import android.location.GpsStatus.NmeaListener;
//import android.location.Location;
//import android.location.LocationListener;
//import android.location.LocationManager;
//import android.os.Bundle;
//
//import org.envirocar.app.event.EventBus;
//import org.envirocar.core.events.gps.GpsDOPEvent;
//import org.envirocar.core.events.gps.GpsSatelliteFixEvent;
//import org.envirocar.core.logging.Logger;
//
//public class LocationUpdateListener implements LocationListener, NmeaListener {
//
//    private static final Logger logger = Logger.getLogger(LocationUpdateListener.class);
//    private static final String GPGSA = "$GPGSA";
//    private static final String NMEA_SEP = ",";
//    private LocationManager lm;
//
//    public LocationUpdateListener(LocationManager lm) {
//        this.lm = lm;
//    }
//
//    /**
//     * updates the location variables when the device moved
//     */
//    @Override
//    public void onLocationChanged(Location location) {
//        EventBus.getInstance().fireEvent(new LocationEvent(location));
//        logger.debug("Get new position of " + location.getProvider() + " : lat " + location
//                .getLatitude() + " long: " + location.getLongitude());
//    }
//
//    @Override
//    public void onProviderDisabled(String arg0) {
//
//    }
//
//    @Override
//    public void onProviderEnabled(String arg0) {
//
//    }
//
//    @Override
//    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
//
//    }
//
//
//    public void startLocating() {
//        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
//                0, this);
//        lm.addNmeaListener(this);
//    }
//
//    public void stopLocating() {
//        lm.removeUpdates(this);
//        lm.removeNmeaListener(this);
//
//        EventBus.getInstance().fireEvent(new GpsSatelliteFixEvent(0, false));
//    }
//
//    @Override
//    public void onNmeaReceived(long timestamp, String nmea) {
//        /*
//         * eg2.: $GPGSA,A,3,19,28,14,18,27,22,31,39,,,,,1.7,1.0,1.3*35
//		 */
//        if (nmea.startsWith(GPGSA)) {
//            boolean fix = true;
//            if (nmea.charAt(7) == ',' || nmea.charAt(9) == '1') {
//                /*
//				 * no GPS fix, skip
//				 */
//                fix = false;
//            }
//
//            int checksumIndex = nmea.lastIndexOf("*");
//            String[] values;
//            if (checksumIndex > 0) {
//                values = nmea.substring(0, checksumIndex).split(NMEA_SEP);
//            } else {
//				/*
//				 * no checksum, skip
//				 */
//                return;
//            }
//
//            int numberOfSats = resolveSatelliteCount(values);
//
//			/*
//			 * fire an event on the GPS status (fix and number of sats)
//			 */
//            EventBus.getInstance().fireEvent(new GpsSatelliteFixEvent(numberOfSats, fix));
//
//            Double pdop = null, hdop = null, vdop = null;
//            if (values.length > 15) {
//                pdop = parseDopString(values[15]);
//            }
//            if (values.length > 16) {
//                hdop = parseDopString(values[16]);
//            }
//            if (values.length > 17) {
//                vdop = parseDopString(values[17]);
//            }
//
//            if (pdop != null || hdop != null || vdop != null) {
//                EventBus.getInstance().fireEvent(new GpsDOPEvent(pdop, hdop, vdop));
//            }
//        }
//    }
//
//    private int resolveSatelliteCount(String[] values) {
//        if (values == null || values.length < 3) {
//            return 0;
//        }
//
//        int result = 0;
//        for (int i = 3; i < 15; i++) {
//            if (i > values.length - 1) {
//                break;
//            }
//
//            if (!values[i].trim().isEmpty()) {
//                result++;
//            }
//        }
//        return result;
//    }
//
//    private Double parseDopString(String string) {
//        if (string == null || string.isEmpty()) return null;
//
//        return parseOrReturnNull(string);
//    }
//
//    private Double parseOrReturnNull(String trim) {
//        try {
//            return Double.parseDouble(trim.trim());
//        } catch (RuntimeException e) {
//        }
//        return null;
//    }
//
//}
