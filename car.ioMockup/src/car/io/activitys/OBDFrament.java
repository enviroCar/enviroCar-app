package car.io.activitys;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import car.io.R;

import com.actionbarsherlock.app.SherlockFragment;

public class OBDFrament extends SherlockFragment {
	

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {	

		return inflater.inflate(R.layout.main, container, false);

	}

}
