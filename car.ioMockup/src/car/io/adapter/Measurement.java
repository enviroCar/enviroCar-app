package car.io.adapter;

import car.io.exception.LocationInvalidException;

/**
 * Measurement class that contains all the measured values
 * 
 * @author jakob
 * 
 */

public class Measurement {

	// All measurement values

	private int id;
	private float latitude;
	private float longitude;
	private long measurementTime;
	private int rpm;
	private int speed;
	private double maf;
	private Track track;

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
	
	//TODO: set track with constructor?

	public Measurement(float latitude, float longitude)
			throws LocationInvalidException {
		if (latitude != 0.0 && longitude != 0.0) {
			this.latitude = latitude;
			this.longitude = longitude;
			this.measurementTime = System.currentTimeMillis();
		} else {
			throw new LocationInvalidException();
		}
	}

	// TODO: implement this
	/**
	 * @return the fuelConsumption
	 */
	public double getFuelConsumption() {
		return 0.0;
	}

	/**
	 * @return the maf
	 */
	public double getMaf() {
		return maf;
	}

	/**
	 * @param maf
	 *            the maf to set
	 */
	public void setMaf(double maf) {
		this.maf = maf;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the latitude
	 */
	public float getLatitude() {
		return latitude;
	}

	/**
	 * @param latitude
	 *            the latitude to set
	 */
	public void setLatitude(float latitude) {
		this.latitude = latitude;
	}

	/**
	 * @return the longitude
	 */
	public float getLongitude() {
		return longitude;
	}

	/**
	 * @param longitude
	 *            the longitude to set
	 */
	public void setLongitude(float longitude) {
		this.longitude = longitude;
	}

	/**
	 * @return the measurementTime
	 */
	public long getMeasurementTime() {
		return measurementTime;
	}

	/**
	 * @param measurementTime
	 *            the measurementTime to set
	 */
	public void setMeasurementTime(long measurementTime) {
		this.measurementTime = measurementTime;
	}

	/**
	 * @return the rpm
	 */
	public int getRpm() {
		return rpm;
	}

	/**
	 * @param rpm
	 *            the rpm to set
	 */
	public void setRpm(int rpm) {
		this.rpm = rpm;
	}

	/**
	 * @return the speed
	 */
	public int getSpeed() {
		return speed;
	}

	/**
	 * @param speed
	 *            the speed to set
	 */
	public void setSpeed(int speed) {
		this.speed = speed;
	}

	/**
	 * @return the co2
	 */
	// TODO: implement this
	public double getCo2() {
		return 0.0;
	}

	/**
	 * @return the track
	 */
	public Track getTrack() {
		return track;
	}

	/**
	 * @param track the track to set
	 */
	public void setTrack(Track track) {
		this.track = track;
	}

}
