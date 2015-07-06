package org.envirocar.app;

import android.content.Context;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.app.bluetooth.BluetoothHandler;
import org.envirocar.app.bluetooth.event.BluetoothServiceStateChangedEvent;
import org.envirocar.app.bluetooth.service.BluetoothServiceState;
import org.envirocar.app.injection.InjectApplicationScope;
import org.envirocar.app.injection.Injector;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.Track;

import javax.inject.Inject;

import rx.Scheduler;
import rx.schedulers.Schedulers;

/**
 * @author de Wall
 */
public class TrackHandler {
    private static final Logger LOGGER = Logger.getLogger(TrackHandler.class);
    private static final String TRACK_MODE = "trackMode";

    @Inject
    @InjectApplicationScope
    protected Context mContext;
    @Inject
    protected Bus mBus;
    @Inject
    protected DbAdapter mDBAdapter;
    @Inject
    protected BluetoothHandler mBluetoothHandler;

    private Scheduler.Worker mBackgroundWorker = Schedulers.io().createWorker();

    private BluetoothServiceState mBluetoothServiceState = BluetoothServiceState.SERVICE_STOPPED;


    /**
     * Constructor.
     *
     * @param context the context of the activity's scope.
     */
    public TrackHandler(Context context) {
        // Inject all annotated fields.
        ((Injector) context).injectObjects(this);
    }


    public void startNewTrack() {


    }

    /**
     * Finishes the current track. On the one hand, the service that handles the connection to
     * the Bluetooth device gets closed and the track in the database gets finished.
     */
    public void finishCurrentTrack() {
        LOGGER.info("stopTrack()");

        // Set the current service state to SERVICE_STOPPING.
        mBus.post(new BluetoothServiceStateChangedEvent(BluetoothServiceState.SERVICE_STOPPING));

        // Schedule a new async task for closing the service, finishing the current track, and
        // finally fireing an event on the event bus.
        mBackgroundWorker.schedule(() -> {
            LOGGER.info("backgroundworker");
            // Stop the background service that is responsible for the OBDConnection.
            mBluetoothHandler.stopOBDConnectionService();

            // Finish the current track.
            final Track track = mDBAdapter.finishCurrentTrack();

            // Fire a new TrackFinishedEvent on the event bus.
            mBus.post(new TrackFinishedEvent(track));
        });
    }

    @Subscribe
    public void onReceiveBluetoothServiceStateChangedEvent(
            BluetoothServiceStateChangedEvent event) {
        LOGGER.info(String.format("onReceiveBluetoothServiceStateChangedEvent: %s",
                event.toString()));
        mBluetoothServiceState = event.mState;
    }
}
