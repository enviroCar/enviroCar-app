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

import org.envirocar.core.delete.Position;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.util.TrackMetadata;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;


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
	 * @deprecated implementations should take care of that on their own
	 */
	@Deprecated
	public DbAdapter open();

	/**
	 * Close the DB connection. Should be called when the app stops
	 */
	public void close();

	/**
	 * Check whether the database is opened at the moment.
	 * @return true if db is open.
	 */
	public boolean isOpen();

	/**
	 * Inserts a measurements into the database
	 * 
	 * @param measurement
	 *            The measurement that should be inserted
	 * @throws TrackAlreadyFinishedException
	 * @throws MeasurementSerializationException 
	 */
	public void insertNewMeasurement(Measurement measurement) throws TrackAlreadyFinishedException, MeasurementSerializationException;

	/**
	 * Inserts a track into the database
	 * 
	 * @param track
	 *            The track that should be inserted
	 * @return the id of the track that has been inserted
	 */
	public long insertTrack(Track track);

	/**
	 * Inserts a track into the database
	 * 
	 * @param track
	 *            The track that should be inserted
	 * @param remote
	 * 				if the track is a remote (=server, already finished) track
	 * @return the id of the track that has been inserted
	 */
	public long insertTrack(Track track, boolean remote);
	
	/**
	 * Updates a Track in the database
	 * 
	 * @param track
	 *            the track to update
	 * @return true or false if the query was successful
	 */
	public boolean updateTrack(Track track);

	
	/**
	 * An implementation shall return the current track that
	 * measurements should be appended to.
	 * It shall determine if using a non-finalized track
	 * is reasonable (based on time and space constraints, using
	 * the system time and the provided currentLocation)
	 * If there is no non-finalized track or appending is not
	 * reasonable, a new track shall be created.
	 * 
	 * @param currentLocation the current location
	 * @return the current active track as reference via TrackId
	 */
	public Track.TrackId getActiveTrackReference(Position currentLocation);
	
	/**
	 * Returns all tracks as an ArrayList<Track>
	 * 
	 * @return All tracks in an ArrayList
	 */
	public ArrayList<Track> getAllTracks();

	/**
	 * @param lazyMeasurements if true, an implementation shall return
	 * {@link Track} objects that load their measurements in lazy fashion
	 * @return all tracks
	 */
	public List<Track> getAllTracks(boolean lazyMeasurements);



	public Observable<Track> getTrackObservable(boolean lazyMeasurements);
	
	/**
	 * Returns one track specified by the id
	 * 
	 * @param id
	 *            The id of the track that should be returned
	 * @return The desired track or null if it does not exist
	 */
	public Track getTrack(Track.TrackId id);
	
	/**
	 * Returns one track specified by the id
	 * 
	 * @param id the tracks internal id
	 * @param lazyMeasurements if true, an implementation shall return a
	 * {@link Track} that loads its measurements in lazy fashion
	 * @return the desired track
	 */
	public Track getTrack(Track.TrackId id, boolean lazyMeasurements);
	
	/**
	 * Returns <code>true</code> if a track with the given id is in the Database
	 * 
	 * @param id
	 * 		The id id ot the checked track
	 * @return exists a track with the id
	 */
	public boolean hasTrack(Track.TrackId id);

	/**
	 * Deletes all tracks and measurements in the database
	 */
	public void deleteAllTracks();

	/**
	 * Returns the number of stored tracks in the SQLite database
	 */
	public int getNumberOfStoredTracks();

	/**
	 * Retruns the track that was last inserted into the database
	 * 
	 * @return the latest track of the DB or null if there are no tracks
	 */
	public Track getLastUsedTrack();

	
	/**
	 * @see #getLastUsedTrack()
	 * 
	 * @param lazyMeasurements if the measurements should be deserialized
	 * @return see {@link #getLastUsedTrack()}
	 */
	public Track getLastUsedTrack(boolean lazyMeasurements);
	
	/**
	 * Delete track specified by id.
	 * 
	 * @param id
	 *            id of the track to be deleted.
	 */
	public void deleteTrack(Track.TrackId id);

	public int getNumberOfRemoteTracks();

	public int getNumberOfLocalTracks();

	/**
	 * an implementation shall delete (!) all
	 * local tracks from the underlying persistence
	 * layer.
	 * 
	 * Friendly warning: every local track (= not yet
	 * uploaded) will be lost irreversible. 
	 */
	public void deleteAllLocalTracks();

	/**
	 * an implementation shall remove
	 * all local representations of remote tracks from
	 * the underlying persistence layer.
	 */
	public void deleteAllRemoteTracks();

	public List<Track> getAllLocalTracks();

	public List<Track> getAllLocalTracks(boolean lazyMeasurements);

	public List<Track> getAllRemoteTracks();

	public List<Track> getAllRemoteTracks(boolean lazyMeasurements);


	public Track createNewTrack();


	public Track finishCurrentTrack();

	/**
	 * an implementation shall return all meaasurements
	 * for the given track.
	 * 
	 * @param track the track object
	 * @return the list of Measurements
	 */
	public List<Measurement> getAllMeasurementsForTrack(Track track);

	/**
	 * an implementation shall update the ID
	 * of all Track's cars which currently have the currentId
	 * and update it to newId.
	 * 
	 * @param currentId
	 * @param newId
	 */
	public void updateCarIdOfTracks(String currentId, String newId);

	void insertMeasurement(Measurement measurement) throws TrackAlreadyFinishedException, MeasurementSerializationException;

	void insertMeasurement(Measurement measurement, boolean ignoreFinished)
			throws MeasurementSerializationException, TrackAlreadyFinishedException;

	public void updateTrackMetadata(Track.TrackId trackId, TrackMetadata trackMetadata);

	public void transitLocalToRemoteTrack(Track track, String remoteId);

	/**
	 * use this method to load measurements for a track that
	 * is marked as lazy loaded.
	 * 
	 * An implementation shall set the field
	 * to false after loading and setting the measurements.
	 * 
	 * @param t the track
	 */
	public void loadMeasurements(Track t);

	public void setConnectedOBDDevice(TrackMetadata obdDeviceMetadata);


	
}
