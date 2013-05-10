package car.io.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import car.io.R;
import car.io.views.RoundProgress;

import com.actionbarsherlock.app.SherlockFragment;

public class DashboardFragment extends SherlockFragment {

	TextView co2TextView;
	RatingBar drivingStyle;
	RoundProgress roundProgress;
	ImageView image;
	int index = 20;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.layout.dashboard, container, false);

	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {

		super.onViewCreated(view, savedInstanceState);

		co2TextView = (TextView) getView().findViewById(R.id.co2TextView);
		co2TextView.setText("Bla");

		roundProgress = (RoundProgress) getView().findViewById(
				R.id.blue_progress_bar);

		drivingStyle = (RatingBar) getView().findViewById(R.id.ratingBar1);

		image = (ImageView) getView().findViewById(R.id.imageView1);
		image.setImageResource(R.drawable.bigmoney);
		index = 80;
		doMockupDemo(index);

		image.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (index < 80) {
					index = index + 20;
					doMockupDemo(index);
				} else {
					index = 20;
					doMockupDemo(index);
				}

			}
		});

	}

	/**
	 * Do a mockup demo
	 * 
	 * TODO animate this
	 */
	private void doMockupDemo(int size) {
		int co2Value = size;
		roundProgress.setProgress(co2Value);
		co2TextView.setText(String.valueOf(co2Value) + " kg/h");
		drivingStyle.setRating(5.0f - (float) size / 16);
		if (size < 30) {
			image.setImageResource(R.drawable.smallmoney);
		}
		if (size > 30 && size < 70) {
			image.setImageResource(R.drawable.mediummoney);
		}
		if (size > 70) {
			image.setImageResource(R.drawable.bigmoney);
		}
	}

}
