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
	private double fuelConsumption;
	private double maf;
	private double co2;

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

	/**
	 * @return the fuelConsumption
	 */
	public double getFuelConsumption() {
		return fuelConsumption;
	}

	/**
	 * @param fuelConsumption
	 *            the fuelConsumption to set
	 */
	public void setFuelConsumption(double fuelConsumption) {
		this.fuelConsumption = fuelConsumption;
	}

	/**
	 * @return the maf
	 */
	public double getMaf() {
		return maf;
	}

	// TODO the setmaf method should automatically set the fuel con and co2
	// values!

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
	public double getCo2() {
		return co2;
	}

	/**
	 * @param co2
	 *            the co2 to set
	 */
	public void setCo2(double co2) {
		this.co2 = co2;
	}

}
