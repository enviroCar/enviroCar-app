package de.ifgi.car.iomockup;

import java.lang.reflect.Field;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewConfiguration;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class MyData extends SherlockActivity {
	
	private ActionBar actionBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mydata_layout);

		// enable drop-down navigation
		SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(this,
				R.array.dropdownnavigation,
				R.layout.sherlock_spinner_dropdown_item);
				
		actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(mSpinnerAdapter, null);
		actionBar.setSelectedNavigationItem(0);
		actionBar.setTitle("");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i("itemid",item.getItemId()+"");
		switch(item.getItemId()){
			case R.id.menu_help:
				startActivity(new Intent(MyData.this,CheckListActivity.class));
			break;
			case R.id.menu_settings:
				startActivity(new Intent(MyData.this,SettingsActivity.class));
			break;			
		}
		return true;
	}

}
