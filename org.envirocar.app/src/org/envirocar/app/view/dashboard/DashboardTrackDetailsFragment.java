package org.envirocar.app.view.dashboard;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.bluetooth.service.BluetoothServiceState;
import org.envirocar.app.events.GpsSatelliteFixEvent;
import org.envirocar.app.events.bluetooth.BluetoothServiceStateChangedEvent;
import org.envirocar.app.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.services.trackdetails.AvrgSpeedUpdateEvent;
import org.envirocar.app.services.trackdetails.DistanceValueUpdateEvent;
import org.envirocar.app.services.trackdetails.StartingTimeEvent;
import org.envirocar.app.storage.Measurement;

import java.text.DecimalFormat;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;

/**
 * @author dewall
 */
public class DashboardTrackDetailsFragment extends BaseInjectorFragment {
    private static final Logger LOGGER = Logger.getLogger(DashboardTrackDetailsFragment.class);

    private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("###.#");


    @InjectView(R.id.fragment_dashboard_header_gps_image)
    protected ImageView mGpsImage;
    @InjectView(R.id.fragment_dashboard_header_gps_text)
    protected TextView mGpsText;

    @InjectView(R.id.fragment_dashboard_header_bt_image)
    protected ImageView mBluetoothImage;
    @InjectView(R.id.fragment_dashboard_header_bt_text)
    protected TextView mBluetoothText;

    @InjectView(R.id.fragment_dashboard_header_time_timer)
    protected Chronometer mTimerText;
    @InjectView(R.id.fragment_dashboard_header_speed_text)
    protected TextView mSpeedText;
    @InjectView(R.id.fragment_dashboard_header_distance_text)
    protected TextView mDistanceText;


    private Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        LOGGER.info("onCreateView()");

        // First inflate the general dashboard view.
        View contentView = inflater.inflate(R.layout.fragment_dashboard_header, container, false);

        // Inject all dashboard-related views.
        ButterKnife.inject(this, contentView);

        // Update the image and text of the bluetooth related views.
        updateBluetoothViews(true);

        // return the inflated content view.
        return contentView;
    }

    @Subscribe
    public void onReceiveBluetoothStateChangedEvent(BluetoothStateChangedEvent event) {
        LOGGER.info(String.format("Received event: %s", event.toString()));
        mMainThreadWorker.schedule(() -> updateBluetoothViews(event.isBluetoothEnabled));
    }

    @Subscribe
    public void onReceiveBluetoothServiceStateChangedEvent(
            BluetoothServiceStateChangedEvent event) {
        LOGGER.info(String.format("Received event: %s", event.toString()));
        mMainThreadWorker.schedule(() -> {
            if (event.mState == BluetoothServiceState.SERVICE_STOPPED) {
                mTimerText.setBase(SystemClock.elapsedRealtime());
                mTimerText.stop();
                mDistanceText.setText("0.0 km");
                mSpeedText.setText("0 km/h");
            }
        });
    }

    @Subscribe
    public void onReceiveAvrgSpeedUpdateEvent(AvrgSpeedUpdateEvent event) {
        mMainThreadWorker.schedule(() -> mSpeedText.setText(String.format("%s km/h",
                Integer.toString(event.mAvrgSpeed))));
    }

    @Subscribe
    public void onReceiveDistanceUpdateEvent(DistanceValueUpdateEvent event) {
        mMainThreadWorker.schedule(() ->
                mDistanceText.setText(String.format("%s km",
                        DECIMAL_FORMATTER.format(event.mDistanceValue))));
    }

    @Subscribe
    public void onReceiveGpsSatelliteFixEvent(GpsSatelliteFixEvent event) {
        LOGGER.info(String.format("Received event: %s", event.toString()));
        updateLocationViews(event.mGpsSatelliteFix.isFix());
    }

    @Subscribe
    public void onReceiveStartingTimeEvent(StartingTimeEvent event) {
        mMainThreadWorker.schedule(() -> {
            mTimerText.setBase(event.mStartingTime);
            if(event.mIsStarted)
                mTimerText.start();
            else
                mTimerText.stop();
        });
    }


    /**
     * @param isConnected
     */
    private void updateBluetoothViews(boolean isConnected) {
        if (isConnected) {
            mBluetoothImage.setImageResource(R.drawable.ic_bluetooth_white_24dp);
            mBluetoothText.setText("Connected");
        } else {
            mBluetoothImage.setImageResource(R.drawable.ic_bluetooth_white_24dp);
            mBluetoothText.setText("Disconnected");
        }
    }

    private void updateLocationViews(boolean isFix) {
        if (isFix) {
            mGpsImage.setImageResource(R.drawable.ic_location_on_white_24dp);
        } else {
            mGpsImage.setImageResource(R.drawable.ic_location_off_white_24dp);
        }
    }

}


