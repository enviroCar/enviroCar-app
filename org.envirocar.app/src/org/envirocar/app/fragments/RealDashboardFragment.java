package org.envirocar.app.fragments;

import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.application.CarManager;
import org.envirocar.app.bluetooth.BluetoothHandler;
import org.envirocar.app.bluetooth.event.BluetoothServiceStateChangedEvent;
import org.envirocar.app.bluetooth.obd.events.Co2Event;
import org.envirocar.app.bluetooth.obd.events.SpeedUpdateEvent;
import org.envirocar.app.bluetooth.service.BluetoothServiceState;
import org.envirocar.app.events.GpsDOPEvent;
import org.envirocar.app.events.GpsSatelliteFix;
import org.envirocar.app.events.GpsSatelliteFixEvent;
import org.envirocar.app.events.LocationChangedEvent;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.view.preferences.PreferenceConstants;
import org.envirocar.app.views.LayeredImageRotateView;
import org.envirocar.app.views.TypefaceEC;

import java.text.DecimalFormat;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import rx.Scheduler;
import rx.Subscription;
import rx.android.content.ContentObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @author dewall
 */
public class RealDashboardFragment extends BaseInjectorFragment {
    private static final Logger LOGGER = Logger.getLogger(RealDashboardFragment.class);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    private static final String LOCATION = "location";
    private static final String SPEED = "speed";
    private static final String CO2 = "co2";


    @Inject
    protected Bus mBus;
    @Inject
    protected CarManager mCarManager;
    @Inject
    protected BluetoothHandler mBluetoothHandler;

    // Injected Views
    @InjectView(R.id.co2TextView)
    protected TextView mCo2TextView;
    @InjectView(R.id.textViewSpeedDashboard)
    protected TextView mSpeedTextView;
    @InjectView(R.id.co2meterView)
    protected LayeredImageRotateView mCo2RotableView;
    @InjectView(R.id.speedometerView)
    protected LayeredImageRotateView mSpeedRotatableView;

    @InjectView(R.id.gpsFixView)
    protected ImageView mGpsFixView;
    @InjectView(R.id.carOkView)
    protected ImageView mCarOkView;
    @InjectView(R.id.connectionStateImage)
    protected ImageView mConnectionStateImage;


    private Subscription mPreferenceSubscription;

    private GpsSatelliteFix mGpsFix = new GpsSatelliteFix(0, false);

    // TODO: change this
    private long mLastUIUpdate;
    private boolean mUseImperialUnits;
    private int mSpeed;
    private double mCo2;
    private Location mLocation;
    private BluetoothServiceState mServiceState;


    private Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        LOGGER.info("onCreateView()");

        // First inflate the general dashboard view.
        View contentView = inflater.inflate(R.layout.dashboard, container, false);

        // Inject all dashboard-related views.
        ButterKnife.inject(this, contentView);

        // return the inflated content view.
        return contentView;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        updateStatusElements();

        TypefaceEC.applyCustomFont((ViewGroup) view,
                TypefaceEC.Newscycle(getActivity()));

        // Subscribe for changes related to specific preference types.
        mPreferenceSubscription = ContentObservable.fromSharedPreferencesChanges(PreferenceManager
                .getDefaultSharedPreferences(getActivity()))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .filter(prefKey -> PreferenceConstants.CAR.equals(prefKey) ||
                        PreferenceConstants.CAR_HASH_CODE.equals(prefKey) ||
                        PreferenceConstants.PREFERENCE_TAG_BLUETOOTH_LIST.equals(prefKey))
                .subscribe(prefKey -> {
                    if (prefKey.equals(PreferenceConstants.PREFERENCE_TAG_BLUETOOTH_LIST)) {
                        updateStatusElements();
                    } else {
                        updateCarStatus();
                    }
                });

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        LOGGER.info("onResume()");
        super.onResume();

        updateGpsStatus();
        updateCarStatus();

        Car car = mCarManager.getCar();
        if (car != null && car.getFuelType() == Car.FuelType.DIESEL) {
            Crouton.makeText(getActivity(), R.string.diesel_not_yet_supported,
                    de.keyboardsurfer.android.widget.crouton.Style.ALERT).show();
        }

        mUseImperialUnits = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getBoolean(PreferenceConstants.IMPERIAL_UNIT, false);

        mLastUIUpdate = System.currentTimeMillis();

    }

    @Override
    public void onDestroy() {
        LOGGER.info("onDestroy()");
        super.onDestroy();

        if (mPreferenceSubscription != null) {
            mPreferenceSubscription.unsubscribe();
        }

        mMainThreadWorker.unsubscribe();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(LOCATION, mLocation);
        outState.putInt(SPEED, mSpeed);
        outState.putDouble(CO2, mCo2);
        super.onSaveInstanceState(outState);
    }

    @Subscribe
    public void onReceiveLocationChangedEvent(LocationChangedEvent event) {
        LOGGER.debug(String.format("Received event: %s", event.toString()));
        this.mLocation = event.mLocation;
        checkUIUpdate();
    }

    @Subscribe
    public void onReceiveGpsDOPEvent(GpsDOPEvent event) {
        LOGGER.debug(String.format("Received event: %s", event.toString()));

    }

    @Subscribe
    public void onReceiveSpeedEvent(SpeedUpdateEvent event) {
        LOGGER.debug(String.format("Received event: %s", event.toString()));
        this.mSpeed = event.mSpeed;
        checkUIUpdate();
    }

    @Subscribe
    public void onReceiveGpsSatelliteFixEvent(GpsSatelliteFixEvent event) {
        LOGGER.debug(String.format("Received event: %s", event.toString()));
        this.mGpsFix = event.mGpsSatelliteFix;
        if (this.mGpsFix == null || this.mGpsFix.isFix() != mGpsFix.isFix()) {
            updateGpsStatus();
        }
    }

    @Subscribe
    public void onReceiveCo2Event(Co2Event event) {
        LOGGER.debug(String.format("Received event: %s", event.toString()));
        this.mCo2 = event.mCo2;
        checkUIUpdate();
    }

    @Subscribe
    public void onReceiveBluetoothServiceStateChangedEvent(
            BluetoothServiceStateChangedEvent event) {
        LOGGER.debug(String.format("Received event: %s", event.toString()));
        mServiceState = event.mState;
        mMainThreadWorker.schedule(() -> updateStatusElements());
    }


    @OnClick(R.id.gpsFixView)
    protected void onGpsFixViewClicked() {

    }

    @OnClick(R.id.carOkView)
    protected void onCarOkViewClicked() {
        Car car = mCarManager.getCar();
        if (car != null) {
            Toast.makeText(getActivity(), car.toString(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), R.string.no_sensor_selected, Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.connectionStateImage)
    protected void onConnectionStateImageClicked() {
        String remoteDevice = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getString(org.envirocar.app.activity.SettingsActivity.BLUETOOTH_KEY,
                        null);

        if (remoteDevice == null) {
            Toast.makeText(getActivity(), R.string.no_device_selected, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStatusElements() {
        BluetoothDevice device = mBluetoothHandler.getSelectedBluetoothDevice();
        if (device == null) {
            mConnectionStateImage.setImageResource(R.drawable.bt_device_not_selected);
        } else if (mServiceState == BluetoothServiceState.SERVICE_STARTED) {
            mConnectionStateImage.setImageResource(R.drawable.bt_device_active);
        } else if (mServiceState == BluetoothServiceState.SERVICE_STARTING) {
            mConnectionStateImage.setImageResource(R.drawable.bt_device_pending);
        } else {
            mConnectionStateImage.setImageResource(R.drawable.bt_device_stopped);
            mCo2 = 0.0;
            mSpeed = 0;
            updateCo2Value();
            updateSpeedValue();
        }
    }

    /**
     *
     */
    private synchronized void checkUIUpdate() {
        if (mServiceState == BluetoothServiceState.SERVICE_STOPPED)
            return;

        if (getActivity() == null || System.currentTimeMillis() - mLastUIUpdate < 250) return;

        mLastUIUpdate = System.currentTimeMillis();

        if (mLocation != null || mSpeed != 0 || mCo2 != 0.0) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateSpeedValue();
                    updateCo2Value();
                }
            });
        }
    }

    /**
     * Updates the drawbale of the GpsFix ImageView depending on the current GPSFix.
     */
    private void updateGpsStatus() {
        if (mGpsFix.isFix()) {
            mGpsFixView.setImageResource(R.drawable.gps_fix);
        } else {
            mGpsFixView.setImageResource(R.drawable.gps_nofix);
        }
    }

    /**
     * Updates the drawable of the CarOk view depending on the thing whether a car has been
     * selected or not.
     */
    private void updateCarStatus() {
        // If a car has been selected, set car_ok image
        if (mCarManager.getCar() != null) {
            mCarOkView.setImageResource(R.drawable.car_ok);
        }
        // No car is setted. Set image accordingly.
        else {
            mCarOkView.setImageResource(R.drawable.car_no);
        }
    }

    /**
     *
     */
    private void updateCo2Value() {
        // Only when the fragment is currently showing the vies gets updated.
        if (!isVisible())
            return;

        mCo2TextView.setText(DECIMAL_FORMAT.format(mCo2) + " kg/h");
        mCo2RotableView.submitScaleValue((float) mCo2);

        if (mCo2 > 30) {
            getView().setBackgroundColor(Color.RED);
        } else {
            getView().setBackgroundColor(Color.WHITE);
        }
    }

    /**
     *
     */
    private void updateSpeedValue() {
        // Only when the fragment is currently showing the vies gets updated.
        if (!isVisible())
            return;

        if (!mUseImperialUnits) {
            mSpeedTextView.setText(mSpeed + " km/h");
            mSpeedRotatableView.submitScaleValue(mSpeed);
        } else {
            mSpeedTextView.setText(mSpeed / 1.6f + " mph");
            mSpeedRotatableView.submitScaleValue(mSpeed / 1.6f);
        }
    }
}
