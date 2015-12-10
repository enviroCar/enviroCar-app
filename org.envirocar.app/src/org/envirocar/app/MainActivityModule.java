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
package org.envirocar.app;


import android.app.Activity;
import android.content.Context;

import org.envirocar.app.activity.StartStopButtonUtil;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.handler.TermsOfUseManager;
import org.envirocar.app.view.LogbookFragment;
import org.envirocar.app.view.RegisterFragment;
import org.envirocar.app.view.dashboard.DashboardMainFragment;
import org.envirocar.app.view.dashboard.DashboardMapFragment;
import org.envirocar.app.view.dashboard.DashboardTempomatFragment;
import org.envirocar.app.view.dashboard.DashboardTrackDetailsFragment;
import org.envirocar.app.view.dashboard.DashboardTrackMapFragment;
import org.envirocar.app.view.dashboard.DashboardTrackSettingsFragment;
import org.envirocar.app.view.dashboard.RealDashboardFragment;
import org.envirocar.app.view.preferences.Tempomat;
import org.envirocar.app.view.tracklist.AbstractTrackListCardFragment;
import org.envirocar.app.view.tracklist.TrackListLocalCardFragment;
import org.envirocar.app.view.tracklist.TrackListPagerFragment;
import org.envirocar.app.view.tracklist.TrackListRemoteCardFragment;
import org.envirocar.core.injection.InjectionActivityScope;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Module(
        injects = {
                BaseMainActivity.class,
                TermsOfUseManager.class,
                CarPreferenceHandler.class,
                LogbookFragment.class,
                RegisterFragment.class,
                StartStopButtonUtil.class,
                RealDashboardFragment.class,
                Tempomat.class,
                DashboardMainFragment.class,
                LogbookFragment.class,
                RegisterFragment.class,
                DashboardTrackDetailsFragment.class,
                DashboardTempomatFragment.class,
                DashboardTrackSettingsFragment.class,
                DashboardMapFragment.class,
                DashboardTrackMapFragment.class,
                TrackListPagerFragment.class,
                AbstractTrackListCardFragment.class,
                TrackListLocalCardFragment.class,
                TrackListRemoteCardFragment.class,
        },
        addsTo = BaseApplicationModule.class,
        library = true,
        complete = false
)
public class MainActivityModule {

    private Activity mActivity;

    /**
     * Constructor
     *
     * @param activity the activity of this scope.
     */
    public MainActivityModule(Activity activity) {
        this.mActivity = activity;
    }


    @Provides
    public Activity provideActivity() {
        return mActivity;
    }

    @Provides
    @InjectionActivityScope
    public Context provideContext() {
        return mActivity;
    }

    @Provides
    @Singleton
    public RealDashboardFragment provideRealDashboardFragment(){
        return new RealDashboardFragment();
    }

}
