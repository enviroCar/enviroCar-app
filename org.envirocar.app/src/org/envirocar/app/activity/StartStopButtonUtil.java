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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class StartStopButtonUtil {

	private ECApplication application;
	private int trackMode;
	private ServiceState serviceState;
	private Activity activity;
	
	public StartStopButtonUtil(ECApplication app, Activity activity, int trackMode, ServiceState serviceState) {
		this.application = app;
		this.trackMode = trackMode;
		this.serviceState = serviceState;
		this.activity = activity;
	}
	
	public void createStopTrackDialog() {
    	Intent intent;
		switch (trackMode) {
		case MainActivity.TRACK_MODE_SINGLE:
			DialogUtil.createTitleMessageDialog(
					R.string.finish_track,
					R.string.finish_track_long,
					new DialogUtil.PositiveNegativeCallback() {
						@Override
						public void positive() {
							application.stopConnection();
							application.finishTrack();
						}
						
						@Override
						public void negative() {
						}
					}, activity);	
			break;
		case MainActivity.TRACK_MODE_AUTO:
			/*
			 * TODO DIALOG!!
			 */
			intent = new Intent(DeviceInRangeService.STATE_CHANGE);
			intent.putExtra(DeviceInRangeService.STATE_CHANGE, false);
			activity.sendBroadcast(intent);
			break;
		default:
			Crouton.makeText(activity, "not supported", Style.INFO).show();
			break;
		}
    	
	}

	public void createStartTrackDialog(final OnTrackModeChangeListener listener) {
    	String[] items = new String[] {activity.getString(R.string.track_mode_single),
    			activity.getString(R.string.track_mode_auto)};
		DialogUtil.createSingleChoiceItemsDialog(
				activity.getString(R.string.question_track_mode),
				items,
				new DialogCallback() {
					@Override
					public void itemSelected(int which) {
						Intent intent;
						switch (which) {
						case 0:
							application.startConnection();
							Crouton.makeText(activity, R.string.start_connection, Style.INFO).show();
							listener.onTrackModeChange(MainActivity.TRACK_MODE_SINGLE);
							break;
						case 1:
							application.startConnection();
							intent = new Intent(DeviceInRangeService.STATE_CHANGE);
							intent.putExtra(DeviceInRangeService.STATE_CHANGE, true);
							activity.sendBroadcast(intent);
							Crouton.makeText(activity, R.string.start_connection, Style.INFO).show();
							listener.onTrackModeChange(MainActivity.TRACK_MODE_AUTO);
							break;
						}
					}
					
					@Override
					public void cancelled() {
						
					}
				}, activity);		
	}

	public void updateStartStopButton(NavMenuItem button) {
		switch (serviceState) {
		case SERVICE_STARTED:
			handleServiceStartedState(button);
			break;
		case SERVICE_STARTING:
			handleServiceStartingState(button);
			break;
		case SERVICE_STOPPED:
			handleServiceStoppedState(button);
			break;
		default:
			break;
		}		
	}

	public void defineButtonContents(NavMenuItem button, boolean enabled,
			int iconRes, String subtitle, String title) {
		button.setEnabled(enabled);
		button.setIconRes(iconRes);
		button.setSubtitle(subtitle);
		if (title != null) {
			button.setTitle(title);
		}
	}

	public void defineButtonContents(NavMenuItem button, boolean enabled,
			int iconRes, String subtitle) {
		defineButtonContents(button, enabled, iconRes, subtitle, null);		
	}

	private void defineButtonContents(NavMenuItem button, boolean enabled,
			int iconRes, int subtitleRes) {
		defineButtonContents(button, enabled, iconRes, activity.getString(subtitleRes));
	}

	private void handleServiceStartingState(NavMenuItem button) {
		button.setTitle(activity.getString(R.string.menu_cancel));
		button.setSubtitle(activity.getString(R.string.menu_starting));
		button.setIconRes(R.drawable.av_cancel);
		button.setEnabled(true);
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
	
	private void handleServiceStoppedState(NavMenuItem button) {
		button.setTitle(activity.getString(R.string.menu_start));
		// Only enable start button when adapter is selected

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
	
	public interface OnTrackModeChangeListener {

		void onTrackModeChange(int trackModeSingle);
		
	}
	
}
