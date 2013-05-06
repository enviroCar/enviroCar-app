package car.io.adapter;

import java.util.ArrayList;

/**
 * Track that consists of measurements.
 * 
 * @author jakob
 * 
 */
public class Track {

	private String name;

	private ArrayList<Measurement> measurements;

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
	 * @return the measurements
	 */
	public ArrayList<Measurement> getMeasurements() {
		return measurements;
	}

	/**
	 * @param measurements
	 *            the measurements to set
	 */
	public void setMeasurements(ArrayList<Measurement> measurements) {
		this.measurements = measurements;
	}

	/**
	 * Add measurement to track
	 * 
	 * @param measurement
	 */
	public void addMeasurementToTrack(Measurement measurement) {
		this.measurements.add(measurement);
	}

	/**
	 * Get number of measurements in the track
	 * 
	 * @return
	 */
	public int getNumberOfMeasurements() {
		return this.measurements.size();
	}

	/**
	 * Returns the measurement with the specified id
	 * 
	 * @param id
	 * @return
	 */
	public Measurement getMeasurement(int id) {
		return this.measurements.get(id);
	}

}
