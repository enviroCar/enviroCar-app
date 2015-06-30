package org.envirocar.app.services;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.app.LocationHandler;
import org.envirocar.app.application.CommandListener;
import org.envirocar.app.bluetooth.BluetoothHandler;
import org.envirocar.app.bluetooth.BluetoothSocketWrapper;
import org.envirocar.app.bluetooth.FallbackBluetoothSocket;
import org.envirocar.app.bluetooth.NativeBluetoothSocket;
import org.envirocar.app.events.GpsSatelliteFix;
import org.envirocar.app.events.GpsSatelliteFixEvent;
import org.envirocar.app.injection.Injector;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.protocol.ConnectionListener;
import org.envirocar.app.protocol.OBDCommandLooper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.content.ContentObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * @author dewall
 */
public class OBDConnectionService extends Service {
    private static final String TAG = OBDConnectionService.class.getSimpleName();
    private static final Logger LOGGER = Logger.getLogger(OBDConnectionService.class);

    protected static final int MAX_RECONNECT_COUNT = 2;

    private static final UUID EMBEDDED_BOARD_SPP = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Inject
    protected Bus mBus;
    @Inject
    protected BluetoothHandler mBluetoothHandler;
    @Inject
    protected LocationHandler mLocationHandler;

    private CommandListener mCommandListener;
    private OBDCommandLooper mOBDCommandLooper;

    private OBDBluetoothConnection mOBDConnection;
    private CompositeSubscription mCompositeSubscription = new CompositeSubscription();

    private Subscription mUUIDSubscription;


    // This satellite fix indicates that there is no satellite connection yet.
    private GpsSatelliteFix mCurrentGpsSatelliteFix = new GpsSatelliteFix(0, false);

    @Override
    public void onCreate() {
        super.onCreate();

        // Inject ourselves.
        ((Injector) getApplicationContext()).injectObjects(this);

        // register on the event bus
        this.mBus.register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start the location
        mLocationHandler.startLocating();

        //
        doTextToSpeech("Establishing connection");

        // Get the default device
        BluetoothDevice device = mBluetoothHandler.getSelectedBluetoothDevice();

        if (device != null) {

            Log.i("yea", "start OBD connection");
            //
            startOBDConnection(device);

        } else {
            LOGGER.severe("No default Bluetooth device selected");
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        LOGGER.info("onDestroy()");
        super.onDestroy();

        if (mCompositeSubscription != null)
            mCompositeSubscription.unsubscribe();

        if (mUUIDSubscription != null)
            mUUIDSubscription.unsubscribe();
    }

    @Subscribe
    public void onReceiveGpsSatelliteFixEvent(GpsSatelliteFixEvent event) {
        boolean isFix = mCurrentGpsSatelliteFix.isFix();
        if (isFix != mCurrentGpsSatelliteFix.isFix()) {
            if (isFix) {

            } else {

            }
            this.mCurrentGpsSatelliteFix = event.mGpsSatelliteFix;
        }
    }

    private void doTextToSpeech(String string) {
//        if (ttsAvailable) {
//			tts.speak("enviro car ".concat(string), TextToSpeech.QUEUE_ADD, null);
//        }
    }

    /**
     * @param uuids
     * @return
     */
    private Observable<UUID> transformUUID(final Parcelable uuids[]) {
        return Observable.create(new Observable.OnSubscribe<UUID>() {
            @Override
            public void call(Subscriber<? super UUID> subscriber) {
                // Create a uuid for every string and return it
                for (Parcelable uuid : uuids) {
                    subscriber.onNext(UUID.fromString(uuid.toString()));
                }
                subscriber.onCompleted();
            }
        });
    }

    /**
     * @param device
     * @return
     */
    private Observable<Object> getUUIDs(final BluetoothDevice device) {
        return ContentObservable.fromBroadcast(getApplicationContext(),
                new IntentFilter(BluetoothDevice.ACTION_UUID))
                .flatMap(intent -> {
                            // Get the device and the UUID provided by the incoming intent.
                            BluetoothDevice deviceExtra = intent
                                    .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            Parcelable[] uuidExtra = intent
                                    .getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);

                            // If the received broadcast does not belong to this receiver,
                            // skip it.
                            if (!deviceExtra.getAddress().equals(device.getAddress()))
                                return null;

                            return transformUUID(uuidExtra);
                        }
                );
    }

    /**
     * @param device
     * @return
     */
    private Observable<List<UUID>> getUUIDList(final BluetoothDevice device) {
        Log.i(TAG, "getUUIDList");
        return ContentObservable.fromBroadcast(getApplicationContext(),
                new IntentFilter(BluetoothDevice.ACTION_UUID))
                .map(new Func1<Intent, List<UUID>>() {
                    @Override
                    public List<UUID> call(Intent intent) {
                        Log.i(TAG, "getUUIDList(): map call");
                        // Get the device and the UUID provided by the incoming intent.
                        BluetoothDevice deviceExtra = intent
                                .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        Parcelable[] uuidExtra = intent
                                .getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);

                        // If the received broadcast does not belong to this receiver,
                        // skip it.
                        if (!deviceExtra.getAddress().equals(device.getAddress()))
                            return null;

                        // Result list to return
                        List<UUID> res = new ArrayList<UUID>();

                        LOGGER.info(String.format("Adding default UUID: %s", EMBEDDED_BOARD_SPP));
                        res.add(EMBEDDED_BOARD_SPP);

                        // Create a uuid for every string and return it
                        for (Parcelable uuid : uuidExtra) {
                            UUID next = UUID.fromString(uuid.toString());
                            if (!res.contains(next)) {
                                res.add(next);
                            }
                        }

                        // return the result list
                        return res;
                    }
                });
    }


    /**
     * @param device the device to start a connection to.
     */
    private void startOBDConnection(final BluetoothDevice device) {
        Log.i(TAG, "startOBDConnection");
        if (device.fetchUuidsWithSdp())
            mUUIDSubscription = getUUIDList(device)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(uuids -> {
                        Log.i("yea", "start bluetooth connection thread");
                        (mOBDConnection = new OBDBluetoothConnection(device, uuids)).start();
                        mUUIDSubscription.unsubscribe();
                    });
    }

    private void onDeviceConnected(BluetoothSocketWrapper bluetoothSocket){
        try {
            InputStream in = bluetoothSocket.getInputStream();
            OutputStream out = bluetoothSocket.getOutputStream();

            if(mCommandListener != null){
                mCommandListener.shutdown();
            }

            this.mCommandListener = new CommandListener(getApplicationContext());
            this.mOBDCommandLooper = new OBDCommandLooper(in, out, bluetoothSocket.getRemoteDeviceName(), this.mCommandListener, new ConnectionListener() {

                private int mReconnectCount = 0;

                @Override
                public void onConnectionVerified() {

                }

                @Override
                public void onAllAdaptersFailed() {

                }

                @Override
                public void onStatusUpdate(String message) {

                }

                @Override
                public void requestConnectionRetry(IOException e) {
                    if(mReconnectCount++ >= MAX_RECONNECT_COUNT){
                        LOGGER.warn("Max count of reconnecctes reaced", e);
                    } else {

                    }
                }
            });
            this.mOBDCommandLooper.start();
        } catch (IOException e) {
            LOGGER.warn(e.getMessage(), e);
            //deviceDisconnected();
            return;
        }
    }

    /**
     *
     */
    class OBDBluetoothConnection extends Thread {

        // The required input variables.
        private final BluetoothDevice mDevice;
        private final List<UUID> mUUIDCandidates;

        // Boolean variables indicating the state.
        private boolean mSuccess;
        private boolean mIsRunning;

        // The socket wrapper for the connection.
        private BluetoothSocketWrapper mSocketWrapper;

        /**
         * Constructor
         *
         * @param device
         * @param uuids
         */
        public OBDBluetoothConnection(BluetoothDevice device, List<UUID> uuids) {
            this.mDevice = device;
            this.mUUIDCandidates = uuids;
            this.mIsRunning = true;
        }

        @Override
        public void run() {
            for (UUID uuid : mUUIDCandidates) {
                if (!mIsRunning)
                    return;

                try {
                    Log.i(TAG, "Trying to create native bleutooth socket");
                    mSocketWrapper = new NativeBluetoothSocket(mDevice
                            .createRfcommSocketToServiceRecord(uuid));
                } catch (IOException e) {
                    Log.i(TAG, "Error");

                    LOGGER.warn(e.getMessage(), e);
                    continue;
                }

                if (mSocketWrapper == null)
                    continue;

                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    mSocketWrapper.connect();
                    mSuccess = true;
                } catch (IOException e) {
                    LOGGER.warn("Exception on bluetooth connection. Trying " +
                            "the fallback... : "
                            + e.getMessage(), e);
                    try {
                        //try the fallback
                        if (mIsRunning) {

                            mSocketWrapper = new FallbackBluetoothSocket(mSocketWrapper
                                    .getUnderlyingSocket());
                            Thread.sleep(500);
                            mSocketWrapper.connect();
                            mSuccess = true;
                        }
                    } catch (FallbackBluetoothSocket.FallbackException e1) {
                        e1.printStackTrace();
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        shutdownSocket(mSocketWrapper);
                    }
                }

                if (mSuccess) {
                    Log.i(TAG, "successful connected");
                    onDeviceConnected(mSocketWrapper);
                    break;
                }
            }
        }

        private void shutdownSocket(BluetoothSocketWrapper socket) {
            OBDConnectionService.LOGGER.info("Shutting down bluetooth socket.");

            try {
                if (socket.getInputStream() != null)
                    socket.getInputStream().close();

            } catch (Exception e) {
            }

            try {
                if (socket.getOutputStream() != null)
                    socket.getOutputStream().close();

            } catch (Exception e) {
            }

            try {
                socket.close();
            } catch (Exception e) {
            }
        }

        protected void cancelConnection() {
            mIsRunning = false;
            shutdownSocket(mSocketWrapper);
        }
    }


    /**
     * Class used for the client Binder. The service is running in the same process as its
     * client, so it is not required to deal with IPC.
     */
    public class OBDConnectionBinder extends Binder {

        /**
         * Returns the instance of the enclosing service.
         *
         * @return the enclosing service.
         */
        OBDConnectionService getService() {
            return OBDConnectionService.this;
        }
    }
}
