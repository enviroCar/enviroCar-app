package org.envirocar.app.view.dashboard;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.view.carselection.CarSelectionActivity;
import org.envirocar.app.view.obdselection.OBDSelectionActivity;
import org.envirocar.core.entity.Car;
import org.envirocar.core.events.NewCarTypeSelectedEvent;
import org.envirocar.core.events.bluetooth.BluetoothDeviceSelectedEvent;
import org.envirocar.core.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.core.injection.BaseInjectorFragment;
import org.envirocar.core.logging.Logger;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * @author dewall
 */
public class DashboardTrackSettingsFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(DashboardTrackSettingsFragment.class);

    @Inject
    protected CarPreferenceHandler mCarPrefHandler;
    @Inject
    protected BluetoothHandler mBluetoothHandler;

    @InjectView(R.id.fragment_startup_obd_selection)
    protected View mOBDTypeView;
    @InjectView(R.id.fragment_startup_obd_selection_text1)
    protected TextView mOBDTypeTextView;
    @InjectView(R.id.fragment_startup_obd_selection_text2)
    protected TextView mOBDTypeSubTextView;

    @InjectView(R.id.fragment_startup_car_selection)
    protected View mCarTypeView;
    @InjectView(R.id.fragment_startup_car_selection_text1)
    protected TextView mCarTypeTextView;
    @InjectView(R.id.fragment_startup_car_selection_text2)
    protected TextView mCarTypeSubTextView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        LOG.info("onCreateView()");

        // First inflate the general dashboard view.
        View contentView = inflater.inflate(R.layout.fragment_startup_settings_layout,
                container, false);

        // Inject all dashboard-related views.
        ButterKnife.inject(this, contentView);

        mCarTypeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CarSelectionActivity.class);
                getActivity().startActivity(intent);
            }
        });
        mOBDTypeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), OBDSelectionActivity.class);
                getActivity().startActivity(intent);
            }
        });

        setCarTypeText(mCarPrefHandler.getCar());
        setOBDTypeText(mBluetoothHandler.getSelectedBluetoothDevice());
//        contentView.setVisibility(View.GONE);

        return contentView;
    }

    @Override
    public void onResume() {
        super.onResume();
//        animateShowView();
    }

    @Override
    public void onStop() {
        super.onStop();
//        animateHideView();
    }

    private void animateShowView(){
        getView().setVisibility(View.VISIBLE);
        getView().startAnimation(
                AnimationUtils.loadAnimation(
                        getActivity(), R.anim.anim_up));
    }

    /**
     *
     */
    private void animateHideView(){
        getView().startAnimation(
                AnimationUtils.loadAnimation(
                        getActivity(), R.anim.anim_up));
        getView().setVisibility(View.GONE);
    }

    @Subscribe
    public void onReceiveNewCarTypeSelectedEvent(NewCarTypeSelectedEvent event) {
        LOG.debug(String.format("Received event: %s", event.toString()));
        setCarTypeText(event.mCar);
    }

    @Subscribe
    public void onReceiveBluetoothDeviceSelectedEvent(BluetoothDeviceSelectedEvent event) {
        LOG.debug(String.format("Received event: %s", event.toString()));
        setOBDTypeText(event.mDevice);
    }

    @Subscribe
    public void onReceiveBluetoothStateChangedEvent(BluetoothStateChangedEvent event) {
        LOG.debug(String.format("Received event: %s", event.toString()));
        setOBDTypeText(mBluetoothHandler.getSelectedBluetoothDevice());
    }

    /**
     * @param device
     */
    private void setOBDTypeText(BluetoothDevice device) {
        getActivity().runOnUiThread(() -> {
            if (!mBluetoothHandler.isBluetoothEnabled()) {
                mOBDTypeTextView.setText("Bluetooth is disabled.");
                mOBDTypeSubTextView.setText("Please enable bluetooth.");
                mOBDTypeSubTextView.setVisibility(View.VISIBLE);
            } else if (device == null) {
                mOBDTypeTextView.setText("No OBD device selected.");
                mOBDTypeSubTextView.setVisibility(View.GONE);
            } else {
                mOBDTypeTextView.setText(device.getName());
                mOBDTypeSubTextView.setText(device.getAddress());
                mOBDTypeSubTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * @param car
     */
    private void setCarTypeText(Car car) {
        if (car != null) {
            mCarTypeTextView.setText(String.format("%s - %s",
                    car.getManufacturer(),
                    car.getModel()));

            mCarTypeSubTextView.setText(String.format("%s    %s    %s ccm",
                    car.getConstructionYear(),
                    car.getFuelType(),
                    car.getEngineDisplacement()));

            mCarTypeSubTextView.setVisibility(View.VISIBLE);
        } else {
            mCarTypeTextView.setText(String.format("No Car selected"));
            mCarTypeSubTextView.setVisibility(View.GONE);
        }
    }
}
