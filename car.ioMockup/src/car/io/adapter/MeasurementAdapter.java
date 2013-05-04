package car.io.adapter;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;

/**
 * Interface of the measurement adapter. The measurement adapter is used to
 * upload the measurements to the server.
 * 
 * @author jakob
 * 
 */

public interface MeasurementAdapter {

	/**
	 * Uploads one measurement to the server
	 * 
	 * @param measurement
	 *            The measurement to upload
	 * @throws ClientProtocolException
	 *             If no connection could be established
	 * @throws IOException
	 *             If there is a problem with the measurements
	 */
	public void uploadMeasurement(Measurement measurement)
			throws ClientProtocolException, IOException;

}
