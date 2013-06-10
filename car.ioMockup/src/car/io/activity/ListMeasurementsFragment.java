package car.io.activity;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import car.io.R;
import car.io.adapter.DbAdapter;
import car.io.adapter.DbAdapterRemote;
import car.io.adapter.Measurement;
import car.io.adapter.Track;
import car.io.application.ECApplication;
import car.io.application.RestClient;
import car.io.exception.LocationInvalidException;
import car.io.views.TYPEFACE;

import com.actionbarsherlock.app.SherlockFragment;
import com.loopj.android.http.JsonHttpResponseHandler;

public class ListMeasurementsFragment extends SherlockFragment {

	private ArrayList<Track> tracksList;
	private TracksListAdapter elvAdapter;
	private DbAdapter dbAdapter;
	private ExpandableListView elv;

	private ProgressBar progress;
	
	
	private Vector<String> dlTrackIds = new Vector<String>();

	public View onCreateView(android.view.LayoutInflater inflater,
			android.view.ViewGroup container,
			android.os.Bundle savedInstanceState) {

		dbAdapter = ((ECApplication) getActivity().getApplication()).getDbAdapterRemote();

		View v = inflater.inflate(R.layout.list_tracks_layout, null);
		elv = (ExpandableListView) v.findViewById(R.id.list);
		progress = (ProgressBar) v.findViewById(R.id.listprogress);
		tracksList = new ArrayList<Track>();
		return v;
	};

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		elv.setGroupIndicator(getResources().getDrawable(
				R.drawable.list_indicator));
		elv.setChildDivider(getResources().getDrawable(
				android.R.color.transparent));
		if(((ECApplication) getActivity().getApplication()).isLoggedIn()){
			downloadTracks();
		}

	}
	
	public void notifyFragmentVisible(){
		
	}

	private void downloadTracks() {
		
		
		String username = ((ECApplication) getActivity().getApplication()).getUser().getUsername();
		//TODO: make AsyncJsonHttpResponseHandler
		RestClient.downloadTracks(username,new JsonHttpResponseHandler() {

			// Variable that holds the number of trackdl requests
			private int ct = 0;

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

				//TODO put everything in the asynctask
				try {
					JSONArray tracks = json.getJSONArray("tracks");
					ct = tracks.length();
					for (int i = 0; i < tracks.length(); i++) {

						// skip tracks already in the ArrayList
						for (Track t : tracksList) {
							if (t.getId().equals(((JSONObject) tracks.get(i)).getString("id"))) {
								ct--;
								if (ct == 0) {
									progress.setVisibility(View.GONE);
								}
								if (elv.getAdapter() == null || (elv.getAdapter() != null && !elv.getAdapter().equals(elvAdapter))) {
									elv.setAdapter(elvAdapter);
								}
								continue;
							}
						}

						if (((DbAdapterRemote) dbAdapter).trackExistsInDatabase(((JSONObject) tracks.get(i)).getString("id"))) {
							// if the track already exists in the db, skip and load from db.
							tracksList.add(dbAdapter.getTrack(((JSONObject) tracks.get(i)).getString("id")));
							elvAdapter.notifyDataSetChanged();
							ct--;
							if (ct == 0) {
								progress.setVisibility(View.GONE);
							}
							if (elv.getAdapter() == null || (elv.getAdapter() != null && !elv.getAdapter().equals(elvAdapter))) {
								elv.setAdapter(elvAdapter);
							}
							continue;
						}

						// else
						// download the track
						RestClient.downloadTrack(
								((JSONObject) tracks.get(i)).getString("href"),
								new JsonHttpResponseHandler() {
									
									@Override
									public void onFinish() {
										super.onFinish();
										if (elv.getAdapter() == null
												|| (elv.getAdapter() != null && !elv
														.getAdapter().equals(
																elvAdapter))) {
											elv.setAdapter(elvAdapter);
										}
										elvAdapter.notifyDataSetChanged();
									}

									@Override
									public void onSuccess(JSONObject trackJson) {
										super.onSuccess(trackJson);
										class AsyncOnSuccessTask extends AsyncTask<JSONObject, Void, Track>{
											
											@Override
											protected Track doInBackground(
													JSONObject... trackJson) {
												Track t;
												try {
													t = new Track(trackJson[0].getJSONObject("properties").getString("id"));
													t.setDatabaseAdapter(dbAdapter);
													t.setName(trackJson[0].getJSONObject("properties").getString("name"));
													t.setDescription(trackJson[0].getJSONObject("properties").getString("description"));
													t.setCarManufacturer(trackJson[0].getJSONObject("sensor").getJSONObject("properties").getString("manufacturer"));
													t.setCarModel(trackJson[0].getJSONObject("sensor").getJSONObject("properties").getString("model"));
													//include server properties tracks created, modified?
													// TODO more properties
													Measurement recycleMeasurement;

													for (int j = 0; j < trackJson[0]
															.getJSONArray("features")
															.length(); j++) {
														recycleMeasurement = new Measurement(
																Float.valueOf(trackJson[0].getJSONArray("features").getJSONObject(j).getJSONObject("geometry").getJSONArray("coordinates").getString(1)),
																Float.valueOf(trackJson[0].getJSONArray("features").getJSONObject(j).getJSONObject("geometry").getJSONArray("coordinates").getString(0)));

														recycleMeasurement.setMaf((trackJson[0].getJSONArray("features").getJSONObject(j).getJSONObject("properties").getJSONObject("phenomenons").getJSONObject("MAF").getDouble("value")));
														recycleMeasurement.setSpeed((trackJson[0].getJSONArray("features").getJSONObject(j).getJSONObject("properties").getJSONObject("phenomenons").getJSONObject("Speed").getInt("value")));
														recycleMeasurement.setTrack(t);
														t.addMeasurement(recycleMeasurement);
													}
													t.commitTrackToDatabase();
													Log.i("track_id",t.getId()+" "+((DbAdapterRemote) dbAdapter).trackExistsInDatabase(t.getId())+" "+dbAdapter.getNumberOfStoredTracks());
													dlTrackIds.remove(t.getId());
													return t;
												} catch (JSONException e) {
													e.printStackTrace();
												} catch (NumberFormatException e) {
													e.printStackTrace();
												} catch (LocationInvalidException e) {
													e.printStackTrace();
												}
												return null;
											}

											@Override
											protected void onPostExecute(
													Track t) {
												super.onPostExecute(t);
												tracksList.add(t);
												elvAdapter.notifyDataSetChanged();
												ct--;
												if (ct == 0) {
													progress.setVisibility(View.GONE);
												}
											}
											
										}
										//check if the task for dling the track is already running
										try {
											if(!dlTrackIds.contains(trackJson.getJSONObject("properties").getString("id"))){
												dlTrackIds.add(trackJson.getJSONObject("properties").getString("id"));
												new AsyncOnSuccessTask().execute(trackJson);
											}
										} catch (JSONException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										
									}

									public void onFailure(Throwable arg0,
											String arg1) {
										// TODO implement errors
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
				View groupRow = ViewGroup.inflate(getActivity(),
						R.layout.list_tracks_group_layout, null);
				TextView textView = (TextView) groupRow
						.findViewById(R.id.track_name_textview);
				textView.setText(currTrack.getName());
				Button button = (Button) groupRow
						.findViewById(R.id.track_name_go_to_map);
				button.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						Intent intent = new Intent(getActivity()
								.getApplicationContext(), Map.class);
						startActivity(intent);
						Log.i("bla", "bla");

					}
				});
				groupRow.setId(10000000 + i);
				TYPEFACE.applyCustomFont((ViewGroup) groupRow,
						TYPEFACE.Newscycle(getActivity()));
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

				try {
					DateFormat sdf = DateFormat.getDateTimeInstance();
					DecimalFormat twoDForm = new DecimalFormat("#.##");
					DateFormat dfDuration = new SimpleDateFormat("HH:mm:ss:SSS"); // TODO:
																					// leave
																					// out
																					// millis
																					// when
																					// we
																					// have
																					// other
																					// data
					dfDuration.setTimeZone(TimeZone.getTimeZone("UTC"));
					start.setText(sdf.format(currTrack.getStartTime()) + "");
					end.setText(sdf.format(currTrack.getEndTime()) + "");
					Log.e("duration",
							currTrack.getEndTime() - currTrack.getStartTime()
									+ "");
					Date durationMillis = new Date(currTrack.getEndTime()
							- currTrack.getStartTime());
					duration.setText(dfDuration.format(durationMillis) + "");
					length.setText(twoDForm.format(currTrack.getLengthOfTrack())
							+ " km");
					car.setText(currTrack.getCarManufacturer() + " "
							+ currTrack.getCarModel());
					co2.setText("");
				} catch (Exception e) {

				}

				row.setId(10000100 + i + i1);
				TYPEFACE.applyCustomFont((ViewGroup) row,
						TYPEFACE.Newscycle(getActivity()));
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
