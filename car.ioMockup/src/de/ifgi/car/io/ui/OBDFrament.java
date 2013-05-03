package de.ifgi.car.io.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;

import de.ifgi.car.io.R;

public class OBDFrament extends SherlockFragment {
	

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {	

		return inflater.inflate(R.layout.main, container, false);

	}

}
