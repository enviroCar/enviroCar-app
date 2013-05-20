package car.io.adapter;

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
	
	public boolean isOpen();

	/**
	 * Inserts a measurements into the database
	 * 
	 * @param measurement
	 *            The measurement that should be inserted
	 */
	public void insertMeasurement(Measurement measurement);

	/**
	 * Inserts a track into the database
	 * 
	 * @param track
	 *            The track that should be inserted
	 * @return the id of the track that has been inserted
	 */
	public long insertTrack(Track track);

	/**
	 * Updates a Track in the database
	 * 
	 * @param track
	 *            the track to update
	 * @return true or false if the query was successful
	 */
	public boolean updateTrack(Track track);

	/**
	 * Returns all tracks as an ArrayList<Track>
	 * 
	 * @return All tracks in an ArrayList
	 */
	public ArrayList<Track> getAllTracks();

	/**
	 * Returns one track specified by the id
	 * 
	 * @param id
	 *            The id of the track that should be returned
	 * @return The desired track
	 */
	public Track getTrack(String id);

	/**
	 * Deletes all tracks and measurements in the database
	 */
	public void deleteAllTracks();

	/**
	 * Returns the number of stored tracks in the SQLite database
	 */
	public int getNumberOfStoredTracks();

}
