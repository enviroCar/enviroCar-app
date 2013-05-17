package car.io.adapter;

import java.util.ArrayList;

public class Track {

	private int id;
	private String name;
	private String description;
	private ArrayList<Measurement> measurements;
	private String carManufacturer;
	private String carModel;

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

	// TODO Implement this with shared prefs

	public String getFuelType() {
		return "FuelType";
	}

	public void addMeasurement(Measurement measurement) {
		this.measurements.add(measurement);
	}

	public int getNumberOfMeasurements() {
		return this.measurements.size();
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

}
