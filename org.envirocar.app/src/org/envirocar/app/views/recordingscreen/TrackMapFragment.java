/**
 * Copyright (C) 2013 - 2021 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.recordingscreen;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.squareup.otto.Subscribe;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.databinding.FragmentTrackMapBinding;
import org.envirocar.app.events.TrackPathOverlayEvent;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.injection.components.MainActivityComponent;
import org.envirocar.app.injection.modules.MainActivityModule;
import org.envirocar.app.views.utils.MapProviderRepository;
import org.envirocar.core.logging.Logger;
import org.envirocar.map.MapController;
import org.envirocar.map.MapView;
import org.envirocar.map.camera.CameraUpdateFactory;
import org.envirocar.map.location.LocationIndicator;
import org.envirocar.map.location.LocationIndicatorCameraMode;
import org.envirocar.map.model.Polyline;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * @author dewall
 */
public class TrackMapFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(TrackMapFragment.class);

    private FragmentTrackMapBinding binding;

    protected MapView mMapView;
    protected FloatingActionButton mFollowFab;

    private MapController mMapController;
    private LocationIndicator mLocationIndicator;

    private Polyline mCurrentPolyline;

    private final Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();

    private boolean mIsFollowingLocation;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LOG.info("onCreateView()");

        binding = FragmentTrackMapBinding.inflate(inflater, container, false);
        final View view = binding.getRoot();

        mMapView = binding.fragmentDashboardFragMapMapview;
        mFollowFab = binding.activityMapFollowFab;

        mMapView.setOnTouchListener((v, event) -> onTouchMapView());
        mFollowFab.setOnClickListener(v -> onClickFollowFab());

        mMapController = mMapView.getController(new MapProviderRepository(requireActivity().getApplication()).getValue());
        mMapController.setMinZoom(16.0F);
        mMapController.notifyCameraUpdate(CameraUpdateFactory.newCameraUpdateZoom(16.0F), null);

        mLocationIndicator = null;
        mCurrentPolyline = null;

        enableLocationIndicator();

        mIsFollowingLocation = true;
        mFollowFab.setVisibility(View.INVISIBLE);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    protected boolean onTouchMapView() {
        if (mIsFollowingLocation) {
            mLocationIndicator.setCameraMode(LocationIndicatorCameraMode.None.INSTANCE);
            mIsFollowingLocation = false;

            // show the floating action button that can enable the follow location mode.
            showFollowFAB();
        }
        return false;
    }

    protected void onClickFollowFab() {
        if (!mIsFollowingLocation) {
            mLocationIndicator.setCameraMode(new LocationIndicatorCameraMode.Follow());
            mIsFollowingLocation = true;

            hideFollowFAB();
        }
    }

    /**
     * Shows the floating action button for toggling the follow location ability.
     */
    private void showFollowFAB() {
        // load the translate animation.
        Animation slideLeft = AnimationUtils.loadAnimation(getActivity(), R.anim.translate_slide_in_right);

        // and start it on the fab.
        mFollowFab.startAnimation(slideLeft);
        mFollowFab.setVisibility(View.VISIBLE);
    }

    /**
     *
     */
    private void hideFollowFAB() {
        // load the translate animation.
        Animation slideRight = AnimationUtils.loadAnimation(getActivity(), R.anim.translate_slide_out_right);

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
            if (event.points.isEmpty()) {
                return;
            }
            if (mCurrentPolyline != null) {
                mMapController.removePolyline(mCurrentPolyline);
            }
            mCurrentPolyline = new Polyline.Builder(event.points)
                    .withColor(Color.BLUE)
                    .withWidth(4.0F)
                    .build();
            mMapController.addPolyline(mCurrentPolyline);
        });
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        MainActivityComponent mainActivityComponent = baseApplicationComponent.plus(new MainActivityModule(getActivity()));
        mainActivityComponent.inject(this);
    }

    private void enableLocationIndicator() {
        if (mLocationIndicator != null) return;
        if (
                requireContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                requireContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            mLocationIndicator = new LocationIndicator(mMapController, requireContext());
            mLocationIndicator.setCameraMode(new LocationIndicatorCameraMode.Follow());
            mLocationIndicator.enable();
        } else {
            final var launcher = registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        for (final Boolean value : result.values()) {
                            if (!value) {
                                Toast.makeText(
                                        requireContext(),
                                        requireContext().getString(R.string.notification_location_access_not_granted),
                                        Toast.LENGTH_LONG
                                ).show();
                                return;
                            }
                        }
                        enableLocationIndicator();
                    });
            launcher.launch(new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }
}
