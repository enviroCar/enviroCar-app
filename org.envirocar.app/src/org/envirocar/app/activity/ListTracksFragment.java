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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.envirocar.app.R;
import org.envirocar.app.application.ECApplication;
import org.envirocar.app.application.TermsOfUseManager;
import org.envirocar.app.application.UploadManager;
import org.envirocar.app.application.User;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.exception.ServerException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.Car.FuelType;
import org.envirocar.app.model.TermsOfUseInstance;
import org.envirocar.app.network.RestClient;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.DbAdapterImpl;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Measurement.PropertyKey;
import org.envirocar.app.storage.Track;
import org.envirocar.app.views.TypefaceEC;
import org.envirocar.app.views.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.preference.PreferenceManager;
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
import android.widget.ImageView;
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
public class ListTracksFragment extends SherlockFragment {

	// Measurements and tracks
	
	private List<Track> tracksList;
	private TracksListAdapter trackListAdapter;
	private DbAdapter dbAdapter;
	
	// UI Elements
	
	private ExpandableListView trackListView;
	private ProgressBar progress;
	private int itemSelect;
	
	private Menu menu;
	
	protected static final Logger logger = Logger.getLogger(ListTracksFragment.class);
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		dbAdapter = DbAdapterImpl.instance();
	}

	public View onCreateView(android.view.LayoutInflater inflater,
			android.view.ViewGroup container,
			android.os.Bundle savedInstanceState) {
		
		setHasOptionsMenu(true);

		View v = inflater.inflate(R.layout.list_tracks_layout, null);
		trackListView = (ExpandableListView) v.findViewById(R.id.list);
		progress = (ProgressBar) v.findViewById(R.id.listprogress);
		trackListView.setEmptyView(v.findViewById(android.R.id.empty));
		
		registerForContextMenu(trackListView);

		trackListView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				itemSelect = ExpandableListView.getPackedPositionGroup(id);
				logger.info(String.valueOf("Selected item: " + itemSelect));
				return false;
			}

		});
		return v;
	};
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		/*
		 * TODO create a mechanism to get informed when a track
		 * has been uploaded
		 */
		
		logger.info("Create view ListTracksFragment");
		super.onViewCreated(view, savedInstanceState);
		trackListView.setGroupIndicator(getResources().getDrawable(
				R.drawable.list_indicator));
		trackListView.setChildDivider(getResources().getDrawable(
				android.R.color.transparent));
		
		//fetch local tracks // TODO load tracks with async thread
		this.tracksList = dbAdapter.getAllTracks();
		logger.info("Number of tracks in the List: " + tracksList.size());
		if (trackListAdapter == null)
			trackListAdapter = new TracksListAdapter();
		trackListView.setAdapter(trackListAdapter);
		trackListAdapter.notifyDataSetChanged();
	
		//if logged in, download tracks from server
		if(UserManager.instance().isLoggedIn()){
			downloadTracks();
		}
		
	}

	/**
	 * This method requests the current fuel price of a given fuel type from a
	 * WPS that caches prices from export.benzinpreis-aktuell.de, calculates the
	 * total fuel price using the fuel consumption and length of track and sets
	 * the text of the respective <code>Textview</code>.
	 * 
	 * @param fuelCostView
	 *            The <code>Textview</code> the fuel price will be written to.
	 * @param twoDForm
	 *            Used for rounding the fuel price.
	 * @param litersPerHundredKm
	 *            Used to calculate the total fuel costs.
	 * @param lengthOfTrack
	 *            Used to calculate the total fuel costs.
	 * @param fuelType
	 *            The price depends on the type of fuel (e.g. diesel or
	 *            gasoline).
	 */
	private void getEstimatedFuelCost(final TextView fuelCostView,
			final DecimalFormat twoDForm, final double litersPerHundredKm,
			final double lengthOfTrack, final FuelType fuelType) {

		new AsyncTask<Void, Void, Double>() {

			@Override
			protected Double doInBackground(Void... params) {

				try {

					URL url = new URL(
							"http://geoprocessing.demo.52north.org:8080/wps/WebProcessingService?Service=WPS&Request=Execute&Version=1.0.0&Identifier=org.n52.wps.extension.GetFuelPriceProcess&DataInputs=fuelType="
									+ fuelType + "&RawDataOutput=fuelPrice");

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
			protected void onPostExecute(Double result) {

				double estimatedFuelCosts = result * (litersPerHundredKm / 100)
						* lengthOfTrack;

				if (result.equals(Double.NaN)) {
					fuelCostView.setText(R.string.error_calculating_fuel_costs);

					fuelCostView.setTextColor(Color.RED);

				} else {

					fuelCostView.setText(twoDForm.format(estimatedFuelCosts)
							+ " " + getActivity().getString(R.string.euro_sign));
				}
			}

		}.execute();

	}


	@Override
	public void onCreateOptionsMenu(Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
    	inflater.inflate(R.menu.menu_tracks, (com.actionbarsherlock.view.Menu) menu);
    	super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		this.menu = menu;
		updateUsabilityOfMenuItems();
	}

	private void updateUsabilityOfMenuItems() {
		if (dbAdapter.getAllLocalTracks().size() > 0) {
			menu.findItem(R.id.menu_delete_all).setEnabled(true);
			if(UserManager.instance().isLoggedIn())
				menu.findItem(R.id.menu_upload).setEnabled(true);
		} else {
			menu.findItem(R.id.menu_upload).setEnabled(false);
			menu.findItem(R.id.menu_delete_all).setEnabled(false);
		}
	}
	
	/**
	 * Method to remove all tracks of the logged in user from the listview and from the internal database.
	 * Tracks which are locally on the device, are not removed.
	 */
	public void clearRemoteTracks(){
		//remove tracks in a safe way
		Iterator<Track> trackIterator = tracksList.iterator();
		
		while (trackIterator.hasNext()) {
			Track track = (Track) trackIterator.next();
			if(!track.isLocalTrack()){
				trackIterator.remove();
			}
		}
		dbAdapter.deleteAllRemoteTracks();
		trackListAdapter.notifyDataSetChanged();
	}
	
	public void notifyDataSetChanged(Track track){
		updateUsabilityOfMenuItems();
		trackListAdapter.notifyDataSetChanged();
		//TODO: refresh tracks?! after they got uploaded, the (now remote) tracks still have the L-marker
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
			if (UserManager.instance().isLoggedIn()) {
				startTrackUpload(true, null);
			} else {
				Crouton.showText(getActivity(), R.string.hint_login_first, Style.INFO);
			}
			return true;
			
		//Delete all tracks

		case R.id.menu_delete_all:
			DbAdapterImpl.instance().deleteAllLocalTracks();
			Crouton.makeText(getActivity(), R.string.all_local_tracks_deleted,Style.CONFIRM).show();
			return true;
			
		}
		return false;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getSherlockActivity().getMenuInflater();
		final Track track = tracksList.get(itemSelect);
		if (track.isLocalTrack()){
			inflater.inflate(R.menu.context_item, menu);
		} else {
			inflater.inflate(R.menu.context_item_remote, menu);
		}
	}
	
	/**
	 * Change one item
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final Track track = tracksList.get(itemSelect);
		switch (item.getItemId()) {
		
		// Edit the trackname
		case R.id.editName:
			if(track.isLocalTrack()){
				logger.info("editing track: " + itemSelect);
				final EditText input = new EditText(getActivity());
				new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.editTrack)).setMessage(getString(R.string.enterTrackName)).setView(input).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();
						logger.info("New name: " + value.toString());
						track.setName(value);
						dbAdapter.updateTrack(track);
						tracksList.get(itemSelect).setName(value);
						trackListAdapter.notifyDataSetChanged();
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
			
		// Edit the track description
		case R.id.editDescription:
			if(track.isLocalTrack()){
				logger.info("editing track: " + itemSelect);
				final EditText input2 = new EditText(getActivity());
				new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.editTrack)).setMessage(getString(R.string.enterTrackDescription)).setView(input2).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input2.getText().toString();
						logger.info("New description: " + value.toString());
						track.setDescription(value);
						dbAdapter.updateTrack(track);
						trackListView.collapseGroup(itemSelect);
						tracksList.get(itemSelect).setDescription(value);
						trackListAdapter.notifyDataSetChanged();
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
			logger.info("Show in Map");
			logger.info(Environment.getExternalStorageDirectory().toString());
			File f = new File(Environment.getExternalStorageDirectory() + "/Android");
			if (f.isDirectory()) {
				ArrayList<Measurement> measurements = track.getMeasurements();
				logger.info("Count of measurements in the track: " + String.valueOf(measurements.size()));
				String[] trackCoordinates = extractCoordinates(measurements);
				
				if (trackCoordinates.length != 0){
					logger.info(String.valueOf(trackCoordinates.length));
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
			
		// Delete only selected track
		case R.id.deleteTrack:
			if(track.isLocalTrack()){
				logger.info("deleting item: " + itemSelect);
				dbAdapter.deleteTrack(track.getId());
				Crouton.showText(getActivity(), getString(R.string.trackDeleted), Style.INFO);
				tracksList.remove(itemSelect);
				trackListAdapter.notifyDataSetChanged();
			} else {
				createRemoteDeleteDialog(track);
			}
			return true;
			
		// Share track
		case R.id.shareTrack:
			try{
				Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
				sharingIntent.setType("application/json");
				Uri shareBody = Uri.fromFile(new UploadManager(getActivity().getApplication()).saveTrackAndReturnFile(track));
				sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "EnviroCar Track "+track.getName());
				sharingIntent.putExtra(android.content.Intent.EXTRA_STREAM,shareBody);
				startActivity(Intent.createChooser(sharingIntent, "Share via"));
			}catch (JSONException e){
				logger.warn(e.getMessage(), e);
				Crouton.showText(getActivity(), R.string.error_json, Style.ALERT);
			}
			return true;
			
		// Upload track
		case R.id.uploadTrack:
			if (UserManager.instance().isLoggedIn()) {
				startTrackUpload(false, track);
			} else {
				Crouton.showText(getActivity(), R.string.hint_login_first, Style.INFO);
			}
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	/**
	 * starts the uploading mechanism. It asserts the Terms of Use
	 * acceptance state.
	 * 
	 * @param all if all local tracks should be uploaded
	 * @param track a single track to upload
	 */
	private void startTrackUpload(final boolean all, final Track track) {
		final User user = UserManager.instance().getUser();
		boolean verified = false;
		try {
			verified = verifyTermsUseOfVersion(user.getAcceptedTermsOfUseVersion());
		} catch (ServerException e) {
			logger.warn(e.getMessage(), e);
			Crouton.makeText(getActivity(), getString(R.string.server_error_please_try_later), Style.ALERT).show();
			return;
		}
		if (!verified) {
			
			final TermsOfUseInstance current;
			try {
				current = TermsOfUseManager.instance().getCurrentTermsOfUse();
			} catch (ServerException e) {
				logger.warn("This should never happen!", e);
				return;
			}
			
			DialogUtil.createTermsOfUseDialog(current,
					user.getAcceptedTermsOfUseVersion() == null, new DialogUtil.PositiveNegativeCallback() {

				@Override
				public void negative() {
					logger.info("User did not accept the ToU.");
					Crouton.makeText(getActivity(), getString(R.string.terms_of_use_info), Style.ALERT).show();
				}

				@Override
				public void positive() {
					TermsOfUseManager.instance().userAcceptedTermsOfUse(user, current.getIssuedDate());
					uploadTracks(all, track);
				}
						
			}, getActivity());
		} else {
			uploadTracks(all, track);
		}
		
	}

	/**
	 * executes the actual track uploading
	 * 
	 * @param all if all local tracks should be uploaded
	 * @param track a single track to upload
	 */
	private void uploadTracks(boolean all, Track track) {
		if (all) {
			new UploadManager(((ECApplication) getActivity().getApplication())).uploadAllTracks();
		} else {
			new UploadManager(((ECApplication) getActivity().getApplication())).uploadSingleTrack(track);		
		}
	}

	/**
	 * verify the users accepted terms of use version
	 * against the latest from the server
	 * 
	 * @param acceptedTermsOfUseVersion the accepted version of the current user
	 * @return true, if the provided version is the latest
	 * @throws ServerException if the server did not respond (as expected)
	 */
	private boolean verifyTermsUseOfVersion(String acceptedTermsOfUseVersion) throws ServerException {
		if (acceptedTermsOfUseVersion == null) return false;
		
		TermsOfUseInstance current = TermsOfUseManager.instance().getCurrentTermsOfUse();
		
		return current.getIssuedDate().equals(acceptedTermsOfUseVersion);
	}

	private void createRemoteDeleteDialog(final Track track) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(R.string.deleteRemoteTrackQuestion)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								User user = UserManager.instance().getUser();
								final String username = user.getUsername();
								final String token = user.getToken();
								RestClient.deleteRemoteTrack(username, token,
										track.getRemoteID(),
										new JsonHttpResponseHandler() {
											@Override
											protected void handleMessage(Message msg) {
												removeRemoteTrack(track);
											}
										});
							}
						})
				.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// do nothing
							}
						});
		builder.create().show();
	}
	
	private void removeRemoteTrack(final Track track) {
		if (track.isRemoteTrack()) {
			if (tracksList.remove(track)) {
				dbAdapter.deleteTrack(track.getId());
				trackListAdapter.notifyDataSetChanged();
				Crouton.showText(
						getActivity(),
						getString(R.string.remoteTrackDeleted),
						Style.INFO);
			}
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

	/**
	 * Download remote tracks from the server and include them in the track list
	 */
	private void downloadTracks() {
		
		if(!((MainActivity<?>)getActivity()).isConnectedToInternet()){
			return;
		}
		
		User user = UserManager.instance().getUser();
		final String username = user.getUsername();
		final String token = user.getToken();
		RestClient.downloadTracks(username,token, new JsonHttpResponseHandler() {
			
			
			@Override
			public void onFailure(Throwable e, JSONObject errorResponse) {
				super.onFailure(e, errorResponse);
				logger.warn(e.getMessage(), e);
			}
			
			@Override
			public void onFailure(Throwable error, String content) {
				super.onFailure(error, content);
				logger.warn(content, error);
			}
			
			// Variable that holds the number of trackdl requests
			private int ct = 0;
			
			class AsyncOnSuccessTask extends AsyncTask<JSONObject, Void, Track>{
				
				@Override
				protected Track doInBackground(JSONObject... trackJson) {
					Track t;
					try {
						JSONObject trackProperties = trackJson[0].getJSONObject("properties");
						t = Track.createRemoteTrack(trackProperties.getString("id"), dbAdapter);
						String trackName = "unnamed Track #"+ct;
						try{
							trackName = trackProperties.getString("name");
						}catch (JSONException e){
							logger.warn(e.getMessage(), e);
						}
						t.setName(trackName);
						String description = "";
						try{
							description = trackProperties.getString("description");
						}catch (JSONException e){
							logger.warn(e.getMessage(), e);
						}
						t.setDescription(description);
						JSONObject sensorProperties = trackProperties.getJSONObject("sensor").getJSONObject("properties");
						
						t.setCar(Car.fromJson(sensorProperties)); 
						//include server properties tracks created, modified?
						
						dbAdapter.updateTrack(t);
						//Log.i("track_id",t.getId()+" "+((DbAdapterRemote) dbAdapter).trackExistsInDatabase(t.getId())+" "+dbAdapter.getNumberOfStoredTracks());
						
						Measurement recycleMeasurement;
						
						for (int j = 0; j < trackJson[0].getJSONArray("features").length(); j++) {
							
							JSONObject measurementJsonObject = trackJson[0].getJSONArray("features").getJSONObject(j);
							recycleMeasurement = new Measurement(
									Float.valueOf(measurementJsonObject.getJSONObject("geometry").getJSONArray("coordinates").getString(1)),
									Float.valueOf(measurementJsonObject.getJSONObject("geometry").getJSONArray("coordinates").getString(0)));
							JSONObject properties = measurementJsonObject.getJSONObject("properties");
							recycleMeasurement.setTime(Utils.isoDateToLong((properties.getString("time"))));
							JSONObject phenomenons = properties.getJSONObject("phenomenons");
							for (PropertyKey key : PropertyKey.values()) {
								if (phenomenons.has(key.toString())) {
									Double value = phenomenons.getJSONObject(key.toString()).getDouble("value"); 
									recycleMeasurement.setProperty(key, value);
								}
							}
							recycleMeasurement.setTrack(t);
							t.addMeasurement(recycleMeasurement);
						}

						return t;
					} catch (JSONException e) {
						logger.warn(e.getMessage(), e);
					} catch (NumberFormatException e) {
						logger.warn(e.getMessage(), e);
					} catch (ParseException e) {
						logger.warn(e.getMessage(), e);
					}
					return null;
				}

				@Override
				protected void onPostExecute(
						Track t) {
					super.onPostExecute(t);
					if(t != null){
						tracksList.add(t);
						trackListAdapter.notifyDataSetChanged();
					}
					ct--;
					if (ct == 0) {
						progress.setVisibility(View.GONE);
					}
				}
			}
			
			
			private void afterOneTrack(){
				View empty = getView().findViewById(android.R.id.empty);
				if (empty != null) {
					empty.setVisibility(View.GONE);
				}
				ct--;
				if (ct == 0) {
					progress.setVisibility(View.GONE);
					//sort the tracks bubblesort ?
					Collections.sort(tracksList);
					trackListAdapter.notifyDataSetChanged();
					updateUsabilityOfMenuItems();
				}
				if (trackListView.getAdapter() == null || (trackListView.getAdapter() != null && !trackListView.getAdapter().equals(trackListAdapter))) {
					trackListView.setAdapter(trackListAdapter);
				}
			}

			@Override
			public void onStart() {
				super.onStart();
				if (tracksList == null)
					tracksList = new ArrayList<Track>();
				if (trackListAdapter == null)
					trackListAdapter = new TracksListAdapter();
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
						boolean trackInList = false;

						// check if track is listed
						for (Track t : tracksList) {
							if (t.getRemoteID() != null && t.getRemoteID().equals(((JSONObject) tracks.get(i)).getString("id"))) {
								afterOneTrack();
								trackInList = true;
							}
						}
//						//AsyncTask to retrieve a Track from the database
//						class RetrieveTrackfromDbAsyncTask extends AsyncTask<Long, Void, Track> {
//							
//							@Override
//							protected Track doInBackground(Long... params) {
//								return dbAdapter.getTrack(params[0]);
//							}
//							
//							protected void onPostExecute(Track result) {
//								tracksList.add(result);
//								trackListAdapter.notifyDataSetChanged();
//								afterOneTrack();
//							}
//							
//						}
//						if (dbAdapter.hasTrack(((JSONObject) tracks.get(i)).getString("id"))) {
//							// if the track already exists in the db, skip and load from db.
//							new RetrieveTrackfromDbAsyncTask().execute(((JSONObject) tracks.get(i)).getString("id"));
//							continue;
//						}

						// else
						// download the track
						if (!trackInList) {
							RestClient.downloadTrack(username, token, ((JSONObject) tracks.get(i)).getString("id"),
									new JsonHttpResponseHandler() {
										
										@Override
										public void onFinish() {
											super.onFinish();
											if (trackListView.getAdapter() == null || (trackListView.getAdapter() != null && !trackListView.getAdapter().equals(trackListAdapter))) {
												trackListView.setAdapter(trackListAdapter);
											}
											trackListAdapter.notifyDataSetChanged();
										}

										@Override
										public void onSuccess(JSONObject trackJson) {
											super.onSuccess(trackJson);

											// start the AsyncTask to handle the downloaded trackjson
											new AsyncOnSuccessTask().execute(trackJson);

										}

										public void onFailure(Throwable arg0, String arg1) {
											logger.warn(arg1,arg0);
												};
											});

								}
							}
						
				} catch (JSONException e) {
					logger.warn(e.getMessage(), e);
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
				textView.setText(currTrack.getName());
				
				if(currTrack.isLocalTrack()){
					ImageView imageView = (ImageView) groupRow.findViewById(R.id.track_icon_view);
					imageView.setImageDrawable(getResources().getDrawable( R.drawable.mobile ));
				}
				
				groupRow.setId(10000000 + i);
				TypefaceEC.applyCustomFont((ViewGroup) groupRow,
						TypefaceEC.Newscycle(getActivity()));
				return groupRow;
			}
			return view;
		}

		@Override
		public View getChildView(int i, int i1, boolean b, View view, ViewGroup viewGroup) {
			logger.info("Selects a track");
			//if (view == null || view.getId() != 10000100 + i + i1) {
				Track currTrack = (Track) getChild(i, i1);
				View row = ViewGroup.inflate(getActivity(),
						R.layout.list_tracks_item_layout, null);
				TextView startView = (TextView) row
						.findViewById(R.id.track_details_start_textview);
				TextView endView = (TextView) row
						.findViewById(R.id.track_details_end_textview);
				TextView lengthView = (TextView) row
						.findViewById(R.id.track_details_length_textview);
				TextView carView = (TextView) row
						.findViewById(R.id.track_details_car_textview);
				TextView durationView = (TextView) row
						.findViewById(R.id.track_details_duration_textview);
				TextView co2View = (TextView) row
						.findViewById(R.id.track_details_co2_textview);
				TextView consumptionView = (TextView) row
						.findViewById(R.id.track_details_consumption_textview);
				TextView fuelCostView = (TextView) row
						.findViewById(R.id.track_details_fuel_cost_textview);
				TextView descriptionView = (TextView) row.findViewById(R.id.track_details_description_textview);

				try {
					DateFormat sdf = DateFormat.getDateTimeInstance();
					DecimalFormat twoDForm = new DecimalFormat("#.##");
					DateFormat dfDuration = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
					dfDuration.setTimeZone(TimeZone.getTimeZone("UTC"));
					startView.setText(sdf.format(currTrack.getStartTime()) + "");
					endView.setText(sdf.format(currTrack.getEndTime()) + "");
					Date durationMillis = new Date(currTrack.getDurationInMillis());
					durationView.setText(dfDuration.format(durationMillis) + "");
					if (!PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).getBoolean(SettingsActivity.IMPERIAL_UNIT, false)) {
						lengthView.setText(twoDForm.format(currTrack.getLengthOfTrack()) + " km");
					} else {
						lengthView.setText(twoDForm.format(currTrack.getLengthOfTrack()/1.6) + " miles");
					}
					Car car = currTrack.getCar();
					carView.setText(car.getManufacturer() + " " + car.getModel());
					descriptionView.setText(currTrack.getDescription());
					double consumption = currTrack.getFuelConsumptionPerHour();
					double literOn100km = currTrack.getLiterPerHundredKm();
					co2View.setText(twoDForm.format(currTrack.getGramsPerKm()) + "g/km");
					consumptionView.setText(twoDForm.format(consumption) + " l/h (" + twoDForm.format(literOn100km) + " l/100 km)");
					if(fuelCostView.getText() == null || fuelCostView.getText().equals("")){
						fuelCostView.setText(R.string.calculating);
						getEstimatedFuelCost(fuelCostView, twoDForm, literOn100km, currTrack.getLengthOfTrack(), currTrack.getCar().getFuelType());
					}
				} catch (Exception e) {
					logger.warn(e.getMessage(), e);
				}

				row.setId(10000100 + i + i1);
				TypefaceEC.applyCustomFont((ViewGroup) row,
						TypefaceEC.Newscycle(getActivity()));
				return row;
			//}
			//return view;
		}

		@Override
		public boolean isChildSelectable(int i, int i1) {
			return false;
		}

	}

}
