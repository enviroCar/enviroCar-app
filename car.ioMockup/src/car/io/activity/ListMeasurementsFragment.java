package car.io.activity;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
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
import car.io.views.Utils;

import com.actionbarsherlock.app.SherlockFragment;
import com.loopj.android.http.JsonHttpResponseHandler;

public class ListMeasurementsFragment extends SherlockFragment {

	private ArrayList<Track> tracksList;
	private TracksListAdapter elvAdapter;
	private DbAdapter dbAdapter;
	private ExpandableListView elv;

	private ProgressBar progress;

	public View onCreateView(android.view.LayoutInflater inflater,
			android.view.ViewGroup container,
			android.os.Bundle savedInstanceState) {

		dbAdapter = ((ECApplication) getActivity().getApplication()).getDbAdapterRemote();

		View v = inflater.inflate(R.layout.list_tracks_layout, null);
		elv = (ExpandableListView) v.findViewById(R.id.list);
		progress = (ProgressBar) v.findViewById(R.id.listprogress);
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
						t.setDatabaseAdapter(dbAdapter);
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
								return dbAdapter.getTrack(params[0]);
							}
							
							protected void onPostExecute(Track result) {
								tracksList.add(result);
								elvAdapter.notifyDataSetChanged();
								afterOneTrack();
							}
							
						}
						if (((DbAdapterRemote) dbAdapter).trackExistsInDatabase(((JSONObject) tracks.get(i)).getString("id"))) {
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
				textView.setText(currTrack.getName());
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
					length.setText(twoDForm.format(currTrack.getLengthOfTrack())
							+ " km");
					car.setText(currTrack.getCarManufacturer() + " "
							+ currTrack.getCarModel());
					description.setText(currTrack.getDescription());
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
