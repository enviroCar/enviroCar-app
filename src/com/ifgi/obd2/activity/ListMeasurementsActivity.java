package com.ifgi.obd2.activity;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.client.ClientProtocolException;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ifgi.obd2.R;
import com.ifgi.obd2.adapter.DbAdapter;
import com.ifgi.obd2.adapter.DbAdapterLocal;
import com.ifgi.obd2.adapter.Measurement;
import com.ifgi.obd2.adapter.MeasurementAdapter;
import com.ifgi.obd2.adapter.MeasurementAdapterLocal;

/**
 * Lists all measurement in a List Activity. Press MENU to upload all
 * measurements or delete all measurements
 * 
 * @author jakob
 * 
 */

public class ListMeasurementsActivity extends ListActivity {

	// DB Adapter for SQLite

	private DbAdapter dbAdapter;

	// MeasurementAdapter for server connection

	private MeasurementAdapter measurementAdapter;

	// List with all measurements

	private ArrayList<Measurement> allMeasurementsList;

	private String[] measurements_short;
	private final int DELETE_ALL = 3;
	private final int UPLOAD_ALL = 4;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initDbAdapter();

		fillListWithMeasurements();

		measurementAdapter = new MeasurementAdapterLocal();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, DELETE_ALL, 0, "Delete All");
		menu.add(0, UPLOAD_ALL, 0, "Upload All");
		return super.onCreateOptionsMenu(menu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case DELETE_ALL:
			dbAdapter.deleteAllMeasurements();
			fillListWithMeasurements();
			return true;
		case UPLOAD_ALL:
			try {
				uploadAllMeasurements();
				dbAdapter.deleteAllMeasurements();
				fillListWithMeasurements();
			} catch (ClientProtocolException e) {
				Toast.makeText(getApplicationContext(), "Upload failed",
						Toast.LENGTH_SHORT).show();
				Log.e("obd2", "upload failed");
				e.printStackTrace();
			} catch (IOException e) {
				Toast.makeText(getApplicationContext(), "Upload failed",
						Toast.LENGTH_SHORT).show();
				Log.e("obd2", "upload failed2");
				e.printStackTrace();
			}

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Uploads all local measurements to the server
	 * 
	 * @throws NoServerResponseException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */

	private void uploadAllMeasurements() throws ClientProtocolException,
			IOException {

		for (Measurement measurement : allMeasurementsList) {

			Log.e("obd2", measurement.toString());

			measurementAdapter.uploadMeasurement(measurement);
		}

	}

	/**
	 * Helper method that fills the ListActivity's list with all measurement ids
	 */
	private void fillListWithMeasurements() {

		allMeasurementsList = dbAdapter.getAllMeasurements();

		// String List for the list itself

		measurements_short = new String[allMeasurementsList.size()];

		for (int i = 0; i < measurements_short.length; i++) {
			measurements_short[i] = "Own Measurement "
					+ allMeasurementsList.get(i).getId();
		}

		if (allMeasurementsList != null) {

			// Generate List

			setListAdapter(new ArrayAdapter<String>(this,
					R.layout.listmeasurements, measurements_short));
			ListView lv = getListView();
			lv.setTextFilterEnabled(true);

			// Start the MeasurementDisplay for the selected entry

			lv.setOnItemClickListener(new OnItemClickListener() {

				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {

					String selectedId = ((TextView) view).getText().toString();

					String[] split = selectedId.split(" ");

					selectedId = split[split.length - 1];

					for (Measurement measurement : allMeasurementsList) {

						if (Integer.valueOf(selectedId) == measurement.getId()) {

							Intent i;

							// Start the intent and put the selected id into the
							// bundle

							i = new Intent(ListMeasurementsActivity.this,
									MeasurementDisplay.class);
							Bundle bundle = new Bundle();
							bundle.putString("id",
									String.valueOf(measurement.getId()));
							i.putExtras(bundle);
							startActivity(i);
						}
					}
				}
			});

		}

	}

	/**
	 * Helper Method to open DB Connection
	 */
	private void initDbAdapter() {
		dbAdapter = new DbAdapterLocal(getApplicationContext());
		dbAdapter.open();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
	}

}
