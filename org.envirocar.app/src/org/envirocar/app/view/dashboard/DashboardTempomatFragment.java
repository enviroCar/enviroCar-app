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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.obd.events.SpeedUpdateEvent;
import org.envirocar.app.view.preferences.Tempomat;
import org.envirocar.core.injection.BaseInjectorFragment;
import org.envirocar.core.logging.Logger;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * @author dewall
 */
public class DashboardTempomatFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(DashboardTempomatFragment.class);

    @InjectView(R.id.fragment_dashboard_tempomat_view)
    protected Tempomat mTempomatView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        LOG.info("onCreateView()");

        setHasOptionsMenu(true);

        // First inflate the general dashboard view.
        View contentView = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Inject all dashboard-related views.
        ButterKnife.inject(this, contentView);

        // return the inflated content view.
        return contentView;
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_dashboard_tempomat, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Receiver method for the speed update event.
     *
     * @param event the SpeedUpdateEvent to receive over the bus.
     */
    @Subscribe
    public void onReceiveSpeedUpdateEvent(SpeedUpdateEvent event) {
        //LOG.debug(String.format("Received event: %s", event.toString()));
        if(mTempomatView != null){
            mTempomatView.setSpeed(event.mSpeed);
        }
    }

}
