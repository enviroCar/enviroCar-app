/**
 * Copyright (C) 2013 - 2015 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
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
import butterknife.BindView;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class DashboardTrackSettingsFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(DashboardTrackSettingsFragment.class);

    @Inject
    protected CarPreferenceHandler mCarPrefHandler;
    @Inject
    protected BluetoothHandler mBluetoothHandler;

    @BindView(R.id.fragment_startup_obd_selection)
    protected View mOBDTypeView;
    @BindView(R.id.fragment_startup_obd_selection_text1)
    protected TextView mOBDTypeTextView;
    @BindView(R.id.fragment_startup_obd_selection_text2)
    protected TextView mOBDTypeSubTextView;

    @BindView(R.id.fragment_startup_car_selection)
    protected View mCarTypeView;
    @BindView(R.id.fragment_startup_car_selection_text1)
    protected TextView mCarTypeTextView;
    @BindView(R.id.fragment_startup_car_selection_text2)
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
        ButterKnife.bind(this, contentView);

        mCarTypeView.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CarSelectionActivity.class);
            getActivity().startActivity(intent);
        });
        mOBDTypeView.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), OBDSelectionActivity.class);
            getActivity().startActivity(intent);
        });

        setCarTypeText(mCarPrefHandler.getCar());
        setOBDTypeText(mBluetoothHandler.getSelectedBluetoothDevice());

        return contentView;
    }

    private void animateShowView() {
        getView().setVisibility(View.VISIBLE);
        getView().startAnimation(
                AnimationUtils.loadAnimation(
                        getActivity(), R.anim.anim_up));
    }

    /**
     *
     */
    private void animateHideView() {
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
                mOBDTypeTextView.setText(R.string.dashboard_bluetooth_disabled);
                mOBDTypeSubTextView.setText(R.string.dashboard_bluetooth_disabled_advise);
                mOBDTypeSubTextView.setVisibility(View.VISIBLE);
            } else if (device == null) {
                mOBDTypeTextView.setText(R.string.dashboard_obd_not_selected);
                mOBDTypeSubTextView.setText(R.string.dashboard_obd_not_selected_advise);
                mOBDTypeSubTextView.setVisibility(View.VISIBLE);
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
            mCarTypeTextView.setText(R.string.dashboard_carselection_no_car_selected);
            mCarTypeSubTextView.setText(R.string.dashboard_carselection_no_car_selected_advise);
            mCarTypeSubTextView.setVisibility(View.VISIBLE);
        }
    }
}
