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

package org.envirocar.app.application;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.envirocar.app.R;
import org.envirocar.app.activity.MainActivity;
import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.dao.CacheDirectoryProvider;
import org.envirocar.app.dao.DAOProvider;
import org.envirocar.app.logging.ACRACustomSender;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.storage.DbAdapterImpl;
import org.envirocar.app.storage.Track;
import org.envirocar.app.util.NamedThreadFactory;
import org.envirocar.app.util.Util;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

/**
 * This is the main application that is the central linking component for all adapters, services and so on.
 * This application is implemented like a singleton, it exists only once while the app is running.
 * @author gerald, jakob
 *
 */
@ReportsCrashes(formKey = "")
public class ECApplication extends Application {
	
	private static final Logger logger = Logger.getLogger(ECApplication.class);
	
	// Strings
	
	public static final String BASE_URL = "https://envirocar.org/api/dev";
//	public static final String BASE_URL = "http://192.168.1.148:8080/webapp-1.1.0-SNAPSHOT";

	private SharedPreferences preferences = null;
	
	// Helpers and objects

//	private DbAdapter dbAdapterLocal;
//	private DbAdapter dbAdapterRemote;
	private final ScheduledExecutorService scheduleTaskExecutor = Executors
			.newScheduledThreadPool(1, new NamedThreadFactory("ECApplication-Factory"));
	private BluetoothAdapter bluetoothAdapter = BluetoothAdapter
			.getDefaultAdapter();

	private int mId = 1133;
	
	protected boolean adapterConnected;
	private Activity currentActivity;
	


	/**
	 * returns the current activity.
	 * @return
	 */
	public Activity getCurrentActivity(){
		return currentActivity;
	}
	

	public void setActivity(Activity a){
		this.currentActivity = a;
	}
	
	/**
	 * Returns whether requirements were fulfilled (bluetooth activated)
	 * @return requirementsFulfilled?
	 */
	public boolean bluetoothActivated() {
		if (bluetoothAdapter == null) {
			logger.warn("Bluetooth disabled");
			return false;
		} else {
			logger.info("Bluetooth enabled");
			return bluetoothAdapter.isEnabled();
		}
	}


	@Override
	public void onCreate() {
		Logger.initialize(Util.getVersionString(this));
		super.onCreate();
		
		try {
			DbAdapterImpl.init(getApplicationContext());
		} catch (InstantiationException e) {
			logger.warn("Could not initalize the database layer. The app will probably work unstable.");
			logger.warn(e.getMessage(), e);
		}
		
		preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		DAOProvider.init(new ContextInternetAccessProvider(getApplicationContext()),
				new CacheDirectoryProvider() {
					@Override
					public File getBaseFolder() {
						return Util.resolveCacheFolder(getApplicationContext());
					}
				});
		
		UserManager.init(getApplicationContext());
		initializeErrorHandling();
		CarManager.init(preferences);
		TermsOfUseManager.instance();
		
		// Make a new commandListener to interpret the measurement values that are
		// returned
		logger.info("init commandListener");
		
	}
	
	private void initializeErrorHandling() {
		ACRA.init(this);
		ACRACustomSender yourSender = new ACRACustomSender();
		ACRA.getErrorReporter().setReportSender(yourSender);
		ACRA.getConfig().setExcludeMatchingSharedPreferencesKeys(SettingsActivity.resolveIndividualKeys());
	}

	/**
	 * Stop the service connector and therefore the scheduled tasks.
	 */
	public void shutdownServiceConnector() {
		scheduleTaskExecutor.shutdown();
	}


	
	/**
	 * 
	 * @action Can also contain the http status code with error if fail
	 */
	public void createNotification(String action) {
		String notification_text = "";
		if(action.equals("success")){
			notification_text = getString(R.string.upload_notification_success);
		}else if(action.equals("start")){
			notification_text = getString(R.string.upload_notification);
		}else{
			notification_text = action;
		}
		
		Intent intent = new Intent(this,MainActivity.class);
		PendingIntent pintent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
		
		NotificationCompat.Builder mBuilder =
		        new NotificationCompat.Builder(this)
		        .setSmallIcon(R.drawable.ic_launcher)
		        .setContentTitle("enviroCar")
		        .setContentText(notification_text)
		        .setContentIntent(pintent)
		        .setTicker(notification_text);
		
		NotificationManager mNotificationManager =
		    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.notify(mId, mBuilder.build());

	  }
	
	
	public void resetTrack() {
		//TODO somehow let the CommandListener know of the reset
	}


	public void finishTrack() {
		final Track track = DbAdapterImpl.instance().finishCurrentTrack();
		
		getCurrentActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (track != null) {
					if (track.getLastMeasurement() == null) {
						Crouton.makeText(getCurrentActivity(), R.string.track_finished_no_measurements, Style.ALERT).show();
					} else {
						String text = getString(R.string.track_finished).concat(track.getName());
						Crouton.makeText(getCurrentActivity(), text, Style.INFO).show();				
					}
				}
				else {
					Crouton.makeText(getCurrentActivity(), R.string.track_finishing_failed, Style.ALERT).show();
				}				
			}
		});
	}


}
