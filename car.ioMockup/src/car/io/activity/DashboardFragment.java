package car.io.activity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import car.io.R;
import car.io.adapter.DbAdapter;
import car.io.application.ECApplication;
import car.io.application.RestClient;
import car.io.views.RoundProgress;
import car.io.views.TYPEFACE;

import com.actionbarsherlock.app.SherlockFragment;
import com.loopj.android.http.JsonHttpResponseHandler;

public class DashboardFragment extends SherlockFragment {

	TextView speedTextView;
	RoundProgress roundProgressSpeed;
	TextView co2TextView;
	RoundProgress roundProgressCO2;
	DbAdapter dbAdapter;
	ECApplication application;
	private Spinner sensorSpinner;
	private ProgressBar sensorDlProgress;
	private Button sensorRetryButton;
	int speed;
	int speedProgress;
	double co2;
	double co2Progress;
	
	private JSONArray sensors;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.layout.dashboard, container, false);

	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {

		super.onViewCreated(view, savedInstanceState);

		application = ((ECApplication) getActivity().getApplication());

		dbAdapter = ((ECApplication) getActivity().getApplication())
				.getDbAdapterLocal();

		co2TextView = (TextView) getView().findViewById(R.id.co2TextView);
		speedTextView = (TextView) getView().findViewById(
				R.id.textViewSpeedDashboard);

		roundProgressCO2 = (RoundProgress) getView().findViewById(
				R.id.blue_progress_bar);
		roundProgressSpeed = (RoundProgress) getView().findViewById(
				R.id.blue_progress_bar2);
		
		sensorSpinner = (Spinner) getView().findViewById(R.id.dashboard_current_sensor_spinner);
		//this rather difficult code is to ensure that the event is only fired for selection
		sensorSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			private boolean firstSelect = true;
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, 
		            int pos, long id) {
				if(!firstSelect){
					Log.i("item",parent.getItemAtPosition(pos)+"");
					
					try {
						application.updateCurrentSensor(((JSONObject) parent.getItemAtPosition(pos)).getJSONObject("properties").getString("id"),
								((JSONObject) parent.getItemAtPosition(pos)).getJSONObject("properties").getString("manufacturer"),
								((JSONObject) parent.getItemAtPosition(pos)).getJSONObject("properties").getString("model"),
								((JSONObject) parent.getItemAtPosition(pos)).getJSONObject("properties").getString("fuelType"),
								((JSONObject) parent.getItemAtPosition(pos)).getJSONObject("properties").getInt("constructionYear"));
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}else{
					firstSelect = false;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				//TODO do something
			}
		});
		sensorDlProgress = (ProgressBar) getView().findViewById(R.id.sensor_dl_progress);
		sensorRetryButton = (Button) getView().findViewById(R.id.retrybutton);
		sensorRetryButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				dlSensors();
			}
		});
		
		dlSensors();

		// Handle the UI updates

		final Handler handler = new Handler();
		Runnable runnable = new Runnable() {
			@Override
			public void run() {

				// Deal with the speed values

				speed = application.getSpeedMeasurement();
				speedTextView.setText(speed + " km/h");
				if (speed <= 0)
					speedProgress = 0;
				else if (speed > 200)
					speedProgress = 100;
				else
					speedProgress = speed / 2;
				roundProgressSpeed.setProgress(speedProgress);

				// Deal with the co2 values

				co2 = application.getCo2Measurement();
				co2TextView.setText(co2 + " unit"); // TODO work out unit
													// precisely
				if (co2 <= 0)
					co2Progress = 0;
				else if (co2 > 200)
					co2Progress = 100;
				else
					co2Progress = co2 / 2;
				roundProgressCO2.setProgress(co2Progress);

				// Repeat this in x ms
				handler.postDelayed(this, 1000);
			}
		};
		handler.postDelayed(runnable, 1000);

		TYPEFACE.applyCustomFont((ViewGroup) view,
				TYPEFACE.Newscycle(getActivity()));

	}
	
	private void dlSensors(){
		sensorDlProgress.setVisibility(View.VISIBLE);
		sensorSpinner.setVisibility(View.GONE);
		sensorRetryButton.setVisibility(View.GONE);
		
		RestClient.downloadSensors(new JsonHttpResponseHandler() {
			
			
			@Override
			public void onFailure(Throwable error, String content) {
				super.onFailure(error, content);
				sensorDlProgress.setVisibility(View.GONE);
				sensorRetryButton.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onSuccess(JSONObject response) {
				super.onSuccess(response);
				try {
					sensors = response.getJSONArray("sensors");
					sensorSpinner.setAdapter(new SensorAdapter());
					sensorDlProgress.setVisibility(View.GONE);
					sensorSpinner.setVisibility(View.VISIBLE);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
	}
	

	private class SensorAdapter extends BaseAdapter implements SpinnerAdapter {

        @Override
        public int getCount() {
            return sensors.length();
        }

        @Override
        public Object getItem(int position) {
            try {
				return sensors.get(position);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            TextView text = new TextView(DashboardFragment.this.getActivity());
            text.setText(getItem(position).toString());
            return text;
        }

    }	

}
