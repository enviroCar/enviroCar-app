package org.envirocar.app.services.autoconnect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.OnLifecycleEvent;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.core.events.NewCarTypeSelectedEvent;
import org.envirocar.core.events.bluetooth.BluetoothDeviceSelectedEvent;
import org.envirocar.core.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.core.events.gps.GpsStateChangedEvent;
import org.envirocar.core.logging.Logger;
import org.envirocar.obd.bluetooth.BluetoothSocketWrapper;
import org.envirocar.obd.bluetooth.FallbackBluetoothSocket;
import org.envirocar.obd.bluetooth.NativeBluetoothSocket;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * @author dewall
 */
public class OBDAutoRecordingStrategy implements AutoRecordingStrategy {
    private static final Logger LOG = Logger.getLogger(OBDAutoRecordingStrategy.class);
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final AutoRecordingService service;
    private final Bus eventBus;
    private final BluetoothHandler bluetoothHandler;

    // Preconditions
    private boolean isCarSelected = false;
    private boolean isGPSEnabled = false;
    private boolean isBTEnabled = false;
    private boolean isBTSelected = false;

    // settings
    private int discoveryInterval = PreferencesHandler.DEFAULT_BLUETOOTH_DISCOVERY_INTERVAL;

    // preferences
    private Disposable detectionDisposable;
    private CompositeDisposable disposables = new CompositeDisposable();
    private AutoRecordingCallback callback;

    /**
     * Constructor
     *
     * @param service
     * @param eventBus
     * @param bluetoothHandler
     */
    public OBDAutoRecordingStrategy(AutoRecordingService service, Bus eventBus, BluetoothHandler bluetoothHandler) {
        this.service = service;
        this.eventBus = eventBus;
        this.bluetoothHandler = bluetoothHandler;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        this.stop();
    }

    @Override
    public boolean preconditionsFulfilled() {
        return isCarSelected && isGPSEnabled && isBTEnabled && isBTSelected;
    }

    @Override
    public void run(AutoRecordingCallback callback) {
        this.callback = callback;

        disposables.add(PreferencesHandler.getDiscoveryIntervalObservable(service)
                .doOnNext(interval -> {
                    this.discoveryInterval = interval;
                    updateDetectionObservable();
                })
                .doOnError(LOG::error)
                .subscribe());

        try {
            eventBus.register(this);
        } catch (Exception e) {
        }

        this.updateDetectionObservable();
    }

    @Override
    public void stop() {
        if (this.detectionDisposable != null) {
            this.detectionDisposable.dispose();
            this.detectionDisposable = null;
        }

        if (disposables != null) {
            disposables.clear();
        }

        try {
            eventBus.unregister(this);
        } catch (Exception e) {
        }
    }

    @Subscribe
    public void onCarSelectedEvent(NewCarTypeSelectedEvent event) {
        LOG.info("Received event. %s", event.toString());
        this.isCarSelected = event.mCar != null;
        checkPrecodinitions();
    }

    @Subscribe
    public void onReceiveBluetoothStateChangedEvent(BluetoothStateChangedEvent event) {
        LOG.info("Received event. %s", event.toString());
        this.isBTEnabled = event.isBluetoothEnabled;
        checkPrecodinitions();
    }

    @Subscribe
    public void onReceiveGpsStatusChangedEvent(GpsStateChangedEvent event) {
        LOG.info("Received event. %s", event.toString());
        this.isGPSEnabled = event.mIsGPSEnabled;
        checkPrecodinitions();
    }

    @Subscribe
    public void onReceiveBluetoothDeviceSelectedEvent(BluetoothDeviceSelectedEvent event) {
        LOG.info("Received event. %s", event.toString());
        this.isBTSelected = event.mDevice != null;
        checkPrecodinitions();
    }

    private void checkPrecodinitions() {
        if (!isGPSEnabled) {
            callback.onPreconditionUpdate(PreconditionType.GPS_DISABLED);
        } else if (!isBTEnabled) {
            callback.onPreconditionUpdate(PreconditionType.BT_DISABLED);
        } else if (!isCarSelected) {
            callback.onPreconditionUpdate(PreconditionType.CAR_NOT_SELECTED);
        } else if (!isBTSelected) {
            callback.onPreconditionUpdate(PreconditionType.BT_DISABLED);
        } else {
            callback.onPreconditionUpdate(PreconditionType.SATISFIED);
        }
    }

    private void updateDetectionObservable() {
        if (this.detectionDisposable != null) {
            detectionDisposable.dispose();
            detectionDisposable = null;
        }

        this.detectionDisposable = Observable.defer(() ->
                Observable.timer(discoveryInterval, TimeUnit.SECONDS))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .map((Function<Long, Object>) aLong -> {
                    LOG.info("trying to connect");
                    if (!preconditionsFulfilled()) {
                        throw new RuntimeException("Preconditions are not satisfied");
                    }
                    return aLong;
                })
                .map(aLong -> bluetoothHandler.getSelectedBluetoothDevice())
                .map(this::tryDirectConnection)
                .retryWhen(throwableObservable -> throwableObservable.flatMap(error -> Observable.timer(discoveryInterval, TimeUnit.SECONDS)))
                .doOnNext(aBoolean -> {
                    if (aBoolean) {
                        callback.onRecordingTypeConditionsMet();
                    }
                })
                .doOnError(LOG::error)
                .subscribe();
    }

    // Alternative approach compared to discovery. OBDLink unfortunately does not support to be discovered by default
    private Boolean tryDirectConnection(BluetoothDevice bluetoothDevice) throws Exception {
        LOG.info("Trying to detect whether bluetooth device %s is close", bluetoothDevice.getName());
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            LOG.error("Bluetooth Adapter not found or is not enabled!");
            throw new Exception("Bluetooth adapter not found or is not enabled!");
        }

        BluetoothSocketWrapper socket = null;
        try {
            socket = new NativeBluetoothSocket(bluetoothDevice.createRfcommSocketToServiceRecord(uuid));
            adapter.cancelDiscovery();
            try {
                socket.connect();
                LOG.info("Successful connected to device");
                return true;
            } catch (Exception e) {
                LOG.info("Trying fallback socket");
                socket = new FallbackBluetoothSocket(socket.getUnderlyingSocket());
                socket.connect();
                LOG.info("Successful connected to device with SocketWrapper");
                return true;
            }
        } catch (Exception e) {
            LOG.info("Unable to connect to bluetooth device.");
            throw Exceptions.propagate(new RuntimeException("Connection could not be established."));
        } finally {
            Thread.sleep(500);
            closeSocket(socket);
        }
    }

    private void closeSocket(BluetoothSocketWrapper socket) throws IOException {
        if (socket.getInputStream() != null) {
            socket.getInputStream().close();
        }
        if (socket.getOutputStream() != null) {
            socket.getOutputStream().close();
        }
        socket.close();
    }
}
