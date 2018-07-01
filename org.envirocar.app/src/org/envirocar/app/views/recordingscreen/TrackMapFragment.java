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
package org.envirocar.app.views.recordingscreen;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.mapbox.mapboxsdk.overlay.PathOverlay;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.squareup.otto.Subscribe;

import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.main.MainActivityComponent;
import org.envirocar.app.main.MainActivityModule;
import org.envirocar.app.R;
import org.envirocar.app.events.TrackPathOverlayEvent;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.views.utils.MapUtils;
import org.envirocar.core.logging.Logger;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

/**
 * @author dewall
 */
public class TrackMapFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(TrackMapFragment.class);

    @BindView(R.id.fragment_dashboard_frag_map_mapview)
    protected MapView mMapView;
    @BindView(R.id.fragment_dashboard_frag_map_follow_fab)
    protected FloatingActionButton mFollowFab;

    private PathOverlay mPathOverlay;

    private final Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread()
            .createWorker();

    private boolean mIsFollowingLocation;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        LOG.info("onCreateView()");

        // First inflate the general dashboard view.
        View contentView = inflater.inflate(R.layout.fragment_track_map, container, false);

        // Inject all dashboard-related views.
        ButterKnife.bind(this, contentView);

        // Init the map view
        mMapView.setTileSource(MapUtils.getOSMTileLayer());
        mMapView.setUserLocationEnabled(true);
        mMapView.setUserLocationTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW_BEARING);
        mMapView.setUserLocationRequiredZoom(18);
        mIsFollowingLocation = true;
        mFollowFab.setVisibility(View.INVISIBLE);


        // If the mPathOverlay has already been set, then add the overlay to the mapview.
        if (mPathOverlay != null)
            mMapView.getOverlays().add(mPathOverlay);

        return contentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        onClickFollowFab();
    }

    @OnTouch(R.id.fragment_dashboard_frag_map_mapview)
    protected boolean onTouchMapView() {
        if (mIsFollowingLocation) {
            // Disable the follow location mode.
            UserLocationOverlay userLocationOverlay = mMapView.getUserLocationOverlay();
            userLocationOverlay.disableFollowLocation();
            mIsFollowingLocation = false;

            // show the floating action button that can enable the follow location mode.
            showFollowFAB();
        }
        return false;
    }

    @OnClick(R.id.fragment_dashboard_frag_map_follow_fab)
    protected void onClickFollowFab() {
        if (!mIsFollowingLocation) {
            UserLocationOverlay userLocationOverlay = mMapView.getUserLocationOverlay();
            userLocationOverlay.enableFollowLocation();
            userLocationOverlay.goToMyPosition(true); // animated is not working... don't know why
            mIsFollowingLocation = true;

            hideFollowFAB();
        }
    }

    /**
     * Shows the floating action button for toggling the follow location ability.
     */
    private void showFollowFAB() {
        // load the translate animation.
        Animation slideLeft = AnimationUtils.loadAnimation(getActivity(),
                R.anim.translate_slide_in_right);

        // and start it on the fab.
        mFollowFab.startAnimation(slideLeft);
        mFollowFab.setVisibility(View.VISIBLE);
    }

    /**
     *
     */
    private void hideFollowFAB() {
        // load the translate animation.
        Animation slideRight = AnimationUtils.loadAnimation(getActivity(),
                R.anim.translate_slide_out_right);

        // set a listener that makes the button invisible when the animation has finished.
        slideRight.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // nothing to do..
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mFollowFab.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // nothing to do..
            }
        });

        // and start it on the fab.
        mFollowFab.startAnimation(slideRight);
    }

    @Subscribe
    public void onReceivePathOverlayEvent(TrackPathOverlayEvent event) {
        mMainThreadWorker.schedule(() -> {
            mPathOverlay = event.mTrackOverlay;
            if (mMapView != null)
                mMapView.addOverlay(mPathOverlay);
        });
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        MainActivityComponent mainActivityComponent =  baseApplicationComponent.plus(new MainActivityModule(getActivity()));
        mainActivityComponent.inject(this);
    }
}