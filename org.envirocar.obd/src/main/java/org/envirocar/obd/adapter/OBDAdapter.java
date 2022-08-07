/**
 * Copyright (C) 2013 - 2021 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.obd.adapter;

import org.envirocar.obd.commands.response.DataResponse;

import java.io.InputStream;
import java.io.OutputStream;

import io.reactivex.rxjava3.core.Observable;


/**
 * Interface for a OBD connector. It can provide device specific
 * command requests and initialization sequences.
 * 
 * @author matthes rieke
 *
 */
public interface OBDAdapter {


    enum ConnectionState {
		
		/**
		 * used to indicate a state when the connector could
		 * not understand any response received
		 */
		DISCONNECTED,
		
		/**
		 * used to indicate a state when the connector understood
		 * at least one command. Return this state only if the
		 * adapter is sure, that it can interact with the device
		 * - but the device yet did not return measurements
		 */
		CONNECTED,

		/**
		 * used to indicate a state where the connector received
		 * a parseable measurement
		 */
		VERIFIED
	}

	/**
	 * Initialize the connection.
	 *
	 * @param is the inpusttream
	 * @param os this outputstream
	 * @return an observable that calls onNext on successful connection
	 */
	Observable<Boolean> initialize(InputStream is, OutputStream os);

	/**
	 * Start the actual data collection
	 *
	 * @return an observable that provides data responses
	 */
	Observable<DataResponse> observe();

	/**
	 * An implementation shall return true if it 
	 * might support the given bluetooth device.
	 * 
	 * @param deviceName the bluetooth device name
	 * @return if it suggests support for the device
	 */
	boolean supportsDevice(String deviceName);

	/**
	 * This method is used to decide if another adapter implementation is
	 * worth a try. If an adapter verified a connection (e.g. via special metadata
	 * responses) and is sure that it is the correct adapter, it shall return
	 * true. Then no other adapter will be tried in order to do not waste time.
	 *
	 * @return true if the adapter has determined a compatible device
	 */
	boolean hasCertifiedConnection();

    /**
     *
     * @return the time (in ms) the adapter might take to connect to the OBD layer
     */
    long getExpectedInitPeriod();

	String getStateMessage();
}
