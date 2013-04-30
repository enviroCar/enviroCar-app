package com.ifgi.obd2.adapter;

import java.util.ArrayList;

/**
 * DB Adapter Interface that saves measurements in a local SQLite Database
 * 
 * @author jakob
 * 
 */

public interface DbAdapter {

	/**
	 * Method to open the DB connection
	 * 
	 * @return DbAdapter Object that can be used to call the other methods
	 */

	public DbAdapter open();

	/**
	 * Close the DB connection. Should be called when the app stops
	 */

	public void close();

	/**
	 * Inserts a measurements into the database
	 * 
	 * @param measurement
	 *            The measurement that should be inserted
	 */

	public void insertMeasurement(Measurement measurement);

	/**
	 * Returns all measurements as an ArrayList<Measurement>
	 * 
	 * @return All measurements in an ArrayList
	 */

	public ArrayList<Measurement> getAllMeasurements();

	/**
	 * Returns one measurement specified by the id
	 * 
	 * @param id
	 *            The id of the measurement that should be returned
	 * @return The desired measurement
	 */

	public Measurement getMeasurement(int id);

	/**
	 * Deletes all measurements in the measurements database
	 */

	public void deleteAllMeasurements();

	/**
	 * Returns the number of stored measurements in the SQLite database<
	 */
	public int getNumberOfStoredMeasurements();

}
