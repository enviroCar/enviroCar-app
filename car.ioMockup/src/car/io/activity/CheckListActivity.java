package car.io.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import car.io.R;
import car.io.views.CheckListItem;
import car.io.views.TYPEFACE;
import car.io.views.Utils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class CheckListActivity extends SherlockActivity implements
		OnClickListener {

	private ActionBar actionBar;
	private int actionBarTitleID = 0;

	private CheckListItem bluetooth;
	private CheckListItem obdPair;
	private CheckListItem obdService;
	private CheckListItem obdConnection;
	private CheckListItem gps;
	private CheckListItem login;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.checklist_layout);
		// actionbar stuff..
		actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);

		// font stuff
		actionBarTitleID = Utils.getActionBarId();
		if (Utils.getActionBarId() != 0) {
			((TextView) this.findViewById(actionBarTitleID))
					.setTypeface(TYPEFACE.Newscycle(this));
		}

		View rootView = findViewById(R.id.checklist_root_layout);
		TYPEFACE.applyCustomFont((ViewGroup) rootView, TYPEFACE.Newscycle(this));

		((TextView) this.findViewById(R.id.welcome)).setTypeface(TYPEFACE
				.Raleway(this));

		setupViews();

	}
	
	@Override
	protected void onResume() {
		super.onResume();
		initCheckAll();
	}

	private void setupViews() {
		bluetooth = (CheckListItem) this.findViewById(R.id.checklist_bluetooth);
		obdPair = (CheckListItem) this.findViewById(R.id.checklist_obd_pair);
		obdService = (CheckListItem) this
				.findViewById(R.id.checklist_obd_service);
		obdConnection = (CheckListItem) this
				.findViewById(R.id.checklist_obd_connection);
		gps = (CheckListItem) this.findViewById(R.id.checklist_gps);
		login = (CheckListItem) this.findViewById(R.id.checklist_login);
		
		bluetooth.setOnClickListener(this);
		obdPair.setOnClickListener(this);
		obdService.setOnClickListener(this);
		obdConnection.setOnClickListener(this);
		gps.setOnClickListener(this);
		login.setOnClickListener(this);
		
		login.setState(CheckListItem.STATUS_PROBLEM);
		initCheckAll();
	}

	private void initCheckAll() {
		checkBluetooth();
		checkGPS();
	}

	private void checkGPS() {
		if (Utils.isGPSEnabled(this)) {
			gps.setState(CheckListItem.STATUS_CLEAR);
			gps.setText(R.string.checklist_item_gps_enabled);
		} else {
			gps.setState(CheckListItem.STATUS_ERROR);
			gps.setText(R.string.checklist_item_gps_disabled);
		}
	}

	private void checkBluetooth() {
		if (Utils.isBluetoothEnabled(this)) {
			bluetooth.setState(CheckListItem.STATUS_CLEAR);
			bluetooth.setText(R.string.checklist_item_bluetooth_enabled);
		} else {
			bluetooth.setState(CheckListItem.STATUS_ERROR);
			bluetooth.setText(R.string.checklist_item_bluetooth_disabled);
		}
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
				NavUtils.navigateUpFromSameTask(this);
				return true;
	
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
		case R.id.checklist_bluetooth:
			startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));
			break;
		case R.id.checklist_gps:
			startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			break;
		case R.id.checklist_login:
			startActivity(new Intent(CheckListActivity.this, LoginActivity.class));
			break;
		case R.id.checklist_obd_connection:
			break;
		case R.id.checklist_obd_pair:
			break;
		case R.id.checklist_obd_service:
			break;
		}
		
	}

}