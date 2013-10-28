/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */

package org.envirocar.app.storage;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.envirocar.app.exception.LocationInvalidException;
import org.envirocar.app.util.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Measurement class that contains all the measured values
 * 
 * @author jakob
 * 
 */

public class Measurement {

	// All measurement values
	public enum PropertyKey {
		SPEED {
			public String toString() {
		        return "Speed";
		    }
		},
		MAF {
			public String toString() {
		        return "MAF";
		    } 
		}, 
		CALCULATED_MAF {
			public String toString() {
		        return "Calculated MAF";
		    } 
		}, 
		RPM {
			public String toString() {
		        return "Rpm";
		    } 
		}, 
		INTAKE_TEMPERATURE {
			public String toString() {
		        return "Intake Temperature";
		    } 
		}, 
		INTAKE_PRESSURE {
			public String toString() {
		        return "Intake Pressure";
		    } 
		},
		CO2 {
			public String toString() {
		        return "CO2";
		    } 
		},
		CONSUMPTION {
			public String toString() {
		        return "Consumption";
		    } 
		},
		THROTTLE_POSITON {
			@Override
			public String toString() {
				return "Throttle Position";
			}
		},
		ENGINE_LOAD {
			@Override
			public String toString() {
				return "Engine Load";
			}
		},
		GPS_ACCURACY {
			@Override
			public String toString() {
				return "GPS Accuracy";
			}
		},
		GPS_SPEED {
			@Override
			public String toString() {
				return "GPS Speed";
			}
		},
		GPS_BEARING {
			@Override
			public String toString() {
				return "GPS Bearing";
			}
		},
		GPS_ALTITUDE {
			@Override
			public String toString() {
				return "GPS Altitude";
			}
		},
		GPS_PDOP {
			@Override
			public String toString() {
				return "GPS PDOP";
			}
		},
		GPS_HDOP {
			@Override
			public String toString() {
				return "GPS HDOP";
			}
		},
		GPS_VDOP {
			@Override
			public String toString() {
				return "GPS VDOP";
			}
		},
		LAMBDA_VOLTAGE {
			@Override
			public String toString() {
				return "O2 Lambda Voltage";
			}
		},
		LAMBDA_VOLTAGE_ER {
			@Override
			public String toString() {
				return LAMBDA_VOLTAGE.toString().concat(" ER");
			}
		},
		LAMBDA_CURRENT {
			@Override
			public String toString() {
				return "O2 Lambda Current";
			}
		},
		LAMBDA_CURRENT_ER {
			@Override
			public String toString() {
				return LAMBDA_CURRENT.toString().concat(" ER");
			}
		},
		FUEL_SYSTEM_LOOP {
			@Override
			public String toString() {
				return "Fuel System Loop";
			}
		},
		FUEL_SYSTEM_STATUS_CODE {
			@Override
			public String toString() {
				return "Fuel System Status Code";
			}
		},
		LONG_TERM_TRIM_1 {
			@Override
			public String toString() {
				return "Long-Term Fuel Trim 1";
			}
		},
		SHORT_TERM_TRIM_1 {
			@Override
			public String toString() {
				return "Short-Term Fuel Trim 1";
			}
		}
		
	}
	
	public static final Map<String, PropertyKey> PropertyKeyValues = new HashMap<String, PropertyKey>();
	
	static {
		for (PropertyKey pk : PropertyKey.values()) {
			PropertyKeyValues.put(pk.toString(), pk);
		}
	}

	private double latitude;
	private double longitude;
	private long time;
	private Track track;
	
	private Map<PropertyKey, Double> propertyMap = new HashMap<PropertyKey, Double>();

	/**
	 * Create a new measurement. Latitude AND longitude are not allowed to both
	 * equal 0.0. This method also sets the measurement time according to the
	 * System.currentTimeMillis() method.
	 * 
	 * @param latitude
	 *            Latitude of the measurement (WGS 84)
	 * @param longitude
	 *            Longitude of the measurement (WGS 84)
	 * @throws LocationInvalidException
	 *             If latitude AND longitude equal 0.0
	 */

	public Measurement(double latitude, double longitude) {
			this.latitude = latitude;
			this.longitude = longitude;
			this.time = System.currentTimeMillis();
	}
	
	public Double getProperty(PropertyKey key) {
		return (Double) propertyMap.get(key);
	}
	
	public Map<PropertyKey, Double> getAllProperties() {
		return propertyMap;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Measurement [");
		sb.append("latitude=");
		sb.append(latitude);
		sb.append(", longitude=");
		sb.append(longitude);
		sb.append(", time=");
		sb.append(time);
		sb.append(", ");
		for (PropertyKey key : propertyMap.keySet()) {
			sb.append(key.toString());
			sb.append("=");
			sb.append(propertyMap.get(key));
			sb.append(", ");
		}
		return sb.toString();
	}

	/**
	 * @return the latitude
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * @param latitude
	 *            the latitude to set
	 */
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	/**
	 * @return the longitude
	 */
	public double getLongitude() {
		return longitude;
	}

	/**
	 * @param longitude
	 *            the longitude to set
	 */
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	/**
	 * @return the measurementTime
	 */
	public long getTime() {
		return time;
	}

	/**
	 * @param measurementTime
	 *            the measurementTime to set
	 */
	public void setTime(long time) {
		this.time = time;
	}

	/**
	 * @return the track
	 */
	public Track getTrack() {
		return track;
	}

	/**
	 * @param track
	 *            the track to set
	 */
	public void setTrack(Track track) {
		this.track = track;
	}
	
	public boolean hasProperty(PropertyKey key) {
		return propertyMap.containsKey(key);
	}

	public void setProperty(PropertyKey key, Double value) {
		propertyMap.put(key, value);
	}


	public static Measurement fromJson(JSONObject measurementJsonObject) throws JSONException, ParseException {
		JSONArray coords = measurementJsonObject.getJSONObject("geometry").getJSONArray("coordinates");
		Measurement result = new Measurement(
				Float.valueOf(coords.getString(1)),
				Float.valueOf(coords.getString(0)));
		
		JSONObject properties = measurementJsonObject.getJSONObject("properties");
		result.setTime(Util.isoDateToLong((properties.getString("time"))));
		JSONObject phenomenons = properties.getJSONObject("phenomenons");
		Iterator<?> it = phenomenons.keys();
		String key;
		while (it.hasNext()) {
			key = (String) it.next();
			if (PropertyKeyValues.keySet().contains(key)) {
				Double value = ((JSONObject) phenomenons.get(key)).getDouble("value"); 
				result.setProperty(PropertyKeyValues.get(key), value);
			}
		}
		
		return result;
	}
}
