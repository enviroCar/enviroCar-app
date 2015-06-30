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

package org.envirocar.app.application.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.envirocar.app.BaseMainActivity;
import org.envirocar.app.R;
import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.activity.TroubleshootingFragment;
import org.envirocar.app.application.CarManager;
import org.envirocar.app.application.CommandListener;
import org.envirocar.app.application.Listener;
import org.envirocar.app.application.LocationUpdateListener;
import org.envirocar.app.application.service.AbstractBackgroundServiceStateReceiver.ServiceState;
import org.envirocar.app.bluetooth.BluetoothConnection;
import org.envirocar.app.bluetooth.BluetoothSocketWrapper;
import org.envirocar.app.event.EventBus;
import org.envirocar.app.event.GpsSatelliteFixEventListener;
import org.envirocar.app.events.GpsSatelliteFix;
import org.envirocar.app.events.GpsSatelliteFixEvent;
import org.envirocar.app.injection.Injector;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.protocol.ConnectionListener;
import org.envirocar.app.protocol.OBDCommandLooper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Inject;

import static org.envirocar.app.application.service.AbstractBackgroundServiceStateReceiver
        .SERVICE_STATE;

/**
 * Service for connection to Bluetooth device and running commands. Imported
 * from Android OBD Reader project in some parts.
 *
 * @author jakob
 */
public class BackgroundServiceImpl extends Service implements BackgroundService {


    private static final Logger logger = Logger.getLogger(BackgroundServiceImpl.class);

    public static final String CONNECTION_PERMANENTLY_FAILED_INTENT =
            BackgroundServiceImpl.class.getName() + ".CONNECTION_PERMANENTLY_FAILED";

    protected static final long CONNECTION_CHECK_INTERVAL = 1000 * 5;

    protected static final int MAX_RECONNECT_COUNT = 2;

    public static final int BG_NOTIFICATION_ID = 42;

    private Listener commandListener;
    private final Binder binder = new LocalBinder();

    private OBDCommandLooper commandLooper;

    private BluetoothConnection bluetoothConnection;

    protected ServiceState state = ServiceState.SERVICE_STOPPED;

    protected int reconnectCount;

    private Handler toastHandler;

//	private TextToSpeech tts;

    public boolean ttsAvailable;

    private LocationUpdateListener locationListener;

    private GpsSatelliteFixEventListener gpsListener;

    protected GpsSatelliteFix fix = new GpsSatelliteFix(0, false);

    @Inject
    protected CarManager mCarManager;


    @Override
    public IBinder onBind(Intent intent) {
        logger.info("onBind " + getClass().getName() + "; Hash: " + System.identityHashCode(this));
        return binder;
    }

    @Override
    public void onCreate() {
        logger.info("onCreate " + getClass().getName() + "; Hash: " + System.identityHashCode
                (this));

        ((Injector) getApplicationContext()).injectObjects(this);
//		tts = new TextToSpeech(getApplicationContext(), new TextToSpeechListener());

        toastHandler = new Handler();
    }


    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        logger.info("onRebind " + getClass().getName() + "; Hash: " + System.identityHashCode
				(this));
    }

    @Override
    public boolean onUnbind(Intent intent) {
        logger.info("onUnbind " + getClass().getName() + "; Hash: " + System.identityHashCode
				(this));
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        logger.info("onDestroy " + getClass().getName() + "; Hash: " + System.identityHashCode
				(this));
        stopBackgroundService();
        stopForeground(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logger.info("onStartCommand " + getClass().getName() + "; Hash: " + System
				.identityHashCode(this));
        startBackgroundService();

        createForegroundNotification(R.string.service_state_starting);

        doTextToSpeech("Establishing connection");

        return START_STICKY;
    }

    private void doTextToSpeech(String string) {
        if (ttsAvailable) {
//			tts.speak("enviro car ".concat(string), TextToSpeech.QUEUE_ADD, null);
        }
    }

    private void createForegroundNotification(int stringResource) {
        CharSequence string = getResources().getText(stringResource);

        Intent intent = new Intent(this, BaseMainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, Intent
				.FLAG_ACTIVITY_REORDER_TO_FRONT);

        Notification note = new NotificationCompat.Builder(getApplicationContext()).
                setContentTitle("enviroCar").
                setContentText(string).
                setContentIntent(pIntent).
                setSmallIcon(R.drawable.dashboard).build();

        note.flags |= Notification.FLAG_NO_CLEAR;

        startForeground(BG_NOTIFICATION_ID, note);
    }

    /**
     * Starts the background service (bluetooth connction). Then calls methods
     * to start sending the obd commands for initialization.
     */
    private void startBackgroundService() {
        logger.info("startBackgroundService called");
        this.locationListener = new LocationUpdateListener((LocationManager) getSystemService
				(Context.LOCATION_SERVICE));
        this.locationListener.startLocating();

        gpsListener = new GpsSatelliteFixEventListener() {
            @Override
            public void receiveEvent(GpsSatelliteFixEvent event) {
                GpsSatelliteFix newFix = event.getPayload();

                if (fix.isFix() != newFix.isFix()) {
                    if (newFix.isFix()) {
                        doTextToSpeech("GPS positioning established");
                    } else {
                        doTextToSpeech("GPS positioning lost. Try to move the phone");
                    }
                }

                fix = newFix;
            }
        };

        EventBus.getInstance().registerListener(gpsListener);

        startConnection();
    }

    /**
     * Method that stops the service, removes everything from the waiting list
     */
    private void stopBackgroundService() {
        logger.info("stopBackgroundService called");
        new Thread(new Runnable() {

            @Override
            public void run() {
                shutdownConnectionAndHandler();

                setState(ServiceState.SERVICE_STOPPED);
                sendStateBroadcast();

                EventBus.getInstance().unregisterListener(gpsListener);

                locationListener.stopLocating();

                if (BackgroundServiceImpl.this.commandListener != null) {
                    BackgroundServiceImpl.this.commandListener.shutdown();
                }

                Notification noti = new NotificationCompat.Builder(getApplicationContext())
						.setContentTitle("enviroCar").
                        setContentText(getResources().getText(R.string.service_state_stopped)).
                        setSmallIcon(R.drawable.dashboard).setAutoCancel(true).build();

                NotificationManager manager = (NotificationManager) getSystemService(Context
						.NOTIFICATION_SERVICE);
                manager.notify(BG_NOTIFICATION_ID, noti);

                doTextToSpeech("Device disconnected");
            }

        }).start();
    }

    private void shutdownConnectionAndHandler() {
        if (BackgroundServiceImpl.this.commandLooper != null) {
            BackgroundServiceImpl.this.commandLooper.stopLooper();
        }

        if (BackgroundServiceImpl.this.bluetoothConnection != null) {
            BackgroundServiceImpl.this.bluetoothConnection.cancelConnection();
        }
    }

    private void sendStateBroadcast() {
        Intent intent = new Intent(SERVICE_STATE);
        intent.putExtra(SERVICE_STATE, state);
        sendBroadcast(intent);
    }

    /**
     * Start and configure the connection to the OBD interface.
     *
     * @throws IOException
     */
    private void startConnection() {
        logger.info("startConnection called");
        // Connect to bluetooth device
        // Init bluetooth

        startBluetoothConnection();

        setState(ServiceState.SERVICE_STARTING);
        sendStateBroadcast();
    }

    private void startBluetoothConnection() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        String remoteDevice = preferences.getString(SettingsActivity.BLUETOOTH_KEY, null);
        // Stop if device is not available

        if (remoteDevice == null || "".equals(remoteDevice)) {
            return;
        }

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter
                .getDefaultAdapter();
        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(remoteDevice);

        bluetoothConnection = new BluetoothConnection(bluetoothDevice, true, this,
				getApplicationContext());
    }


    public void deviceDisconnected() {
        logger.info("Bluetooth device disconnected.");
        stopBackgroundService();
    }

    /**
     * method gets called when the bluetooth device connection
     * has been established.
     */
    public void deviceConnected(BluetoothSocketWrapper bluetoothSocket) {
        logger.info("Bluetooth device connected.");

        InputStream in;
        OutputStream out;
        try {
            in = bluetoothSocket.getInputStream();
            out = bluetoothSocket.getOutputStream();
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
            deviceDisconnected();
            return;
        }

        initializeCommandLooper(in, out, bluetoothSocket.getRemoteDeviceName());

        createForegroundNotification(R.string.service_state_started);

        doTextToSpeech("Connection established");
    }

    protected void initializeCommandLooper(InputStream in, OutputStream out, String deviceName) {
        if (this.commandListener != null) {
            this.commandListener.shutdown();
        }

        this.commandListener = new CommandListener(getApplicationContext());
        this.commandLooper = new OBDCommandLooper(
                in, out, deviceName,
                this.commandListener, new ConnectionListener() {
            @Override
            public void onConnectionVerified() {
                setState(ServiceState.SERVICE_STARTED);
                BackgroundServiceImpl.this.sendStateBroadcast();
                reconnectCount = 0;
            }

//					@Override
//					public void onConnectionException(IOException e) {
//						logger.warn("onConnectionException", e);
//						BackgroundServiceImpl.this.deviceDisconnected();
//					}

            @Override
            public void onAllAdaptersFailed() {
                BackgroundServiceImpl.this.onAllAdaptersFailed();
            }

            @Override
            public void onStatusUpdate(String message) {
                displayToast(message);
            }

            @Override
            public void requestConnectionRetry(IOException reason) {
                if (reconnectCount++ >= MAX_RECONNECT_COUNT) {
                    logger.warn("max reconnect count reached", reason);
                    BackgroundServiceImpl.this.deviceDisconnected();
                } else {
                    logger.info("Restarting Device Connection...");
                    doTextToSpeech("Connection lost. Trying to reconnect.");
                    shutdownConnectionAndHandler();
                    startConnection();
                }
            }
        });
        this.commandLooper.start();
    }

    protected void setState(ServiceState serviceStarted) {
        this.state = serviceStarted;
        logger.info(this.state.toString());
    }

    private void displayToast(final String s) {
//        Observable.just(s)
//                .observeOn(AndroidSchedulers.mainThread())
        toastHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void onAllAdaptersFailed() {
        logger.info("all adapters failed!");
        stopBackgroundService();
        doTextToSpeech("failed to connect to the OBD adapter");
        sendBroadcast(new Intent(CONNECTION_PERMANENTLY_FAILED_INTENT));
    }

    public void openTroubleshootingFragment(int type) {
        Intent intent = new Intent(TroubleshootingFragment.INTENT);
        sendBroadcast(intent);
    }

    /**
     * Binder imported directly from Android OBD Project. Runs the waiting list
     * when jobs are added to it
     *
     * @author jakob
     */
    private class LocalBinder extends Binder implements BackgroundServiceInteractor {

        @Override
        public ServiceState getServiceState() {
            return BackgroundServiceImpl.this.state;
        }

    }

    private class TextToSpeechListener implements OnInitListener {

        @Override
        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS) {
                ttsAvailable = true;
//				tts.setLanguage(Locale.ENGLISH);
            } else {
                logger.warn("TextToSpeech is not available.");
            }
        }

    }

}