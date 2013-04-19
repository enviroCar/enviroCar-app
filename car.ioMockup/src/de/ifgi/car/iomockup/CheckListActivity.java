package de.ifgi.car.iomockup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class CheckListActivity extends SherlockActivity {

	private ActionBar actionBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.checklist_layout);
		
		
//		OnNavigationListener  mOnNavigationListener = new OnNavigationListener() {
//			  // Get the same strings provided for the drop-down's ArrayAdapter
//			  String[] strings = getResources().getStringArray(R.array.dropdownnavigation);
//
//			  @Override
//			  public boolean onNavigationItemSelected(int position, long itemId) {
//				switch(position){
//				case 0:
//					//startActivity(new Intent(CheckListActivity.this,MyData.class));
//					break;
//				}
//			    return true;
//
//			  }
//			};
//
//		// enable drop-down navigation
//		SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(this,
//				R.array.dropdownnavigation,
//				R.layout.sherlock_spinner_dropdown_item);
//				
		actionBar = getSupportActionBar();
//		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
//		actionBar.setListNavigationCallbacks(mSpinnerAdapter,mOnNavigationListener);
		
		//actionBar.setTitle("Checklist");
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}


	

}