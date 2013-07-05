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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.TimeZone;

import org.envirocar.app.R;
import org.envirocar.app.application.ECApplication;
import org.envirocar.app.application.RestClient;
import org.envirocar.app.application.UploadManager;
import org.envirocar.app.exception.LocationInvalidException;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.DbAdapterRemote;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Track;
import org.envirocar.app.views.TypefaceEC;
import org.envirocar.app.views.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
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

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.loopj.android.http.JsonHttpResponseHandler;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
/**
 * List Fragement that displays local and remote tracks.
 * @author jakob
 * @author gerald
 *
 */
public class ListMeasurementsFragment extends SherlockFragment {
	
	ECApplication application;

	// Measurements and tracks
	
	private ArrayList<Track> tracksList;
	private TracksListAdapter elvAdapter;
	private DbAdapter dbAdapterRemote;
	private DbAdapter dbAdapterLocal;
	
	// UI Elements
	
	private ExpandableListView elv;
	private ProgressBar progress;
	private int itemSelect;
	
	private com.actionbarsherlock.view.MenuItem upload;

	public View onCreateView(android.view.LayoutInflater inflater,
			android.view.ViewGroup container,
			android.os.Bundle savedInstanceState) {
		
		application = ((ECApplication) getActivity().getApplication()); 
		
		setHasOptionsMenu(true);

		dbAdapterRemote = ((ECApplication) getActivity().getApplication()).getDbAdapterRemote();
		dbAdapterLocal = ((ECApplication) getActivity().getApplication()).getDbAdapterLocal();

		View v = inflater.inflate(R.layout.list_tracks_layout, null);
		elv = (ExpandableListView) v.findViewById(R.id.list);
		progress = (ProgressBar) v.findViewById(R.id.listprogress);
		elv.setEmptyView(v.findViewById(android.R.id.empty));
		
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
	public void onCreateOptionsMenu(Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
    	inflater.inflate(R.menu.menu_tracks, (com.actionbarsherlock.view.Menu) menu);
    	super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if (((ECApplication) getActivity().getApplication()).getDbAdapterLocal().getAllTracks().size() > 0) {
			menu.findItem(R.id.menu_delete_all).setEnabled(true);
			if(((ECApplication) getActivity().getApplication()).isLoggedIn())
				menu.findItem(R.id.menu_upload).setEnabled(true);
		} else {
			menu.findItem(R.id.menu_upload).setEnabled(false);
			menu.findItem(R.id.menu_delete_all).setEnabled(false);
		}
		upload = menu.findItem(R.id.menu_upload);
		
	}
	
	/**
	 * Method to remove all tracks of the logged in user from the listview and from the internal database.
	 * Tracks which are locally on the device, are not removed.
	 */
	public void clearRemoteTracks(){
		try{
			for(Track t : tracksList){
				if(!t.isLocalTrack())
					tracksList.remove(t);
			}
		} catch (ConcurrentModificationException e) {
			clearRemoteTracks();
		}
		dbAdapterRemote.deleteAllTracks();
		elvAdapter.notifyDataSetChanged();
	}
	
	/**
	 * Edit all tracks
	 */
	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {
		switch(item.getItemId()){
		
		//Upload all tracks
		
		case R.id.menu_upload:
			((ECApplication) getActivity().getApplicationContext()).createNotification("start");
			UploadManager uploadManager = new UploadManager(((ECApplication) getActivity().getApplication()));
			uploadManager.uploadAllTracks();
			upload.setEnabled(false);
			return true;
			
		//Delete all tracks

		case R.id.menu_delete_all:
			((ECApplication) getActivity().getApplication()).getDbAdapterLocal().deleteAllTracks();
			((ECApplication) getActivity().getApplication()).setTrack(null);
			Crouton.makeText(getActivity(), R.string.all_local_tracks_deleted,Style.CONFIRM).show();
			return true;
			
		}
		return false;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getSherlockActivity().getMenuInflater();
		inflater.inflate(R.menu.context_item_remote, menu);
	}
	
	/**
	 * Change one item
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final Track track = tracksList.get(itemSelect);
		switch (item.getItemId()) {
		
		//Edit the trackname

		case R.id.editName:
			if(track.isLocalTrack()){
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
						Crouton.showText(getActivity(), getString(R.string.nameChanged), Style.INFO);
					}
				}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Do nothing.
					}
				}).show();
			} else {
				Crouton.showText(getActivity(), R.string.not_possible_for_remote, Style.INFO);
			}
			return true;
			
		//Edit the track description

		case R.id.editDescription:
			if(track.isLocalTrack()){
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
						Crouton.showText(getActivity(), getString(R.string.descriptionChanged), Style.INFO);
					}
				}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Do nothing.
					}
				}).show();
			} else {
				Crouton.showText(getActivity(), R.string.not_possible_for_remote, Style.INFO);
			}
			return true;
			
		// Show that track in the map

		case R.id.startMap:
			Log.e("obd2", Environment.getExternalStorageDirectory().toString());
			File f = new File(Environment.getExternalStorageDirectory() + "/Android");
			if (f.isDirectory()) {
				ArrayList<Measurement> measurements = track.getMeasurements();
				Log.e("obd2",String.valueOf(measurements.size()));
				String[] trackCoordinates = extractCoordinates(measurements);
				
				if (trackCoordinates.length != 0){
					Log.e("obd2",String.valueOf(trackCoordinates.length));
					Intent intent = new Intent(getActivity().getApplicationContext(), Map.class);
					Bundle bundle = new Bundle();
					bundle.putStringArray("coordinates", trackCoordinates);
					intent.putExtras(bundle);
					startActivity(intent);
				} else {
					Crouton.showText(getActivity(), getString(R.string.trackContainsNoCoordinates), Style.INFO);
				}
				
			} else {
				Crouton.showText(getActivity(), getString(R.string.noSdCard), Style.INFO);
			}

			return true;
			
		// Delete only this track

		case R.id.deleteTrack:
			if(track.isLocalTrack()){
				Log.e("obd2", "deleting item: " + itemSelect);
				dbAdapterLocal.deleteTrack(track.getId());
				Crouton.showText(getActivity(), getString(R.string.trackDeleted), Style.INFO);
				tracksList.remove(itemSelect);
				elvAdapter.notifyDataSetChanged();
			} else {
				Crouton.showText(getActivity(), R.string.not_possible_for_remote, Style.INFO);
			}
			return true;

		
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		elv.setGroupIndicator(getResources().getDrawable(
				R.drawable.list_indicator));
		elv.setChildDivider(getResources().getDrawable(
				android.R.color.transparent));
		
		//fetch local tracks
		this.tracksList = dbAdapterLocal.getAllTracks();
		Log.i("obd", "Number of tracks: " + tracksList.size());
		if (elvAdapter == null)
			elvAdapter = new TracksListAdapter();
		elv.setAdapter(elvAdapter);
		elvAdapter.notifyDataSetChanged();

		//if logged in, download tracks from server
		if(((ECApplication) getActivity().getApplication()).isLoggedIn()){
			downloadTracks();
		}

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
	
	public void notifyFragmentVisible(){
		
	}

	/**
	 * Download remote tracks from the server and include them in the track list
	 */
	private void downloadTracks() {
		
		final String username = ((ECApplication) getActivity().getApplication()).getUser().getUsername();
		final String token = ((ECApplication) getActivity().getApplication()).getUser().getToken();
		RestClient.downloadTracks(username,token, new JsonHttpResponseHandler() {
			
			
			@Override
			public void onFailure(Throwable e, JSONObject errorResponse) {
				super.onFailure(e, errorResponse);
				Log.i("error",e.toString());
			}
			
			@Override
			public void onFailure(Throwable error, String content) {
				super.onFailure(error, content);
				Log.i("faildl",content,error);
			}
			
			// Variable that holds the number of trackdl requests
			private int ct = 0;
			
			class AsyncOnSuccessTask extends AsyncTask<JSONObject, Void, Track>{
				
				@Override
				protected Track doInBackground(JSONObject... trackJson) {
					Track t;
					try {
						t = new Track(trackJson[0].getJSONObject("properties").getString("id"));
						t.setDatabaseAdapter(dbAdapterRemote);
						String trackName = "unnamed Track #"+ct;
						try{
							trackName = trackJson[0].getJSONObject("properties").getString("name");
						}catch (JSONException e){}
						t.setName(trackName);
						String description = "";
						try{
							description = trackJson[0].getJSONObject("properties").getString("description");
						}catch (JSONException e){}
						t.setDescription(description);
						String manufacturer = "unknown";
						try{
							manufacturer = trackJson[0].getJSONObject("properties").getJSONObject("sensor").getJSONObject("properties").getString("manufacturer");
						}catch (JSONException e){}
						t.setCarManufacturer(manufacturer);
						String carModel = "unknown";
						try{
							carModel = trackJson[0].getJSONObject("properties").getJSONObject("sensor").getJSONObject("properties").getString("model");
						}catch (JSONException e){}
						t.setCarModel(carModel);
						String sensorId = "undefined";
						try{
							sensorId = trackJson[0].getJSONObject("properties").getJSONObject("sensor").getString("id");
						}catch (JSONException e) {}
						t.setSensorID(sensorId);
						//include server properties tracks created, modified?
						// TODO more properties
						
						t.commitTrackToDatabase();
						//Log.i("track_id",t.getId()+" "+((DbAdapterRemote) dbAdapter).trackExistsInDatabase(t.getId())+" "+dbAdapter.getNumberOfStoredTracks());
						
						Measurement recycleMeasurement;
						
						for (int j = 0; j < trackJson[0].getJSONArray("features").length(); j++) {
							recycleMeasurement = new Measurement(
									Float.valueOf(trackJson[0].getJSONArray("features").getJSONObject(j).getJSONObject("geometry").getJSONArray("coordinates").getString(1)),
									Float.valueOf(trackJson[0].getJSONArray("features").getJSONObject(j).getJSONObject("geometry").getJSONArray("coordinates").getString(0)));

							recycleMeasurement.setMaf((trackJson[0].getJSONArray("features").getJSONObject(j).getJSONObject("properties").getJSONObject("phenomenons").getJSONObject("MAF").getDouble("value")));
							recycleMeasurement.setSpeed((trackJson[0].getJSONArray("features").getJSONObject(j).getJSONObject("properties").getJSONObject("phenomenons").getJSONObject("Speed").getInt("value")));
							recycleMeasurement.setMeasurementTime(Utils.isoDateToLong((trackJson[0].getJSONArray("features").getJSONObject(j).getJSONObject("properties").getString("time"))));
							recycleMeasurement.setTrack(t);
							t.addMeasurement(recycleMeasurement);
						}

						return t;
					} catch (JSONException e) {
						e.printStackTrace();
					} catch (NumberFormatException e) {
						e.printStackTrace();
					} catch (LocationInvalidException e) {
						e.printStackTrace();
					} catch (ParseException e) {
						e.printStackTrace();
					}
					return null;
				}

				@Override
				protected void onPostExecute(
						Track t) {
					super.onPostExecute(t);
					if(t != null){
						t.setLocalTrack(false);
						tracksList.add(t);
						elvAdapter.notifyDataSetChanged();
					}
					ct--;
					if (ct == 0) {
						progress.setVisibility(View.GONE);
					}
				}
			}
			
			
			private void afterOneTrack(){
				getView().findViewById(android.R.id.empty).setVisibility(View.GONE);
				ct--;
				if (ct == 0) {
					progress.setVisibility(View.GONE);
				}
				if (elv.getAdapter() == null || (elv.getAdapter() != null && !elv.getAdapter().equals(elvAdapter))) {
					elv.setAdapter(elvAdapter);
				}
			}

			@Override
			public void onStart() {
				super.onStart();
				if (tracksList == null)
					tracksList = new ArrayList<Track>();
				if (elvAdapter == null)
					elvAdapter = new TracksListAdapter();
				progress.setVisibility(View.VISIBLE);
			}

			@Override
			public void onSuccess(int httpStatus, JSONObject json) {
				super.onSuccess(httpStatus, json);

				try {
					JSONArray tracks = json.getJSONArray("tracks");
					if(tracks.length()==0) progress.setVisibility(View.GONE);
					ct = tracks.length();
					for (int i = 0; i < tracks.length(); i++) {

						// skip tracks already in the ArrayList
						for (Track t : tracksList) {
							if (t.getId().equals(((JSONObject) tracks.get(i)).getString("id"))) {
								afterOneTrack();
								continue;
							}
						}
						//AsyncTask to retrieve a Track from the database
						class RetrieveTrackfromDbAsyncTask extends AsyncTask<String, Void, Track>{
							
							@Override
							protected Track doInBackground(String... params) {
								return dbAdapterRemote.getTrack(params[0]);
							}
							
							protected void onPostExecute(Track result) {
								tracksList.add(result);
								elvAdapter.notifyDataSetChanged();
								afterOneTrack();
							}
							
						}
						if (((DbAdapterRemote) dbAdapterRemote).trackExistsInDatabase(((JSONObject) tracks.get(i)).getString("id"))) {
							// if the track already exists in the db, skip and load from db.
							new RetrieveTrackfromDbAsyncTask().execute(((JSONObject) tracks.get(i)).getString("id"));
							continue;
						}

						// else
						// download the track
						RestClient.downloadTrack(username, token, ((JSONObject) tracks.get(i)).getString("id"),
								new JsonHttpResponseHandler() {
									
									@Override
									public void onFinish() {
										super.onFinish();
										if (elv.getAdapter() == null || (elv.getAdapter() != null && !elv.getAdapter().equals(elvAdapter))) {
											elv.setAdapter(elvAdapter);
										}
										elvAdapter.notifyDataSetChanged();
									}

									@Override
									public void onSuccess(JSONObject trackJson) {
										super.onSuccess(trackJson);

										// start the AsyncTask to handle the downloaded trackjson
										new AsyncOnSuccessTask().execute(trackJson);

									}

									public void onFailure(Throwable arg0,
											String arg1) {
										Log.i("downloaderror",arg1,arg0);
									};
								});

					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});
		
		
		

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
		public View getGroupView(int i, boolean b, View view,
				ViewGroup viewGroup) {
			if (view == null || view.getId() != 10000000 + i) {
				Track currTrack = (Track) getGroup(i);
				View groupRow = ViewGroup.inflate(getActivity(), R.layout.list_tracks_group_layout, null);
				TextView textView = (TextView) groupRow.findViewById(R.id.track_name_textview);
				textView.setText((currTrack.isLocalTrack() ? "L" : "R")+" "+currTrack.getName());
				groupRow.setId(10000000 + i);
				TypefaceEC.applyCustomFont((ViewGroup) groupRow,
						TypefaceEC.Newscycle(getActivity()));
				return groupRow;
			}
			return view;
		}

		@Override
		public View getChildView(int i, int i1, boolean b, View view,
				ViewGroup viewGroup) {
			if (view == null || view.getId() != 10000100 + i + i1) {
				Track currTrack = (Track) getChild(i, i1);
				View row = ViewGroup.inflate(getActivity(),
						R.layout.list_tracks_item_layout, null);
				TextView start = (TextView) row
						.findViewById(R.id.track_details_start_textview);
				TextView end = (TextView) row
						.findViewById(R.id.track_details_end_textview);
				TextView length = (TextView) row
						.findViewById(R.id.track_details_length_textview);
				TextView car = (TextView) row
						.findViewById(R.id.track_details_car_textview);
				TextView duration = (TextView) row
						.findViewById(R.id.track_details_duration_textview);
				TextView co2 = (TextView) row
						.findViewById(R.id.track_details_co2_textview);
				TextView description = (TextView) row.findViewById(R.id.track_details_description_textview);

				try {
					DateFormat sdf = DateFormat.getDateTimeInstance();
					DecimalFormat twoDForm = new DecimalFormat("#.##");
					DateFormat dfDuration = new SimpleDateFormat("HH:mm:ss");
					dfDuration.setTimeZone(TimeZone.getTimeZone("UTC"));
					start.setText(sdf.format(currTrack.getStartTime()) + "");
					end.setText(sdf.format(currTrack.getEndTime()) + "");
					Log.e("duration",
							currTrack.getEndTime() - currTrack.getStartTime()
									+ "");
					Date durationMillis = new Date(currTrack.getEndTime()
							- currTrack.getStartTime());
					duration.setText(dfDuration.format(durationMillis) + "");
					if (!application.isImperialUnits()) {
						length.setText(twoDForm.format(currTrack.getLengthOfTrack()) + " km");
					} else {
						length.setText(twoDForm.format(currTrack.getLengthOfTrack()/1.6) + " miles");
					}
					car.setText(currTrack.getCarManufacturer() + " "
							+ currTrack.getCarModel());
					description.setText(currTrack.getDescription());
					co2.setText("");
				} catch (Exception e) {

				}

				row.setId(10000100 + i + i1);
				TypefaceEC.applyCustomFont((ViewGroup) row,
						TypefaceEC.Newscycle(getActivity()));
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
