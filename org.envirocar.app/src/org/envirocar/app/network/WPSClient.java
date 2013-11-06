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
package org.envirocar.app.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import org.envirocar.app.exception.MeasurementsException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.storage.Track;
import org.envirocar.app.util.Util;

import android.os.AsyncTask;

public class WPSClient {

	private static final Logger logger = Logger.getLogger(WPSClient.class);

	/**
	 * Make a WPS request for calculating the fuel costs of a track.
	 * 
	 * the callback will be called with the result or {@link Double#NaN} if the
	 * request failed for whatever reason.
	 * 
	 * @param track the track for calculating the fule costs
	 * @param callback called after finishing the request
	 */
	public static void calculateFuelCosts(final Track track,
			final ResultCallback<Double> callback) {

		if (track == null || track.getCar() == null
				|| track.getCar().getFuelType() == null) {
			callback.onResultAvailable(Double.NaN);
			return;
		}

		AsyncTask<Void, Void, Double> task = new AsyncTask<Void, Void, Double>() {

			@Override
			protected Double doInBackground(Void... params) {
				Thread.currentThread()
						.setName(
								"TrackList-WPSCaller-"
										+ Thread.currentThread().getId());
				try {

					URL url = new URL(
							"http://geoprocessing.demo.52north.org:8080/wps/WebProcessingService?Service=WPS&Request=Execute&Version=1.0.0&Identifier=org.n52.wps.extension.GetFuelPriceProcess&DataInputs=fuelType="
									+ track.getCar().getFuelType()
									+ "&RawDataOutput=fuelPrice");

					BufferedReader reader = new BufferedReader(
							new InputStreamReader(url.openStream()));

					String content = "";
					String line = "";

					while ((line = reader.readLine()) != null) {
						content = content.concat(line);
					}
					return Double.parseDouble(content);
				} catch (Exception e) {
					logger.warn(e.getMessage(), e);
					return Double.NaN;
				}

			}

			@Override
			protected void onPostExecute(Double executeResult) {

				double result = executeResult;
				if (!Double.isNaN(executeResult)) {
					try {
						result = executeResult
								* (track.getLiterPerHundredKm() / 100)
								* track.getLengthOfTrack();

					} catch (MeasurementsException e) {
						logger.warn(e.getMessage(), e);
					}
				}

				callback.onResultAvailable(result);

			}

		};
		
		Util.execute(task);
	}

	public static interface ResultCallback<T> {

		public void onResultAvailable(T result);

	}

}
