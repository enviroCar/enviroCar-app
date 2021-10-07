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
package org.envirocar.core.entity;

import org.envirocar.core.R;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface Measurement extends BaseEntity<Measurement> {

    interface PropertyKeyExtension {
        int getStringResource();

        int getUnitResource();
    }

    // All measurement values
    enum PropertyKey implements PropertyKeyExtension {
        SPEED {
            @Override
            public int getStringResource() {
                return R.string.property_key_speed;
            }

            public String toString() {
                return "Speed";
            }
        },
        MAF {
            @Override
            public int getStringResource() {
                return R.string.property_key_maf;
            }

            public String toString() {
                return "MAF";
            }
        },
        CALCULATED_MAF {
            @Override
            public int getStringResource() {
                return R.string.property_key_calc_maf;
            }

            public String toString() {
                return "Calculated MAF";
            }
        },
        RPM {
            @Override
            public int getStringResource() {
                return R.string.property_key_rpm;
            }

            public String toString() {
                return "Rpm";
            }
        },
        INTAKE_TEMPERATURE {
            @Override
            public int getStringResource() {
                return R.string.property_key_intake_temp;
            }

            public String toString() {
                return "Intake Temperature";
            }
        },
        INTAKE_PRESSURE {
            @Override
            public int getStringResource() {
                return R.string.property_key_intake_pressure;
            }

            public String toString() {
                return "Intake Pressure";
            }
        },
        CO2 {
            @Override
            public int getStringResource() {
                return R.string.property_key_co2;
            }

            public String toString() {
                return "CO2";
            }
        },
        ENGINE_FULE_RATE {
            @Override
            public int getStringResource() {
                return R.string.property_key_engine_fuel_rate;
            }

            public String toString() {
                return "Engine Fuel Rate";
            }
        },
        CONSUMPTION {
            @Override
            public int getStringResource() {
                return R.string.property_key_consumption;
            }

            public String toString() {
                return "Consumption";
            }
        },
        ENERGY_CONSUMPTION {
            @Override
            public int getStringResource() {
                return R.string.property_key_energy_consumption;
            }

            public String toString() {
                return "Consumption (GPS-based)";
            }
        },
        ENERGY_CONSUMPTION_CO2 {
            @Override
            public int getStringResource() {
                return R.string.property_key_energy_co2_emission;
            }

            @Override
            public String toString() {
                return "CO2 Emission (GPS-based)";
            }
        },
        THROTTLE_POSITON {
            @Override
            public int getStringResource() {
                return R.string.property_key_throttle_position;
            }

            @Override
            public String toString() {
                return "Throttle Position";
            }
        },
        ENGINE_LOAD {
            @Override
            public int getStringResource() {
                return R.string.property_key_engine_load;
            }

            @Override
            public String toString() {
                return "Engine Load";
            }
        },
        GPS_ACCURACY {
            @Override
            public int getStringResource() {
                return R.string.property_key_gps_accuracy;
            }

            @Override
            public String toString() {
                return "GPS Accuracy";
            }
        },
        GPS_SPEED {
            @Override
            public int getStringResource() {
                return R.string.property_key_gps_speed;
            }

            @Override
            public String toString() {
                return "GPS Speed";
            }
        },
        GPS_BEARING {
            @Override
            public int getStringResource() {
                return R.string.property_key_gps_bearing;
            }

            @Override
            public String toString() {
                return "GPS Bearing";
            }
        },
        GPS_ALTITUDE {
            @Override
            public int getStringResource() {
                return R.string.property_key_gps_altitude;
            }

            @Override
            public String toString() {
                return "GPS Altitude";
            }
        },
        GPS_PDOP {
            @Override
            public int getStringResource() {
                return R.string.property_key_gps_pdop;
            }

            @Override
            public String toString() {
                return "GPS PDOP";
            }
        },
        GPS_HDOP {
            @Override
            public int getStringResource() {
                return R.string.property_key_gps_hdop;
            }

            @Override
            public String toString() {
                return "GPS HDOP";
            }
        },
        GPS_VDOP {
            @Override
            public int getStringResource() {
                return R.string.property_key_gps_vdop;
            }

            @Override
            public String toString() {
                return "GPS VDOP";
            }
        },
        LAMBDA_VOLTAGE {
            @Override
            public int getStringResource() {
                return R.string.property_key_lambda_voltage;
            }

            @Override
            public String toString() {
                return "O2 Lambda Voltage";
            }
        },
        LAMBDA_VOLTAGE_ER {
            @Override
            public int getStringResource() {
                return R.string.property_key_lambda_voltage_er;
            }

            @Override
            public String toString() {
                return LAMBDA_VOLTAGE.toString().concat(" ER");
            }
        },
        LAMBDA_CURRENT {
            @Override
            public int getStringResource() {
                return R.string.property_key_lambda_current;
            }

            @Override
            public String toString() {
                return "O2 Lambda Current";
            }
        },
        LAMBDA_CURRENT_ER {
            @Override
            public int getStringResource() {
                return R.string.property_key_lambda_current_er;
            }

            @Override
            public String toString() {
                return LAMBDA_CURRENT.toString().concat(" ER");
            }
        },
        FUEL_SYSTEM_LOOP {
            @Override
            public int getStringResource() {
                return R.string.property_key_fuel_system_loop;
            }

            @Override
            public String toString() {
                return "Fuel System Loop";
            }
        },
        FUEL_SYSTEM_STATUS_CODE {
            @Override
            public int getStringResource() {
                return R.string.property_key_fuel_system_status_code;
            }

            @Override
            public String toString() {
                return "Fuel System Status Code";
            }
        },
        LONG_TERM_TRIM_1 {
            @Override
            public int getStringResource() {
                return R.string.property_key_long_term_trim_1;
            }

            @Override
            public String toString() {
                return "Long-Term Fuel Trim 1";
            }
        },
        SHORT_TERM_TRIM_1 {
            @Override
            public int getStringResource() {
                return R.string.property_key_short_term_trim_1;
            }

            @Override
            public String toString() {
                return "Short-Term Fuel Trim 1";
            }
        },
        MIN_ACCELERATION {
            @Override
            public int getStringResource() {
                return R.string.property_key_min_acceleration;
            }

            @Override
            public String toString() {
                return "Minimum Acceleration";
            }
        },
        MAX_ACCELERATION {
            @Override
            public int getStringResource() {
                return R.string.property_key_max_acceleration;
            }

            @Override
            public String toString() {
                return "Maximum Acceleration";
            }
        };


        @Override
        public int getUnitResource() {
            return -1;
        }
    }

    Map<String, PropertyKey> PropertyKeyValues = new HashMap<String,
            PropertyKey>() {
        {
            for (PropertyKey pk : PropertyKey.values()) {
                put(pk.toString(), pk);
            }
        }
    };

    Track.TrackId getTrackId();

    void setTrackId(Track.TrackId trackId);

    Double getLatitude();

    void setLatitude(double latitude);

    Double getLongitude();

    void setLongitude(double longitude);

    long getTime();

    void setTime(long time);

    Double getProperty(PropertyKey key);

    void setProperty(PropertyKey key, Double value);

    boolean hasProperty(PropertyKey key);

    Map<PropertyKey, Double> getAllProperties();

    void setAllProperties(Map<PropertyKey, Double> properties);

    Measurement carbonCopy();

    @Deprecated
    void reset();
}
