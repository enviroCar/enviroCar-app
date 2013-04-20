package de.ifgi.car.iomockup;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.View;
import android.view.View.OnClickListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class CheckListActivity extends SherlockActivity implements OnClickListener {

	private ActionBar actionBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.checklist_layout);

		actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		this.findViewById(R.id.fourthItem).setOnClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {

	    case android.R.id.home:
	         //finish();
	    	NavUtils.navigateUpFromSameTask(this);
	         return true;


	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

	@Override
	public void onClick(View arg0) {
		//up until now, just the login activity is implemented, omitting the switch block
		startActivity(new Intent(CheckListActivity.this, LoginActivity.class));
	}
	

}