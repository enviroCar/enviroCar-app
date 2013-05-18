package car.io.adapter;

import java.util.ArrayList;

import car.io.exception.FuelConsumptionException;
import car.io.exception.MeasurementsException;

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
	 * Constructor for creating a Track from the Database. Use this constructor
	 * when you want to rebuild tracks from the database.
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
	 * Constructor for creating "fresh" new track. Use this for new measurements
	 * that were captured from the OBD-II adapter.
	 */
	public Track(String vin, String fuelType, DbAdapter dbAdapter) {
		this.vin = vin;
		this.name = ""; // TODO current date
		this.description = "";
		this.carManufacturer = ""; // TODO decode vin
		this.carModel = "";
		this.fuelType = fuelType;
		this.measurements = new ArrayList<Measurement>();
		this.dbAdapter = dbAdapter;
		id = String.valueOf(dbAdapter.insertTrack(this));
	}

	/**
	 * Updates the Track in the database
	 * @return
	 */
	public boolean commitTrackToDatabase(){
		return dbAdapter.updateTrack(this);
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

	/**
	 * get the time where the track started
	 * 
	 * @return start time of track as unix long
	 * @throws MeasurementsException
	 */
	public long getStartTime() throws MeasurementsException {
		if (this.getMeasurements().size() > 0)
			return this.getMeasurements().get(0).getMeasurementTime();
		else
			throw new MeasurementsException("No measurements in the track");
	}

	/**
	 * get the time where the track ended
	 * 
	 * @return end time of track as unix long
	 * @throws MeasurementsException
	 */
	public long getEndTime() throws MeasurementsException {
		if (this.getMeasurements().size() > 0)
			return this.getMeasurements()
					.get(this.getMeasurements().size() - 1)
					.getMeasurementTime();
		else
			throw new MeasurementsException("No measurements in the track");
	}

	/**
	 * Sets the measurements with an arraylist of measurements
	 * 
	 * @param measurements
	 *            the measurements of a track
	 */
	public void setMeasurementsAsArrayList(ArrayList<Measurement> measurements) {
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
		measurement.setTrack(Track.this);
		this.measurements.add(measurement);
		dbAdapter.insertMeasurement(measurement);
	}

	/**
	 * Returns the number of measurements of this track
	 * 
	 * @return
	 */
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

	/**
	 * get the VIN
	 * 
	 * @return
	 */
	public String getVin() {
		return vin;
	}

	/**
	 * set the vin
	 * 
	 * @param vin
	 */
	public void setVin(String vin) {
		this.vin = vin;
	}

	/**
	 * set the fuel type
	 * 
	 * @param fuelType
	 */
	public void setFuelType(String fuelType) {
		this.fuelType = fuelType;
	}

	/**
	 * get the fuel type
	 * 
	 * @return
	 */
	public String getFuelType() {
		return fuelType;
	}

	/**
	 * Returns the fuel consumption for a measurement
	 * 
	 * @param measurement
	 *            The measurement with the fuel consumption
	 * @return The fuel consumption in l/s
	 * @throws FuelConsumptionException
	 */

	public double getFuelConsumptionOfMeasurement(int measurement)
			throws FuelConsumptionException {

		// TODO make this in l/100km (include speed information)

		Measurement m = getMeasurements().get(measurement);

		double maf = m.getMaf();

		if (this.fuelType.equals("Gasoline")) {
			return (maf / 14.7) / 747;
		} else if (this.fuelType.equals("Diesel")) {
			return (maf / 14.5) / 832;
		} else
			throw new FuelConsumptionException();

	}

	/**
	 * Returns the Co2 emission of a measurement
	 * 
	 * @param measurement
	 * @return co2 emission in kg/s
	 * @throws FuelConsumptionException
	 */
	public double getCO2EmissionOfMeasurement(int measurement)
			throws FuelConsumptionException {

		// TODO change unit to kg/km (include speed information)

		double fuelCon;
		fuelCon = getFuelConsumptionOfMeasurement(measurement);

		if (this.fuelType.equals("Gasoline")) {
			return fuelCon * 2.35;
		} else if (this.fuelType.equals("Diesel")) {
			return fuelCon * 2.65;
		} else
			throw new FuelConsumptionException();

	}

}
