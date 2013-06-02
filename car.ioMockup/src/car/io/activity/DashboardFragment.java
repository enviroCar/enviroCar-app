package car.io.activity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import car.io.R;
import car.io.adapter.DbAdapter;
import car.io.adapter.Measurement;
import car.io.application.ECApplication;
import car.io.exception.MeasurementsException;
import car.io.views.RoundProgress;
import car.io.views.TYPEFACE;

import com.actionbarsherlock.app.SherlockFragment;

public class DashboardFragment extends SherlockFragment {

	TextView speedTextView;
	RoundProgress roundProgressSpeed;
	TextView co2TextView;
	RatingBar drivingStyle;
	RoundProgress roundProgressCO2;
	ImageView image;
	DbAdapter dbAdapter;

	// public ECApplication application;

	// int index = 20;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.layout.dashboard, container, false);

	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {

		super.onViewCreated(view, savedInstanceState);

		// application = ((ECApplication) getApplication()).getInstance();
		// dbAdapter = (DbAdapter) application.getDbAdapterLocal();
		dbAdapter = ((ECApplication) getActivity().getApplication())
				.getInstance().getDbAdapterLocal();

		co2TextView = (TextView) getView().findViewById(R.id.co2TextView);
		speedTextView = (TextView) getView().findViewById(
				R.id.textViewSpeedDashboard);
		// co2TextView.setText("Bla");

		roundProgressCO2 = (RoundProgress) getView().findViewById(
				R.id.blue_progress_bar);
		roundProgressSpeed = (RoundProgress) getView().findViewById(
				R.id.blue_progress_bar2);

		drivingStyle = (RatingBar) getView().findViewById(R.id.ratingBar1);

		image = (ImageView) getView().findViewById(R.id.imageView1);

		/*
		 * The following code for the sheduledexecutorservice is a performance
		 * killer as well. We have to do this differently.
		 */
		// TODO: update speed and co2 values
		// ScheduledExecutorService uploadTaskExecutor = Executors
		// .newScheduledThreadPool(1);
		// uploadTaskExecutor.scheduleAtFixedRate(new Runnable() {
		//
		// @Override
		// public void run() {
		// try {
		// Measurement lastMeasurement = dbAdapter.getLastUsedTrack()
		// .getLastMeasurement();
		//
		// if ((System.currentTimeMillis() - lastMeasurement
		// .getMeasurementTime()) < 10000) {
		// speedTextView.setText(lastMeasurement.getSpeed()
		// + " km/h");
		// roundProgressSpeed.setProgress(lastMeasurement
		// .getSpeed() / 2);
		// } else {
		// speedTextView.setText("");
		// roundProgressSpeed.setProgress(0);
		// }
		//
		// } catch (MeasurementsException e) {
		// speedTextView.setText("");
		// roundProgressSpeed.setProgress(0);
		// }
		//
		// }
		// }, 0, 2, TimeUnit.SECONDS);

		// image.setImageResource(R.drawable.bigmoney);
		// index = 80;
		// doMockupDemo(index);

		// image.setOnClickListener(new OnClickListener() {
		//
		// @Override
		// public void onClick(View arg0) {
		// if (index < 80) {
		// index = index + 20;
		// doMockupDemo(index);
		// } else {
		// index = 20;
		// doMockupDemo(index);
		// }
		// }
		// });

		TYPEFACE.applyCustomFont((ViewGroup) view,
				TYPEFACE.Newscycle(getActivity()));

	}

	// /**
	// * Do a mockup demo
	// *
	// */
	// private void doMockupDemo(int size) {
	// int co2Value = size;
	// roundProgress.setProgress(co2Value);
	// co2TextView.setText(String.valueOf(co2Value) + " kg/h");
	// drivingStyle.setRating(5.0f - (float) size / 16);
	// if (size < 30) {
	// image.setImageResource(R.drawable.smallmoney);
	// }
	// if (size > 30 && size < 70) {
	// image.setImageResource(R.drawable.mediummoney);
	// }
	// if (size > 70) {
	// image.setImageResource(R.drawable.bigmoney);
	// }
	// }

}
