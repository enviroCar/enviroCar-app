/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.app.activity;

import org.envirocar.app.R;
import org.envirocar.app.activity.DialogUtil.DialogCallback;
import org.envirocar.app.application.CarManager;
import org.envirocar.app.application.ECApplication;
import org.envirocar.app.application.NavMenuItem;
import org.envirocar.app.application.service.AbstractBackgroundServiceStateReceiver.ServiceState;
import org.envirocar.app.application.service.DeviceInRangeService;
import org.envirocar.app.logging.Logger;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * Outsource of the start/stop button interaction and content updates.
 * objects of this class can be created without leaking any resources - 
 * they simply hold the state-related flags and backlinks to the Activity.
 * 
 * @author matthes rieke
 *
 */
public class StartStopButtonUtil {

	private static final Logger logger = Logger.getLogger(StartStopButtonUtil.class);
	
	private ECApplication application;
	private int trackMode;
	private ServiceState serviceState;
	private Activity activity;
	private boolean deviceDiscoveryActive;
	
	public StartStopButtonUtil(ECApplication app, Activity activity, int trackMode, ServiceState serviceState, boolean deviceDiscoveryActive) {
		this.application = app;
		this.trackMode = trackMode;
		this.serviceState = serviceState;
		this.activity = activity;
		this.deviceDiscoveryActive = deviceDiscoveryActive;
	}
	
	/**
	 * Update the UI contents of the button. This method
	 * DOES NOT fire any service state changes, it is completely
	 * passive.
	 * 
	 * @param button the drawer button
	 */
	public void updateStartStopButtonOnServiceStateChange(NavMenuItem button) {
		logger.info("updateStartStopButtonOnServiceStateChange called with state: "+ serviceState +" / trackMode: "+ trackMode+
				" discovery: "+deviceDiscoveryActive);;
		switch (serviceState) {
			case SERVICE_STOPPED:
				handleServiceStoppedState(button);
				break;
			case SERVICE_DEVICE_DISCOVERY_PENDING:
				handleServiceDeviceDiscoveryPendingState(button);
			case SERVICE_STARTING:
				handleServiceStartingState(button);
				break;
			case SERVICE_STARTED:
				handleServiceStartedState(button);
				break;
			default:
				break;
		}		
	}
	

	/**
	 * React to a button click, considering the current state of the
	 * application and its services. This method fires events
	 * and service starts actively.
	 * 
	 * @param trackModeListener a callback to handle the inputs of the user
	 */
	public void processButtonClick(OnTrackModeChangeListener trackModeListener) {
		logger.info("processButtonClick called with state: "+ serviceState +" / trackMode: "+ trackMode+
				" discovery: "+deviceDiscoveryActive);;
		switch (serviceState) {
			case SERVICE_STOPPED:
				processStoppedStateClick(trackModeListener);
				break;
			case SERVICE_DEVICE_DISCOVERY_PENDING:
				processPendingStateClick();
				break;
			case SERVICE_STARTING:
				processStartingStateClick();
				break;
			case SERVICE_STARTED:
				processStartedStateClick(trackModeListener);
				break;
			default:
				break;
		}		
	}

	/**
	 * convenience method to define the contents of a button,
	 * using String objects.
	 */
	public void defineButtonContents(NavMenuItem button, boolean enabled,
			int iconRes, String subtitle, String title) {
		button.setEnabled(enabled);
		button.setIconRes(iconRes);
		button.setSubtitle(subtitle);
		if (title != null) {
			button.setTitle(title);
		}
	}

	/**
	 * convenience method to define the contents of a button,
	 * using string resource id for subtitle and String for title.
	 */
	public void defineButtonContents(NavMenuItem button, boolean enabled,
			int iconRes, String subtitle) {
		defineButtonContents(button, enabled, iconRes, subtitle, null);		
	}

	private void defineButtonContents(NavMenuItem button, boolean enabled,
			int iconRes, int subtitleRes) {
		defineButtonContents(button, enabled, iconRes, activity.getString(subtitleRes));
	}
	
	private void createStopTrackDialog(final OnTrackModeChangeListener trackModeListener) {
		int titleId;
		int messageId;
		switch (trackMode) {
			case MainActivity.TRACK_MODE_SINGLE:
				titleId = R.string.finish_track;
				messageId = R.string.finish_track_long;
				break;
			case MainActivity.TRACK_MODE_AUTO:
				titleId = R.string.stop_automatic_mode;
				messageId = R.string.stop_automatic_mode_long;
				break;
			default:
				Crouton.makeText(activity, "not supported", Style.INFO).show();
				return;
		}
		
		DialogUtil.createTitleMessageDialog(
				titleId,
				messageId,
				new DialogUtil.PositiveNegativeCallback() {
					@Override
					public void positive() {
						application.stopConnection();
						application.finishTrack();
						trackModeListener.onTrackModeChange(MainActivity.TRACK_MODE_SINGLE);
					}
					
					@Override
					public void negative() {
					}
				}, activity);
    	
	}

	private void createStartTrackDialog(final OnTrackModeChangeListener listener) {
    	String[] items = new String[] {activity.getString(R.string.track_mode_single),
    			activity.getString(R.string.track_mode_auto)};
		DialogUtil.createSingleChoiceItemsDialog(
				activity.getString(R.string.question_track_mode),
				items,
				new DialogCallback() {
					@Override
					public void itemSelected(int which) {
						switch (which) {
							case 0:
								listener.onTrackModeChange(MainActivity.TRACK_MODE_SINGLE);
								break;
							case 1:
								listener.onTrackModeChange(MainActivity.TRACK_MODE_AUTO);
								break;
						}
						
						application.startConnection();
						Crouton.makeText(activity, R.string.start_connection, Style.INFO).show();
					}
					
					@Override
					public void cancelled() {
						
					}
				}, activity);		
	}

	private void handleServiceStoppedState(NavMenuItem button) {
		switch (trackMode) {
			case MainActivity.TRACK_MODE_SINGLE:
				resetToStartButtonState(button);
				break;
			case MainActivity.TRACK_MODE_AUTO:
				if (deviceDiscoveryActive) {
					button.setTitle(activity.getString(R.string.menu_cancel));
					defineButtonContents(button, true, R.drawable.av_stop, "");
				} else {
					resetToStartButtonState(button);
				}
				break;
			default:
				break;
		}
		
	}

	private void handleServiceDeviceDiscoveryPendingState(NavMenuItem button) {
		button.setTitle(activity.getString(R.string.menu_cancel));
		defineButtonContents(button, true, R.drawable.av_cancel, R.string.device_discovery_pending);
	}
	
	private void handleServiceStartingState(NavMenuItem button) {
		button.setTitle(activity.getString(R.string.menu_cancel));
		defineButtonContents(button, true, R.drawable.av_cancel, R.string.menu_starting);
	}

	private void handleServiceStartedState(NavMenuItem button) {
		button.setTitle(activity.getString(R.string.menu_stop));

		int subtitleRes;
		switch (trackMode) {
			case MainActivity.TRACK_MODE_SINGLE:
				subtitleRes = R.string.track_mode_single;
				break;
			case MainActivity.TRACK_MODE_AUTO:
				subtitleRes = R.string.track_mode_auto;
				break;
			default:
				subtitleRes = R.string.track_mode_single;
				break;
		}

		defineButtonContents(button, true, R.drawable.av_stop, subtitleRes);
	}
	
	private void resetToStartButtonState(NavMenuItem button) {
		button.setTitle(activity.getString(R.string.menu_start));
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(activity);

		String remoteDevice = preferences.getString(
				org.envirocar.app.activity.SettingsActivity.BLUETOOTH_KEY,
				null);

		if (remoteDevice != null) {
			if(!CarManager.instance().isCarSet()){
				defineButtonContents(button, false, R.drawable.not_available, R.string.no_sensor_selected);
			} else {
				defineButtonContents(button, true, R.drawable.av_play, preferences.getString(SettingsActivity.BLUETOOTH_NAME, ""));
			}
		} else {
			defineButtonContents(button, false, R.drawable.not_available, R.string.pref_summary_chose_adapter);
		}	
	}
	

	private void processStoppedStateClick(
			OnTrackModeChangeListener onTrackModeChangeListener) {
		createStartTrackDialog(onTrackModeChangeListener);
	}
	
	private void processPendingStateClick() {
		/*
		 * this broadcast stops the DeviceInRangeService
		 */
		Intent intent = new Intent(DeviceInRangeService.STATE_CHANGE);
		intent.putExtra(DeviceInRangeService.STATE_CHANGE, false);
		activity.sendBroadcast(intent);
	}
	
	private void processStartingStateClick() {
		application.stopConnection();
		Crouton.makeText(activity, R.string.stop_connection, Style.INFO).show();		
	}

	private void processStartedStateClick(OnTrackModeChangeListener l) {
		createStopTrackDialog(l);
	}
	
	
	/*
	 * CALLBACK INTERFACES
	 */
	public interface OnTrackModeChangeListener {

		/**
		 * called when the mode changes on user input or
		 * a certain state change.
		 * 
		 * @param trackModeSingle the track mode
		 */
		void onTrackModeChange(int trackModeSingle);
		
	}
	
}
