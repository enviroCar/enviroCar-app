package de.ifgi.car.io.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.support.v4.view.PagerTabStrip;

import de.ifgi.car.io.R;

public class MainActivity extends SwipeableFragmentActivity {

	private int actionBarTitleID = 0;
	private ActionBar actionBar;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        int pageMargin = (int) (4 * getResources().getDisplayMetrics().density);
        viewPager.setPageMargin(pageMargin);
        viewPager.setPageMarginDrawable(R.drawable.viewpager_margin);
        
        actionBar = getSupportActionBar();
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(false);
        
        ((PagerTabStrip) this.findViewById(R.id.pager_title_strip)).setTabIndicatorColorResource(R.color.blue_light_cario);
        
		// font stuff
		actionBarTitleID = Utils.getActionBarId();
		if (Utils.getActionBarId() != 0) {
			((TextView) this.findViewById(actionBarTitleID))
					.setTypeface(TYPEFACE.Newscycle(this));
		}
		View rootView = findViewById(R.id.pager_title_strip);
		TYPEFACE.applyCustomFont((ViewGroup) rootView, TYPEFACE.Newscycle(this));
 
        addTab( "Data", MyData.class, MyData.createBundle( "My Data") );
        addTab( "Overview", MyData.class, MyData.createBundle( "Overview") );
        addTab( "Friends", MyData.class, MyData.createBundle( "Fragment 3") );
        
        setSelectedTab(1);
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
				startActivity(new Intent(MainActivity.this,CheckListActivity.class));
			break;
			case R.id.menu_settings:
				startActivity(new Intent(MainActivity.this,SettingsActivity.class));
			break;			
		}
		return true;
	}
}
