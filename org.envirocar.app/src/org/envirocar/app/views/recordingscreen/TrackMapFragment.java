/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.OnLocationCameraTransitionListener;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.sources.TileSet;
import com.squareup.otto.Subscribe;

import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.main.MainActivityComponent;
import org.envirocar.app.main.MainActivityModule;
import org.envirocar.app.R;
import org.envirocar.app.events.TrackPathOverlayEvent;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.views.trackdetails.MapLayer;
import org.envirocar.app.views.utils.MapUtils;
import org.envirocar.core.logging.Logger;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

/**
 * @author dewall
 */
public class TrackMapFragment extends BaseInjectorFragment implements
        OnMapReadyCallback, PermissionsListener {
    private static final Logger LOG = Logger.getLogger(TrackMapFragment.class);

    private PermissionsManager permissionsManager;
    private MapboxMap mapboxMap;
    private LocationComponent locationComponent;
    @BindView(R.id.fragment_dashboard_frag_map_mapview)
    protected MapView mMapView;
    @BindView(R.id.fragment_dashboard_frag_map_follow_fab)
    protected FloatingActionButton mFollowFab;

    private MapLayer mPathOverlay;

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
        Mapbox.getInstance(Mapbox.getApplicationContext(), "");
        // Inject all dashboard-related views.
        ButterKnife.bind(this, contentView);

        // Init the map view
        mMapView.onCreate(savedInstanceState);

        mMapView.getMapAsync(this);
        //mMapView.setTileSource(MapUtils.getOSMTileLayer());
        //mMapView.setUserLocationEnabled(true);
        //mMapView.setUserLocationTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW_BEARING);
        //mMapView.setUserLocationRequiredZoom(18);
        mIsFollowingLocation = true;
        mFollowFab.setVisibility(View.INVISIBLE);


        // If the mPathOverlay has already been set, then add the overlay to the mapview.
        if (mPathOverlay != null) {
            mapboxMap.getStyle().addSource(mPathOverlay.getGeoJsonSource());
            mapboxMap.getStyle().addLayer(mPathOverlay.getLineLayer());
        }

        return contentView;
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        TrackMapFragment.this.mapboxMap = mapboxMap;
        TileSet layer = MapUtils.getOSMTileLayer();
        mapboxMap.setMaxZoomPreference(layer.getMaxZoom());
        mapboxMap.setMinZoomPreference(layer.getMinZoom());
        mapboxMap.setStyle(new Style.Builder().fromUrl("https://api.maptiler.com/maps/basic/style.json?key=YJCrA2NeKXX45f8pOV6c "),
                new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        enableLocationComponent(style);
                    }
                });
    }

    private void setCameraTrackingMode(@CameraMode.Mode int mode) {
        locationComponent.setCameraMode(mode, new OnLocationCameraTransitionListener() {
            @Override
            public void onLocationCameraTransitionFinished(@CameraMode.Mode int cameraMode) {
                if (mode != CameraMode.NONE) {
                    locationComponent.zoomWhileTracking(15, 750, new MapboxMap.CancelableCallback() {
                        @Override
                        public void onCancel() {
                            // No impl
                        }

                        @Override
                        public void onFinish() {
                            locationComponent.tiltWhileTracking(45);
                        }
                    });
                } else {
                    mapboxMap.easeCamera(CameraUpdateFactory.tiltTo(0));
                }
            }

            @Override
            public void onLocationCameraTransitionCanceled(@CameraMode.Mode int cameraMode) {
                // No impl
            }
        });
    }

    @OnTouch(R.id.fragment_dashboard_frag_map_mapview)
    protected boolean onTouchMapView() {
        if (mIsFollowingLocation) {
            setCameraTrackingMode(CameraMode.NONE);
            mIsFollowingLocation = false;

            // show the floating action button that can enable the follow location mode.
            showFollowFAB();
        }
        return false;
    }

    @OnClick(R.id.fragment_dashboard_frag_map_follow_fab)
    protected void onClickFollowFab() {
        if (!mIsFollowingLocation) {
            //UserLocationOverlay userLocationOverlay = mMapView.getUserLocationOverlay();
            //userLocationOverlay.enableFollowLocation();
            //userLocationOverlay.goToMyPosition(true); // animated is not working... don't know why
            setCameraTrackingMode(CameraMode.TRACKING_GPS);
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
            if (mMapView != null) {
                mapboxMap.getStyle(new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        style.addSource(mPathOverlay.getGeoJsonSource());
                        style.addLayer(mPathOverlay.getLineLayer());
                    }
                });
            }
        });
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        MainActivityComponent mainActivityComponent =  baseApplicationComponent.plus(new MainActivityModule(getActivity()));
        mainActivityComponent.inject(this);
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(getContext())) {

            // Get an instance of the component
            locationComponent = mapboxMap.getLocationComponent();

            // Activate with options
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions
                            .builder(getContext(), loadedMapStyle)
                            .useDefaultLocationEngine(true)
                            .locationEngineRequest(new LocationEngineRequest.Builder(750)
                                    .setFastestInterval(750)
                                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                                    .build())
                            .build());

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

            // Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(getActivity());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(getContext(), "We need access to your location.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });
        } else {
            Toast.makeText(getContext(), "Location access not granted", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    @SuppressWarnings( {"MissingPermission"})
    public void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        onClickFollowFab();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

}
