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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.envirocar.app.R;
import org.envirocar.app.application.ECApplication;
import org.envirocar.app.application.TermsOfUseManager;
import org.envirocar.app.application.TrackUploadFinishedHandler;
import org.envirocar.app.application.UploadManager;
import org.envirocar.app.application.User;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.exception.FuelConsumptionException;
import org.envirocar.app.exception.MeasurementsException;
import org.envirocar.app.exception.ServerException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.Car.FuelType;
import org.envirocar.app.model.TermsOfUseInstance;
import org.envirocar.app.network.RestClient;
import org.envirocar.app.protocol.algorithm.UnsupportedFuelTypeException;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.DbAdapterImpl;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Track;
import org.envirocar.app.storage.TrackWithoutMeasurementsException;
import org.envirocar.app.util.NamedThreadFactory;
import org.envirocar.app.util.Util;
import org.envirocar.app.views.TypefaceEC;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
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
	private int itemSelect;
	
	private Menu menu;
	private TextView statusText;
	private View statusProgressBar;
	
	private int remoteTrackCount;
	
	protected static final Logger logger = Logger.getLogger(ListTracksFragment.class);
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		dbAdapter = DbAdapterImpl.instance();
		
	}

	public View onCreateView(android.view.LayoutInflater inflater,
			android.view.ViewGroup container,
			android.os.Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		
		setHasOptionsMenu(true);

		View v = inflater.inflate(R.layout.list_tracks_layout, null);
		
		trackListView = (ExpandableListView) v.findViewById(R.id.list);
		
		statusProgressBar = v.findViewById(R.id.list_tracks_status_progress);
		statusText = (TextView) v.findViewById(R.id.list_tracks_status_text);
		
		setProgressStatusText(R.string.fetching_tracks);
		
		trackListView.setEmptyView(v.findViewById(R.id.empty));
		
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
		super.onViewCreated(view, savedInstanceState);
		/*
		 * TODO create a mechanism to get informed when a track
		 * has been uploaded
		 */
		
		logger.info("Create view ListTracksFragment");
		super.onViewCreated(view, savedInstanceState);
		trackListView.setGroupIndicator(getResources().getDrawable(R.drawable.group_indicator));
		trackListView.setChildDivider(getResources().getDrawable(
				android.R.color.transparent));
		
		tracksList = new ArrayList<Track>();
		
		if (trackListAdapter == null) {
			trackListAdapter = new TracksListAdapter();
			trackListView.setAdapter(trackListAdapter);
		}
		
		startTracksRetrieval();
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

		AsyncTask<Void, Void, Double> task = new AsyncTask<Void, Void, Double>() {

			@Override
			protected Double doInBackground(Void... params) {
				Thread.currentThread().setName("TrackList-WPSCaller-"+Thread.currentThread().getId());
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

		};
		
		Util.execute(task);

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
	}

	private void updateUsabilityOfMenuItems() {
		if (menu == null) return;
		
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
		updateTrackListView();
	}
	
	public void notifyDataSetChanged(Track track){
		updateUsabilityOfMenuItems();
		updateTrackListView();
	}
	
	/**
	 * Edit all tracks
	 */
	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {
		switch (item.getItemId()) {
		
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
			Crouton.makeText(getActivity(), R.string.not_yet_supported, Style.CONFIRM).show();
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
		final Track track;
		synchronized (this) {
			track = tracksList.get(itemSelect);	
		}
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
						updateTrackListView();
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
						updateTrackListView();
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
				List<Measurement> measurements = track.getMeasurements();
				logger.info("Count of measurements in the track: " + String.valueOf(measurements.size()));
				String[] trackCoordinates = extractCoordinates(measurements);
				
				if (trackCoordinates.length != 0){
					logger.info(String.valueOf(trackCoordinates.length));
					Intent intent = new Intent(getActivity().getApplicationContext(), org.envirocar.app.activity.Map.class);
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
				updateTrackListView();
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
			} catch (TrackWithoutMeasurementsException e) {
				logger.warn(e.getMessage(), e);
				Crouton.showText(getActivity(), R.string.track_finished_no_measurements, Style.ALERT);
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
			verified = TermsOfUseManager.verifyTermsUseOfVersion(user.getAcceptedTermsOfUseVersion());
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
		
		TrackUploadFinishedHandler callback = new TrackUploadFinishedHandler() {
			@Override
			public void onSuccessfulUpload(Track track) {
				trackListAdapter.updateTrackGroupView(track);
			}
		};
		
		if (all) {
			new UploadManager(((ECApplication) getActivity().getApplication())).uploadAllTracks(callback);
		} else {
			new UploadManager(((ECApplication) getActivity().getApplication())).uploadSingleTrack(track, callback);		
		}
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
												removeRemoteTrackFromViewAndDB(track);
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
	
	private void removeRemoteTrackFromViewAndDB(final Track track) {
		if (track.isRemoteTrack()) {
			if (tracksList.remove(track)) {
				dbAdapter.deleteTrack(track.getId());
				updateTrackListView();
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
	private String[] extractCoordinates(List<Measurement> measurements) {
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
	@SuppressLint("NewApi")
	private void startTracksRetrieval() {
		
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				Thread.currentThread().setName("TrackList-TrackRetriever"+Thread.currentThread().getId());
				
				setProgressStatusText(R.string.fetching_tracks_local);
				
				//fetch db tracks (local+remote)
				
				for (Track t : dbAdapter.getAllTracks(true)) {
					synchronized (ListTracksFragment.this) {
						tracksList.add(t);
					}	
				}
				
				updateTrackListView();
				
				logger.info("Number of tracks in the List: " + tracksList.size());
				
				if (UserManager.instance().isLoggedIn()) {
					setProgressStatusText(R.string.fetching_tracks_remote);
					if (((MainActivity<?>)getActivity()).isConnectedToInternet()) {
						User user = UserManager.instance().getUser();
						final String username = user.getUsername();
						final String token = user.getToken();
						
						downloadTracks(username, token);					
					}
						
				} else {
					updateStatusLayout();
				}
				
				return null;
			}
			
		};
		
		Util.execute(task);
		
	}

	protected void updateStatusLayout() {
		if (!isAdded()) return;
		
		getActivity().runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				statusProgressBar.setVisibility(View.INVISIBLE);
				
				statusText.setText(getResources().getString(R.string.track_list_count_text,
						resolveTrackCount(false),
						resolveTrackCount(true), createRemoteTrackCountString()));				
			}
		});
		
	}

	protected String createRemoteTrackCountString() {
		if (remoteTrackCount < 100) {
			return Integer.toString(remoteTrackCount);
		}
		return "100+";
	}

	private int resolveTrackCount(boolean remote) {
		int result = 0;
		
		for (Track t : tracksList) {
			if (t.isRemoteTrack() && remote) {
				result++;
			}
			else if (t.isLocalTrack() && !remote) {
				result++;
			}
		}
		
		return result;
	}

	protected void setProgressStatusText(int resId) {
		if (isAdded()) {
			final String str = getString(resId);
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					statusText.setText(str);	
					TypefaceEC.applyCustomFont(statusText,
							TypefaceEC.Newscycle(getActivity()));
				}
			});
		}
	}

	protected void updateTrackListView() {
		if (!isAdded()) return;
		
		synchronized (this) {
			Collections.sort(tracksList);	
		}
		
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				trackListAdapter.notifyDataSetChanged();				
			}
		});
	}

	private void downloadTracks(final String username, final String token) {
		RestClient.downloadTracks(username, token, 100, 1, new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(int statusCode, JSONObject response) {
				super.onSuccess(statusCode, response);

				try {
					JSONArray tracks = response.getJSONArray("tracks");
					remoteTrackCount = tracks.length();
				}
				catch (JSONException e) {
					logger.warn(e.getMessage(), e);
				}
			}
			
			@Override
			public void onFinish() {
				super.onFinish();
				
				RestClient.downloadTracks(username, token, new JsonHttpResponseHandler() {
					
					
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
					private int unprocessedTrackCount;
					
					protected void processDownloadedTrack(JSONObject... trackJson) {
						
						Track t;
						try {
							t = Track.fromJson(trackJson[0], dbAdapter);
							
							synchronized (ListTracksFragment.this) {
								tracksList.add(t);	
							}
							
							afterOneTrack();
							
						} catch (JSONException e) {
							logger.warn(e.getMessage(), e);
						} catch (NumberFormatException e) {
							logger.warn(e.getMessage(), e);
						} catch (ParseException e) {
							logger.warn(e.getMessage(), e);
						}
					}

					
					private synchronized void afterOneTrack() {
						
						View empty = getView().findViewById(android.R.id.empty);
						if (empty != null) {
							empty.setVisibility(View.GONE);
						}
						
						if (--unprocessedTrackCount == 0) {
							logger.info("Finished fetching tracks.");
							updateStatusLayout();
							updateTrackListView();
							updateUsabilityOfMenuItems();
						}
					}


					@Override
					public void onSuccess(int httpStatus, JSONObject json) {
						super.onSuccess(httpStatus, json);

						try {
							JSONArray tracks = json.getJSONArray("tracks");
							if (tracks.length() == 0) {
								updateStatusLayout();
							}
							
							Set<String> localRemoteIds = new HashSet<String>();
							synchronized (ListTracksFragment.this) {
								for (Track t : tracksList) {
									if (t.getRemoteID() != null) {
										localRemoteIds.add(t.getRemoteID());
									}
								}	
							}
							
							logger.info("found "+localRemoteIds.size()+" local tracks which have remoteIds.");
							
							List<String> tracksToDownload = new ArrayList<String>();
							unprocessedTrackCount = tracks.length();
							for (int i = 0; i < tracks.length(); i++) {

								String remoteId = ((JSONObject) tracks.get(i)).getString("id");
								// check if track is listed. if, continue
								if (localRemoteIds.contains(remoteId)) {
									logger.info("Skipping track with remoteId "+remoteId);
									afterOneTrack();
									continue;
								}
								
								tracksToDownload.add(remoteId);
							}
							
							if (!tracksToDownload.isEmpty()) {
								logger.info("Starting download of "+ tracksToDownload.size() +" tracks");
								final AtomicInteger index = new AtomicInteger(0);
								downloadTrack(tracksToDownload, index);
							}
								
						} catch (JSONException e) {
							logger.warn(e.getMessage(), e);
						}
					}

					private void downloadTrack(final List<String> tracksToDownload,
							final AtomicInteger index) {
						
						final String id = tracksToDownload.get(index.get());
						logger.info("downloading track with remoteId "+id);
						
						// download the track
						RestClient.downloadTrack(username, token, id,
							new JsonHttpResponseHandler() {
								
								@Override
								public void onFinish() {
									super.onFinish();
									updateTrackListView();
									
									/*
									 * on finish, start the next track download
									 */
									if (index.getAndIncrement() < tracksToDownload.size()) {
										downloadTrack(tracksToDownload, index);
									}
								}

								@Override
								public void onSuccess(JSONObject trackJson) {
									super.onSuccess(trackJson);
									logger.info("Download of track " +id+ " succeeded. Processing...");
									processDownloadedTrack(trackJson);
								}

								public void onFailure(Throwable arg0, String arg1) {
									logger.warn(arg1,arg0);
								};
							}
						);				
					}
				});	
				
			}
		});
			
	}

	private class TracksListAdapter extends BaseExpandableListAdapter {

		private ExecutorService lazyLoader = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("MeasurementLazyLoader"));
		private static final int GROUP_VIEW_BASE_ID = 10000000;
		private static final int CHILD_VIEW_BASE_ID = 10001000;
		private java.util.Map<Track, View> trackToGroupViewMap;
		private Map<Track,View> trackToChildViewMap;

		public TracksListAdapter() {
			trackToGroupViewMap = new HashMap<Track, View>();
			trackToChildViewMap = new HashMap<Track, View>();
		}
		
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
		
		public void updateTrackGroupView(Track t) {
			View groupRow = trackToGroupViewMap.get(t);
			
			if (groupRow != null) {
				setTrackTypeImage(t, groupRow);
				groupRow.invalidate();
			}
		}

		@Override
		public View getGroupView(int i, boolean b, View view,
				ViewGroup viewGroup) {
			Track t;
			synchronized (ListTracksFragment.this) {
				t = tracksList.get(i);
				if (!trackToGroupViewMap.containsKey(t)) {
					View groupRow = ViewGroup.inflate(getActivity(), R.layout.list_tracks_group_layout, null);
					setTrackTypeImage(t, groupRow);
					
					groupRow.setId(GROUP_VIEW_BASE_ID + i);
					TypefaceEC.applyCustomFont((ViewGroup) groupRow,
							TypefaceEC.Newscycle(getActivity()));

					trackToGroupViewMap.put(t, groupRow);
				}
			}
			
			return trackToGroupViewMap.get(t);
		}

		private void setTrackTypeImage(Track t, View groupRow) {
			TextView textView = (TextView) groupRow.findViewById(R.id.track_name_textview);
			textView.setText(t.getName());
			if (t.isLocalTrack()){
				ImageView imageView = (ImageView) groupRow.findViewById(R.id.track_icon_view);
				imageView.setImageDrawable(getResources().getDrawable( R.drawable.mobile ));
			}
		}

		@Override
		public View getChildView(int position, int i1, boolean b, View view, ViewGroup viewGroup) {
			final Track t;
			synchronized (ListTracksFragment.this) {
				t = tracksList.get(position);
				if (!trackToChildViewMap.containsKey(t)) {
					View row = ViewGroup.inflate(getActivity(),
							R.layout.list_tracks_item_layout, null);
					final TextView startView = (TextView) row
							.findViewById(R.id.track_details_start_textview);
					final TextView endView = (TextView) row
							.findViewById(R.id.track_details_end_textview);
					final TextView lengthView = (TextView) row
							.findViewById(R.id.track_details_length_textview);
					final TextView carView = (TextView) row
							.findViewById(R.id.track_details_car_textview);
					final TextView durationView = (TextView) row
							.findViewById(R.id.track_details_duration_textview);
					final TextView co2View = (TextView) row
							.findViewById(R.id.track_details_co2_textview);
					final TextView consumptionView = (TextView) row
							.findViewById(R.id.track_details_consumption_textview);
					final TextView fuelCostView = (TextView) row
							.findViewById(R.id.track_details_fuel_cost_textview);
					final TextView descriptionView = (TextView) row.findViewById(R.id.track_details_description_textview);

					setTrackChildFields(t, startView, endView, lengthView,
							carView, durationView, co2View,
							consumptionView, fuelCostView, descriptionView);
					if (t.isLazyLoadingMeasurements()) {
						lazyLoader.submit(new Runnable() {
							@Override
							public void run() {
								t.setLazyLoadingMeasurements(false);
								t.getMeasurements();
								getActivity().runOnUiThread(new Runnable() {
									
									@Override
									public void run() {
										setTrackChildFields(t, startView, endView, lengthView,
												carView, durationView, co2View,
												consumptionView, fuelCostView, descriptionView);										
									}
								});
								
							}
						});
					}

					row.setId(CHILD_VIEW_BASE_ID + position + i1);
					TypefaceEC.applyCustomFont((ViewGroup) row,
							TypefaceEC.Newscycle(getActivity()));
					trackToChildViewMap.put(t, row);
				}
			}
			
			return trackToChildViewMap.get(t);
		}

		private void setTrackChildFields(Track t, TextView startView,
				TextView endView, TextView lengthView, TextView carView,
				TextView durationView, TextView co2View,
				TextView consumptionView, TextView fuelCostView,
				TextView descriptionView) {
			Car car = t.getCar();
			carView.setText(car.toString());
			descriptionView.setText(t.getDescription());
			if (t.isLazyLoadingMeasurements()) {
				String loading = "loading...";
				setTextViewContent(startView, loading);
				setTextViewContent(endView, loading);
				setTextViewContent(lengthView, loading);
				setTextViewContent(durationView, loading);
				setTextViewContent(co2View, loading);
				setTextViewContent(consumptionView, loading);
			}
			else {
				try {
					DateFormat sdf = DateFormat.getDateTimeInstance();
					DecimalFormat twoDForm = new DecimalFormat("#.##");
					DateFormat dfDuration = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
					dfDuration.setTimeZone(TimeZone.getTimeZone("UTC"));
					startView.setText(sdf.format(t.getStartTime()));
					endView.setText(sdf.format(t.getEndTime()));
					Date durationMillis = new Date(t.getDurationInMillis());
					durationView.setText(dfDuration.format(durationMillis));
					if (!PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).getBoolean(SettingsActivity.IMPERIAL_UNIT, false)) {
						lengthView.setText(twoDForm.format(t.getLengthOfTrack()) + " km");
					} else {
						lengthView.setText(twoDForm.format(t.getLengthOfTrack()/1.6) + " miles");
					}
					
					try {
						double consumption = t.getFuelConsumptionPerHour();
						double literOn100km = t.getLiterPerHundredKm();
						co2View.setText(twoDForm.format(t.getGramsPerKm()) + " g/km");
						consumptionView.setText(twoDForm.format(consumption) + " l/h (" + twoDForm.format(literOn100km) + " l/100 km)");
						if(fuelCostView.getText() == null || fuelCostView.getText().equals("")){
							fuelCostView.setText(R.string.calculating);
							getEstimatedFuelCost(fuelCostView, twoDForm, literOn100km, t.getLengthOfTrack(), t.getCar().getFuelType());
						}
					} catch (UnsupportedFuelTypeException e) {
						logger.warn(e.getMessage());
					} catch (MeasurementsException e) {
						logger.warn(e.getMessage());
					} catch (FuelConsumptionException e) {
						logger.warn(e.getMessage());
					}
					
				} catch (MeasurementsException e) {
					logger.warn(e.getMessage(), e);
				}
			}
			
		}

		private void setTextViewContent(TextView tv, String string) {
			tv.setText(string);
		}

		@Override
		public boolean isChildSelectable(int i, int i1) {
			return false;
		}
		
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
}
