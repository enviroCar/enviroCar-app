package car.io.activity;

import org.apache.http.HttpStatus;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.widget.TextView;
import car.io.R;
import car.io.views.TYPEFACE;
import car.io.views.Utils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends SherlockFragmentActivity {
	
	static final int REQUEST_MY_GARAGE = 1000;
	
	private int actionBarTitleID = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		
		// font stuff
		actionBarTitleID = Utils.getActionBarId();
		if (Utils.getActionBarId() != 0) {
			((TextView) this.findViewById(actionBarTitleID))
					.setTypeface(TYPEFACE.Newscycle(this));
		}
		//TODO style tabs

		actionBar.setLogo(getResources().getDrawable(R.drawable.home_icon));
		
		
		actionBar.setDisplayHomeAsUpEnabled(true);

		Tab tabA = actionBar.newTab();
		tabA.setText(getResources().getString(R.string.action_sign_in_short));
		tabA.setTabListener(new TabListener<LoginFragment>(this, "Login",
				LoginFragment.class));
		actionBar.addTab(tabA);

		Tab tabB = actionBar.newTab();
		tabB.setText(getResources().getString(R.string.action_sign_in_register));
		tabB.setTabListener(new TabListener<RegisterFragment>(this, "Register",
				RegisterFragment.class));
		actionBar.addTab(tabB);		

		if (savedInstanceState != null) {
			int savedIndex = savedInstanceState.getInt("SAVED_INDEX");
			getSupportActionBar().setSelectedNavigationItem(savedIndex);
		}

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			// TODO: If Settings has multiple levels, Up should navigate up
			// that hierarchy.
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getSupportMenuInflater().inflate(R.menu.login, menu);
		return true;
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("SAVED_INDEX", getSupportActionBar()
				.getSelectedNavigationIndex());
	}
	
	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		super.onActivityResult (requestCode,resultCode, data);
		switch(requestCode){
		case REQUEST_MY_GARAGE:
			if(resultCode == HttpStatus.SC_CREATED){
				finish();
			}
			break;
		case MainActivity.REQUEST_MY_GARAGE:
			finish();
			break;
		}
		
	}

	public static class TabListener<T extends SherlockFragment> implements
			ActionBar.TabListener {

		private final FragmentActivity myActivity;
		private final String myTag;
		private final Class<T> myClass;

		public TabListener(FragmentActivity activity, String tag, Class<T> cls) {
			myActivity = activity;
			myTag = tag;
			myClass = cls;
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {

			Fragment myFragment =  myActivity.getSupportFragmentManager()
					.findFragmentByTag(myTag);

			// Check if the fragment is already initialized
			if (myFragment == null) {
				// If not, instantiate and add it to the activity
				myFragment = Fragment
						.instantiate(myActivity, myClass.getName());
				ft.add(android.R.id.content, myFragment, myTag);
			} else {
				// If it exists, simply attach it in order to show it
				ft.attach(myFragment);
			}

		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {

			Fragment myFragment = myActivity.getSupportFragmentManager()
					.findFragmentByTag(myTag);

			if (myFragment != null) {
				// Detach the fragment, because another one is being attached
				ft.detach(myFragment);
			}

		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {

		}

	}
}
