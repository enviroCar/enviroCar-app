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

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
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
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.squareup.otto.Subscribe;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.injection.components.MainActivityComponent;
import org.envirocar.app.injection.modules.MainActivityModule;
import org.envirocar.app.R;
import org.envirocar.app.events.TrackPathOverlayEvent;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.views.trackdetails.MapLayer;
import org.envirocar.core.logging.Logger;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * @author dewall
 */
public class TrackMapFragment extends BaseInjectorFragment implements PermissionsListener {
    private static final Logger LOG = Logger.getLogger(TrackMapFragment.class);

    private PermissionsManager permissionsManager;
    private MapboxMap mapboxMap;
    private Style mapStyle;
    private LocationComponent locationComponent;
    @BindView(R.id.fragment_dashboard_frag_map_mapview)
    protected MapView mMapView;
    @BindView(R.id.activity_map_follow_fab)
    protected FloatingActionButton mFollowFab;

    private MapLayer mPathOverlay;
    private List<Point> points = new ArrayList<>();

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
        mMapView.onCreate(savedInstanceState);

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap mapbox) {
                mapboxMap = mapbox;
                mapbox.getUiSettings().setLogoEnabled(false);
                mapbox.getUiSettings().setAttributionEnabled(false);
                mapbox.setMinZoomPreference(18);
                mapbox.setStyle(new Style.Builder().fromUrl("https://api.maptiler.com/maps/basic/style.json?key=YJCrA2NeKXX45f8pOV6c "),
                        new Style.OnStyleLoaded() {
                            @Override
                            public void onStyleLoaded(@NonNull Style style) {
                                enableLocationComponent(style);
                                mapStyle = style;
                                // If the mPathOverlay has already been set, then add the overlay to the mapview.
                                //if (mPathOverlay != null) {
                                //    mapStyle.addSource(mPathOverlay.getGeoJsonSource());
                                //    mapStyle.addLayer(mPathOverlay.getLineLayer());
                                //}
                                GeoJsonSource geoJsonSource = new GeoJsonSource("source-id", FeatureCollection.fromFeatures(new Feature[] {Feature.fromGeometry(
                                        LineString.fromLngLats(points)
                                )}));
                                style.addSource(geoJsonSource);

                                LineLayer lineLayer = new LineLayer("linelayer", "source-id").withSourceLayer("source-id").withProperties(
                                        PropertyFactory.lineColor(Color.BLUE),
                                        PropertyFactory.lineWidth(4f)
                                );
                                style.addLayer(lineLayer);
                            }
                        });
            }
        });
        mIsFollowingLocation = true;
        mFollowFab.setVisibility(View.INVISIBLE);

        return contentView;
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

    @OnClick(R.id.activity_map_follow_fab)
    protected void onClickFollowFab() {
        if (!mIsFollowingLocation) {
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
            points = mPathOverlay.getPoints();

            if (mMapView != null) {
                mMapView.getMapAsync(
                        new OnMapReadyCallback() {
                            @Override
                            public void onMapReady(@NonNull MapboxMap mapboxMap) {
                                TrackMapFragment.this.mapboxMap = mapboxMap;
                                mapboxMap.getStyle(new Style.OnStyleLoaded() {
                                    @Override
                                    public void onStyleLoaded(@NonNull Style style) {
                                        style.removeLayer("linelayer");
                                        if(style.removeSource("source-id"))
                                        {
                                            Log.i("Info","removeSource successfull");
                                            GeoJsonSource geoJsonSource = new GeoJsonSource("source-id", FeatureCollection.fromFeatures(new Feature[] {Feature.fromGeometry(
                                                    LineString.fromLngLats(points)
                                            )}));
                                            style.addSource(geoJsonSource);
                                            LineLayer lineLayer = new LineLayer("linelayer", "source-id").withSourceLayer("source-id").withProperties(
                                                    PropertyFactory.lineColor(Color.BLUE),
                                                    PropertyFactory.lineWidth(4f)
                                            );
                                            style.addLayer(lineLayer);
                                        } else{
                                            Log.i("Info","removeSource failed");
                                        }

                                    }
                                });
                            }
                        }
                );
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
        if(mMapView!=null){
            LOG.info("mMapView is not null. onDestroy() called.");
            mMapView.onDestroy();
        } else{
            LOG.info("mMapView is null. onDestroy() wasn't called.");
        }

    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

}
