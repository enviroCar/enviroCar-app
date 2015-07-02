///*
// * enviroCar 2013
// * Copyright (C) 2013
// * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
// *
// * This program is free software; you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation; either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software Foundation,
// * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
// *
// */
//package org.envirocar.app.application.service;
//
//
//import org.envirocar.app.BaseMainActivity;
//import org.envirocar.app.R;
//
//import org.envirocar.app.activity.SettingsActivity;
//import org.envirocar.app.application.service.AbstractBackgroundServiceStateReceiver.ServiceState;
//import org.envirocar.app.logging.Logger;
//
//import android.app.Notification;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.app.Service;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.content.BroadcastReceiver;
//import android.content.ComponentName;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.content.ServiceConnection;
//import android.content.SharedPreferences;
//import android.os.Binder;
//import android.os.Handler;
//import android.os.IBinder;
//import android.preference.PreferenceManager;
//import android.support.v4.app.NotificationCompat;
//
///**
// * backgroundService for managing the auto-discovery of the
// * specified OBD-II bluetooth device.
// *
// * @author matthes rieke
// *
// */
//public class DeviceInRangeService extends Service {
//
//	private static final Logger logger = Logger.getLogger(DeviceInRangeService.class);
//
//	private static final long DISCOVERY_PERIOD = 1000 * 60 * 2;
//
//	protected boolean autoConnect;
//
//	private final BroadcastReceiver receiver = new BroadcastReceiver() {
//		@Override
//		public void onReceive(Context context, Intent intent) {
//			String action = intent.getAction();
//
//			// When discovery finds a device
//			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//				verifyRemoteDevice(intent);
//			}
//
//		}
//	};
//
//
//
//	private Runnable discoveryRunnable;
//	protected boolean discoveryEnabled = false;
//	private Handler discoveryHandler;
//
//	protected BackgroundServiceInteractor backgroundService;
//
//	private long targetSystemTime;
//
//	@Override
//	public void onCreate() {
//		logger.info("onCreate " + getClass().getName() +"; Hash: "+System.identityHashCode(this));
//		registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
//
//		discoveryHandler = new Handler();
//
//		bindToBackgroundService();
//	}
//
//
//	private void bindToBackgroundService() {
//		if (!bindService(new Intent(this, BackgroundServiceImpl.class),
//				new ServiceConnection() {
//
//					@Override
//					public void onServiceDisconnected(ComponentName name) {
//						logger.info(String.format("BackgroundService %S disconnected!", name
//                                .flattenToString()));
//					}
//
//					@Override
//					public void onServiceConnected(ComponentName name, IBinder service) {
//						backgroundService = (BackgroundServiceInteractor) service;
//					}
//				}, 0)) {
//			logger.warn("Could not connect to BackgroundService.");
//		}
//	}
//
//	@Override
//	public void onDestroy() {
//		logger.info("onDestroy " + getClass().getName() +"; Hash: "+System.identityHashCode(this));
//		unregisterReceiver(receiver);
//
//		NotificationManager notificationManager = (NotificationManager)
//				  getSystemService(NOTIFICATION_SERVICE);
//
//		notificationManager.cancel(BackgroundServiceImpl.BG_NOTIFICATION_ID);
//
//		discoveryEnabled = false;
//		discoveryHandler.removeCallbacks(discoveryRunnable);
//	}
//
//
//	protected void verifyRemoteDevice(Intent intent) {
//		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//		String targetDeviceFromSettings = preferences.getString(SettingsActivity.BLUETOOTH_KEY, null);
//
//		if (targetDeviceFromSettings != null) {
//			BluetoothDevice discoveredDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//			logger.info("Found Device: "+discoveredDevice.getName() +" / "+discoveredDevice.getAddress());
//			if (targetDeviceFromSettings.equals(discoveredDevice.getAddress())) {
//				initializeConnection(discoveredDevice);
//			}
//		}
//	}
//
//	private void initializeConnection(BluetoothDevice discoveredDevice) {
//		stopSelf();
//
//		startService(new Intent(getApplicationContext(), BackgroundServiceImpl.class));
//	}
//
//	@Override
//	public int onStartCommand(Intent intent, int flags, int startId) {
//		logger.info("onStartCommand " + getClass().getName() +"; Hash: "+System.identityHashCode(this));
//
//		discoveryEnabled = true;
//
//		startWithDelay(0);
//
//        PendingIntent pIntent = PendingIntent.getActivity(this, 0,
//        		new Intent(this, BaseMainActivity.class), Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//
//		Notification note = new NotificationCompat.Builder(getApplicationContext()).
//				setSmallIcon(R.drawable.dashboard).
//				setContentTitle("enviroCar").
//				setContentIntent(pIntent).
//				setContentText(getResources().getText(R.string.device_discovery_pending)).
//				build();
//
//		startForeground(BackgroundServiceImpl.BG_NOTIFICATION_ID, note);
//
//
//		return super.onStartCommand(intent, flags, startId);
//	}
//
//	protected void startWithDelay(long d) {
//		if (backgroundService != null && backgroundService.getServiceState() == ServiceState.SERVICE_STARTED) {
//			return;
//		}
//
//		discoveryEnabled = true;
//
//		discoveryRunnable = new Runnable() {
//			@Override
//			public void run() {
//				if (!discoveryEnabled) {
//					return;
//				}
//
//				logger.info("starting device discovery...");
//				Intent intent = new Intent(AbstractBackgroundServiceStateReceiver.SERVICE_STATE);
//				intent.putExtra(AbstractBackgroundServiceStateReceiver.SERVICE_STATE, ServiceState.SERVICE_DEVICE_DISCOVERY_RUNNING);
//				sendBroadcast(intent);
//
//				BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
//				if (adapter != null) {
//					if (adapter.isDiscovering()) {
//						adapter.cancelDiscovery();
//					}
//					adapter.startDiscovery();
//				}
//
//				try {
//					Thread.sleep(5000);
//				} catch (InterruptedException e) {
//					logger.warn(e.getMessage(), e);
//				}
//
//				if (!discoveryEnabled) {
//					/*
//					 * we are done, we found the device
//					 */
//					return;
//				}
//
//				/*
//				 * update the target time and send a broadcast
//				 */
//				intent = new Intent(AbstractBackgroundServiceStateReceiver.SERVICE_STATE);
//				intent.putExtra(AbstractBackgroundServiceStateReceiver.SERVICE_STATE, ServiceState.SERVICE_DEVICE_DISCOVERY_PENDING);
//				sendBroadcast(intent);
//
//				targetSystemTime = System.currentTimeMillis() + DISCOVERY_PERIOD;
//
//				/*
//				 * re-schedule ourselves
//				 */
//				invokeDiscoveryRunnable(DISCOVERY_PERIOD);
//			}
//
//		};
//
//		if (d > 0) {
//			Intent intent = new Intent(AbstractBackgroundServiceStateReceiver.SERVICE_STATE);
//			intent.putExtra(AbstractBackgroundServiceStateReceiver.SERVICE_STATE, ServiceState.SERVICE_DEVICE_DISCOVERY_PENDING);
//			sendBroadcast(intent);
//		}
//
//		targetSystemTime = System.currentTimeMillis() + d;
//
//		/*
//		 * do the actual invoking
//		 */
//		invokeDiscoveryRunnable(d);
//	}
//
//	private void invokeDiscoveryRunnable(long delay) {
//		discoveryHandler.postDelayed(discoveryRunnable, delay);
//	}
//
//	@Override
//	public IBinder onBind(Intent intent) {
//		logger.info("onBind " + getClass().getName() +"; Hash: "+System.identityHashCode(this));
//		return new LocalBinder();
//	}
//
//	private class LocalBinder extends Binder implements DeviceInRangeServiceInteractor {
//
//		@Override
//		public long getNextDiscoveryTargetTime() {
//			return targetSystemTime;
//		}
//
//		@Override
//		public boolean isDiscoveryPending() {
//			return discoveryEnabled;
//		}
//
//	}
//
//}
