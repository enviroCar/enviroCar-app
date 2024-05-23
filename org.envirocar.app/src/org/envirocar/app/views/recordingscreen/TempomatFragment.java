/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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
package org.envirocar.app.views.recordingscreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.squareup.otto.Subscribe;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.databinding.FragmentTempomatViewBinding;
import org.envirocar.app.events.GPSSpeedChangeEvent;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.injection.components.MainActivityComponent;
import org.envirocar.app.injection.modules.MainActivityModule;
import org.envirocar.core.logging.Logger;
import org.envirocar.obd.events.SpeedUpdateEvent;

/**
 * @author dewall
 */
public class TempomatFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(TempomatFragment.class);

    private FragmentTempomatViewBinding binding;

    protected Tempomat mTempomatView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        LOG.info("onCreateView()");

        binding = FragmentTempomatViewBinding.inflate(inflater, container, false);
        final View view = binding.getRoot();

        mTempomatView = binding.fragmentDashboardTempomatView;

        // return the inflated content view.
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        LOG.info("onResume()");
        super.onResume();
    }

    @Override
    public void onPause(){
        LOG.info("onPause()");
        super.onPause();
    }

    @Override
    public void onDestroy() {
        LOG.info("onDestroy()");
        super.onDestroy();

        // null check before destroying.
        if(mTempomatView != null) {
            mTempomatView.destroyDrawingCache();
            mTempomatView = null;
        }
    }

    /**
     * Receiver method for the speed update event.
     *
     * @param event the SpeedUpdateEvent to receive over the bus.
     */
    @Subscribe
    public void onReceiveSpeedUpdateEvent(SpeedUpdateEvent event) {
        LOG.debug(String.format("Received event: %s", event.toString()));
        if(mTempomatView != null){
            mTempomatView.setSpeed(event.mSpeed);
        }
    }

    /**
     * Receiver method for the GPS speed update event.
     *
     * @param event the GPSSpeedChangeEvent to receive over the bus.
     */
    @Subscribe
    public void onReceiveGPSSpeedUpdateEvent(GPSSpeedChangeEvent event) {
        LOG.debug(String.format("Received event: %s", event.toString()));
        if(mTempomatView != null){
            mTempomatView.setSpeed((int) Math.round(event.mGPSSpeed));
        }
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        MainActivityComponent mainActivityComponent =  baseApplicationComponent.plus(new MainActivityModule(getActivity()));
        mainActivityComponent.inject(this);
    }
}
