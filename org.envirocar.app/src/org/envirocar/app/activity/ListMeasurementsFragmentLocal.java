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

package org.envirocar.app.activity;

import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import org.envirocar.app.R;
import org.envirocar.app.adapter.DbAdapter;
import org.envirocar.app.adapter.Measurement;
import org.envirocar.app.adapter.Track;
import org.envirocar.app.application.ECApplication;
import org.envirocar.app.views.TYPEFACE;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

public class ListMeasurementsFragmentLocal extends SherlockFragment {

	private ArrayList<Track> tracksList;
	private TracksListAdapter elvAdapter;
	private DbAdapter dbAdapterLocal;
	private ExpandableListView elv;

	private ProgressBar progress;

	private int itemSelect;

	public View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, android.os.Bundle savedInstanceState) {

		dbAdapterLocal = ((ECApplication) getActivity().getApplication()).getDbAdapterLocal();
		/*
		 * Testing
		 */

		// ECApplication application = ((ECApplication)
		// getActivity().getApplication());
		//
		// SharedPreferences preferences =
		// PreferenceManager.getDefaultSharedPreferences(application);
		//
		// String fuelType =
		// preferences.getString(application.PREF_KEY_FUEL_TYPE, "gasoline");
		// String carManufacturer =
		// preferences.getString(application.PREF_KEY_CAR_MANUFACTURER,
		// "undefined");
		// String carModel =
		// preferences.getString(application.PREF_KEY_CAR_MODEL, "undefined");
		// String sensorId =
		// preferences.getString(application.PREF_KEY_SENSOR_ID, "undefined");
		//
		// Track track = new Track("123456", fuelType, carManufacturer,
		// carModel, sensorId, dbAdapter);
		// track.setName("Track 1");
		// track.commitTrackToDatabase();
		// try {
		// Measurement m1 = new Measurement(51.5f, 7.5f);
		// Measurement m2 = new Measurement(52.5f, 7.6f);
		// track.addMeasurement(m1);
		// track.addMeasurement(m2);
		// } catch (LocationInvalidException e) {
		// e.printStackTrace();
		// }
		//
		// Track track2 = new Track("123456", fuelType, carManufacturer,
		// carModel, sensorId, dbAdapter);
		// track2.setName("Track 2");
		// track2.commitTrackToDatabase();
		// try {
		// Measurement m1 = new Measurement(41.5f, 7.5f);
		// Measurement m2 = new Measurement(42.5f, 7.6f);
		// track2.addMeasurement(m1);
		// track2.addMeasurement(m2);
		// } catch (LocationInvalidException e) {
		// e.printStackTrace();
		// }

		/*
		 * Testing End
		 */

		View v = inflater.inflate(R.layout.list_tracks_layout_local, null);
		elv = (ExpandableListView) v.findViewById(R.id.list);
		progress = (ProgressBar) v.findViewById(R.id.listprogress);

		registerForContextMenu(elv);

		elv.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				itemSelect = ExpandableListView.getPackedPositionGroup(id);
				Log.e("obd2", String.valueOf("Selected item: " + itemSelect));
				return false;
			}

		});

		return v;
	};

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getSherlockActivity().getMenuInflater();
		inflater.inflate(R.menu.context_item, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ArrayList<Track> tracks = dbAdapterLocal.getAllTracks();
		final Track track = tracks.get(itemSelect);
		switch (item.getItemId()) {

		case R.id.editName:
			Log.e("obd2", "editing track: " + itemSelect);
			final EditText input = new EditText(getActivity());
			new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.editTrack)).setMessage(getString(R.string.enterTrackName)).setView(input).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String value = input.getText().toString();
					Log.e("obd2", "New name: " + value.toString());
					track.setName(value);
					track.setDatabaseAdapter(dbAdapterLocal);
					track.commitTrackToDatabase();
					tracksList.get(itemSelect).setName(value);
					elvAdapter.notifyDataSetChanged();
					Toast.makeText(getActivity(), getString(R.string.nameChanged), Toast.LENGTH_SHORT).show();
				}
			}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Do nothing.
				}
			}).show();
			return true;

		case R.id.editDescription:
			Log.e("obd2", "editing track: " + itemSelect);
			final EditText input2 = new EditText(getActivity());
			new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.editTrack)).setMessage(getString(R.string.enterTrackDescription)).setView(input2).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String value = input2.getText().toString();
					Log.e("obd2", "New description: " + value.toString());
					track.setDescription(value);
					track.setDatabaseAdapter(dbAdapterLocal);
					track.commitTrackToDatabase();
					elv.collapseGroup(itemSelect);
					tracksList.get(itemSelect).setDescription(value);
					elvAdapter.notifyDataSetChanged();
					// TODO Bug: update the description when it is changed.
					Toast.makeText(getActivity(), getString(R.string.descriptionChanged), Toast.LENGTH_SHORT).show();

				}
			}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Do nothing.
				}
			}).show();
			return true;

		case R.id.startMap:
			Log.e("obd2", Environment.getExternalStorageDirectory().toString());
			File f = new File(Environment.getExternalStorageDirectory() + "/Android");
			if (f.isDirectory()) {
				ArrayList<Measurement> measurements = track.getMeasurements();
				String[] trackCoordinates = extractCoordinates(measurements);
				Intent intent = new Intent(getActivity().getApplicationContext(), Map.class);
				Bundle bundle = new Bundle();
				bundle.putStringArray("coordinates", trackCoordinates);
				intent.putExtras(bundle);
				startActivity(intent);
			} else {
				Toast.makeText(getActivity(), "Map not possible without SD card.", Toast.LENGTH_LONG).show();
			}

			return true;

		case R.id.deleteTrack:
			Log.e("obd2", "deleting item: " + itemSelect);
			dbAdapterLocal.deleteTrack(track.getId());
			Toast.makeText(getActivity(), getString(R.string.trackDeleted), Toast.LENGTH_LONG).show();
			tracksList.remove(itemSelect);
			elvAdapter.notifyDataSetChanged();
			return true;

		case R.id.uploadTrack:
			Log.e("obd2", "uploading item: " + itemSelect);
			Toast.makeText(getActivity(), "This function is not supported yet. Please upload all tracks at once via the menu.", Toast.LENGTH_LONG).show();
			// TODO implement this (not "mission critical")
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		elv.setGroupIndicator(getResources().getDrawable(R.drawable.list_indicator));
		elv.setChildDivider(getResources().getDrawable(android.R.color.transparent));

		this.tracksList = dbAdapterLocal.getAllTracks();
		Log.i("obd", "Number of tracks: " + tracksList.size());
		if (elvAdapter == null)
			elvAdapter = new TracksListAdapter();
		elv.setAdapter(elvAdapter);
		elvAdapter.notifyDataSetChanged();

		// TODO update the list if new track is inserted into the database.

	}

	/**
	 * Returns an StringArray of coordinates for the mpa
	 * 
	 * @param measurements
	 *            arraylist with all measurements
	 * @return string array with coordinates
	 */
	private String[] extractCoordinates(ArrayList<Measurement> measurements) {
		ArrayList<String> coordinates = new ArrayList<String>();

		for (Measurement measurement : measurements) {
			String lat = String.valueOf(measurement.getLatitude());
			String lon = String.valueOf(measurement.getLongitude());
			coordinates.add(lat);
			coordinates.add(lon);
		}
		return coordinates.toArray(new String[coordinates.size()]);
	}

	private class TracksListAdapter extends BaseExpandableListAdapter {

		@Override
		public int getGroupCount() {
			return tracksList.size();
		}

		@Override
		public int getChildrenCount(int i) {
			return 1;
		}

		@Override
		public Object getGroup(int i) {
			return tracksList.get(i);
		}

		@Override
		public Object getChild(int i, int i1) {
			return tracksList.get(i);
		}

		@Override
		public long getGroupId(int i) {
			return i;
		}

		@Override
		public long getChildId(int i, int i1) {
			return i;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public View getGroupView(int i, boolean b, View view, ViewGroup viewGroup) {
			if (view == null || view.getId() != 10000000 + i) {
				Track currTrack = (Track) getGroup(i);
				View groupRow = ViewGroup.inflate(getActivity(), R.layout.list_tracks_group_layout, null);
				TextView textView = (TextView) groupRow.findViewById(R.id.track_name_textview);
				textView.setText(currTrack.getName());
				// Button button = (Button)
				// groupRow.findViewById(R.id.track_name_go_to_map);
				// button.setOnClickListener(new OnClickListener() {
				//
				// @Override
				// public void onClick(View v) {
				// Intent intent = new
				// Intent(getActivity().getApplicationContext(), Map.class);
				// startActivity(intent);
				// Log.i("bla", "bla");
				//
				// }
				// });
				groupRow.setId(10000000 + i);
				TYPEFACE.applyCustomFont((ViewGroup) groupRow, TYPEFACE.Newscycle(getActivity()));
				return groupRow;
			}
			return view;
		}

		@Override
		public View getChildView(int i, int i1, boolean b, View view, ViewGroup viewGroup) {
			if (view == null || view.getId() != 10000100 + i + i1) {
				Track currTrack = (Track) getChild(i, i1);
				View row = ViewGroup.inflate(getActivity(), R.layout.list_tracks_item_layout, null);
				TextView start = (TextView) row.findViewById(R.id.track_details_start_textview);
				TextView end = (TextView) row.findViewById(R.id.track_details_end_textview);
				TextView length = (TextView) row.findViewById(R.id.track_details_length_textview);
				TextView car = (TextView) row.findViewById(R.id.track_details_car_textview);
				TextView description = (TextView) row.findViewById(R.id.track_details_description_textview);
				TextView duration = (TextView) row.findViewById(R.id.track_details_duration_textview);
				TextView co2 = (TextView) row.findViewById(R.id.track_details_co2_textview);

				try {
					DateFormat sdf = DateFormat.getDateTimeInstance();
					DecimalFormat twoDForm = new DecimalFormat("#.##");
					DateFormat dfDuration = new SimpleDateFormat("HH:mm:ss:SSS");
					dfDuration.setTimeZone(TimeZone.getTimeZone("UTC"));
					start.setText(sdf.format(currTrack.getStartTime()) + "");
					end.setText(sdf.format(currTrack.getEndTime()) + "");
					Log.e("duration", currTrack.getEndTime() - currTrack.getStartTime() + "");
					Date durationMillis = new Date(currTrack.getEndTime() - currTrack.getStartTime());
					duration.setText(dfDuration.format(durationMillis) + "");
					length.setText(twoDForm.format(currTrack.getLengthOfTrack()) + " km");
					car.setText(currTrack.getCarManufacturer() + " " + currTrack.getCarModel());
					description.setText(currTrack.getDescription());
					co2.setText("");
				} catch (Exception e) {

				}

				row.setId(10000100 + i + i1);
				TYPEFACE.applyCustomFont((ViewGroup) row, TYPEFACE.Newscycle(getActivity()));
				return row;
			}
			return view;
		}

		@Override
		public boolean isChildSelectable(int i, int i1) {
			return false;
		}

	}

}
