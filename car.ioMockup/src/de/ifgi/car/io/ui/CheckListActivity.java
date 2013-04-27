package de.ifgi.car.io.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.ifgi.car.io.R;

public class CheckListActivity extends SherlockActivity implements
		OnClickListener {

	private ActionBar actionBar;
	private int actionBarTitleID = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.checklist_layout);

		actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);

		actionBarTitleID = Utils.getActionBarId();
		if (Utils.getActionBarId() == 0) {
			((TextView) this.findViewById(actionBarTitleID))
					.setTypeface(TYPEFACE.Newscycle(this));
		}

		((TextView) this.findViewById(actionBarTitleID)).setTypeface(TYPEFACE
				.Raleway(this));

		View rootView = findViewById(R.id.checklist_root_layout);
		TYPEFACE.applyCustomFont((ViewGroup) rootView, TYPEFACE.Newscycle(this));

		((TextView) this.findViewById(R.id.welcome)).setTypeface(TYPEFACE
				.Raleway(this));

		this.findViewById(R.id.fourthItem).setOnClickListener(this);
		this.findViewById(R.id.continue_button).setOnClickListener(this);
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
			// finish();
			NavUtils.navigateUpFromSameTask(this);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onClick(View arg0) {
		// up until now, just the login activity is implemented, omitting the
		// switch block
		startActivity(new Intent(CheckListActivity.this, LoginActivity.class));
	}

}