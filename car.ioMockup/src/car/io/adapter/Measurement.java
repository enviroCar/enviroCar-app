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
	private double throttlePosition;
	private int rpm;
	private int speed;
	private String fuelType;
	private double engineLoad;
	private double fuelConsumption;
	private int intakePressure;
	private int intakeTemperature;
	private double shortTermTrimBank1;
	private double longTermTrimBank1;
	private double maf;
	private String car;

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Measurement [id=" + id + ", latitude=" + latitude
				+ ", longitude=" + longitude + ", measurementTime="
				+ measurementTime + ", throttlePosition=" + throttlePosition
				+ ", rpm=" + rpm + ", speed=" + speed + ", fuelType="
				+ fuelType + ", engineLoad=" + engineLoad
				+ ", fuelConsumption=" + fuelConsumption + ", intakePressure="
				+ intakePressure + ", intakeTemperature=" + intakeTemperature
				+ ", shortTermTrimBank1=" + shortTermTrimBank1
				+ ", longTermTrimBank1=" + longTermTrimBank1 + ", maf=" + maf
				+ ", car=" + car + "]";
	}

	/**
	 * @return the car
	 */
	public String getCar() {
		return car;
	}

	/**
	 * @param car
	 *            the car to set
	 */
	public void setCar(String car) {
		this.car = car;
	}

	/**
	 * @return the engineLoad
	 */
	public double getEngineLoad() {
		return engineLoad;
	}

	/**
	 * @param engineLoad
	 *            the engineLoad to set
	 */
	public void setEngineLoad(double engineLoad) {
		this.engineLoad = engineLoad;
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
	 * @return the intakePressure
	 */
	public int getIntakePressure() {
		return intakePressure;
	}

	/**
	 * @param intakePressure
	 *            the intakePressure to set
	 */
	public void setIntakePressure(int intakePressure) {
		this.intakePressure = intakePressure;
	}

	/**
	 * @return the intakeTemperature
	 */
	public int getIntakeTemperature() {
		return intakeTemperature;
	}

	/**
	 * @param intakeTemperature
	 *            the intakeTemperature to set
	 */
	public void setIntakeTemperature(int intakeTemperature) {
		this.intakeTemperature = intakeTemperature;
	}

	/**
	 * @return the shortTermTrimBank1
	 */
	public double getShortTermTrimBank1() {
		return shortTermTrimBank1;
	}

	/**
	 * @param shortTermTrimBank1
	 *            the shortTermTrimBank1 to set
	 */
	public void setShortTermTrimBank1(double shortTermTrimBank1) {
		this.shortTermTrimBank1 = shortTermTrimBank1;
	}

	/**
	 * @return the longTermTrimBank1
	 */
	public double getLongTermTrimBank1() {
		return longTermTrimBank1;
	}

	/**
	 * @param longTermTrimBank1
	 *            the longTermTrimBank1 to set
	 */
	public void setLongTermTrimBank1(double longTermTrimBank1) {
		this.longTermTrimBank1 = longTermTrimBank1;
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
	 * @return the throttlePosition
	 */
	public double getThrottlePosition() {
		return throttlePosition;
	}

	/**
	 * @param throttlePosition
	 *            the throttlePosition to set
	 */
	public void setThrottlePosition(double throttlePosition) {
		this.throttlePosition = throttlePosition;
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
	 * @return the fuelType
	 */
	public String getFuelType() {
		return fuelType;
	}

	/**
	 * @param fuelType
	 *            the fuelType to set
	 */
	public void setFuelType(String fuelType) {
		this.fuelType = fuelType;
	}

}
