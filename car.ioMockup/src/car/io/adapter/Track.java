package car.io.adapter;

import java.util.ArrayList;

public class Track {

	private String id;
	private String name;
	private String description;
	private ArrayList<Measurement> measurements;
	private String carManufacturer;
	private String carModel;
	private String vin;
	private String fuelType;

	private DbAdapter dbAdapter;

	/**
	 * Constructor for creating a Track from the Database
	 */
	public Track(String id) {
		this.id = id;
		this.name = "";
		this.description = "";
		this.carManufacturer = "";
		this.carModel = "";
		this.vin = "";
		this.fuelType = "";
		this.measurements = new ArrayList<Measurement>();
	}

	/**
	 * Constructor for creating "fresh" new track
	 */
	public Track(String vin, String fuelType, DbAdapter dbAdapter) {
		this.vin = vin;
		this.name = "heute"; // TODO current date
		this.description = "";
		this.carManufacturer = ""; // TODO decode vin
		this.carModel = "";
		this.fuelType = fuelType;
		this.measurements = new ArrayList<Measurement>();

		id = String.valueOf(dbAdapter.insertTrack(this));
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description
	 *            the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the carManufacturer
	 */
	public String getCarManufacturer() {
		return carManufacturer;
	}

	/**
	 * @param carManufacturer
	 *            the carManufacturer to set
	 */
	public void setCarManufacturer(String carManufacturer) {
		this.carManufacturer = carManufacturer;
	}

	/**
	 * @return the carModel
	 */
	public String getCarModel() {
		return carModel;
	}

	/**
	 * @param carModel
	 *            the carModel to set
	 */
	public void setCarModel(String carModel) {
		this.carModel = carModel;
	}

	/**
	 * @return the measurements
	 */
	public ArrayList<Measurement> getMeasurements() {
		return measurements;
	}

	// TODO throw exceptions here

	public long getStartTime() {
		if (this.getMeasurements().size() > 0)
			return this.getMeasurements().get(0).getMeasurementTime();
		else
			return 0;
	}

	// TODO throw exceptions here

	public long getEndTime() {
		if (this.getMeasurements().size() > 0)
			return this.getMeasurements()
					.get(this.getMeasurements().size() - 1)
					.getMeasurementTime();
		else
			return 999999999;
	}

	public void insertMeasurement(ArrayList<Measurement> measurements) {
		this.measurements = measurements;
	}

	/**
	 * Use this method only to insert "fresh" measurements, not to recreate a
	 * Track from the database Use
	 * {@code insertMeasurement(ArrayList<Measurement> measurements)} instead
	 * Inserts measurments into the Track and into the database!
	 * 
	 * @param measurement
	 */
	public void addMeasurement(Measurement measurement) {
		measurement.setTrack(this);
		this.measurements.add(measurement);
		dbAdapter.insertMeasurement(measurement);
	}

	public int getNumberOfMeasurements() {
		return this.measurements.size();
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	public String getVin() {
		return vin;
	}

	public void setVin(String vin) {
		this.vin = vin;
	}

	public void setFuelType(String fuelType) {
		this.fuelType = fuelType;
	}

	public String getFuelType() {
		return fuelType;
	}

}
