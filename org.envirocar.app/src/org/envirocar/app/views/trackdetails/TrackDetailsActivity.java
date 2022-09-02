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
package org.envirocar.app.views.trackdetails;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.transition.Slide;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.handler.ApplicationSettings;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.core.EnviroCarDB;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.FuelConsumptionException;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.UnsupportedFuelTypeException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.trackprocessing.statistics.TrackStatisticsProvider;
import org.envirocar.core.utils.CarUtils;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.schedulers.Schedulers;


/**
 * @author dewall
 */
public class TrackDetailsActivity extends BaseInjectorActivity {
    private static final Logger LOG = Logger.getLogger(TrackDetailsActivity.class);

    private static final String EXTRA_TRACKID = "org.envirocar.app.extraTrackID";
    private static final String EXTRA_TITLE = "org.envirocar.app.extraTitle";
    private static final DecimalFormat DECIMAL_FORMATTER_TWO_DIGITS = new DecimalFormat("#.##");
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();
    private static final DateFormat UTC_DATE_FORMATTER = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);

    public static void navigate(Activity activity, View transition, int trackID) {
        Intent intent = new Intent(activity, TrackDetailsActivity.class);
        intent.putExtra(EXTRA_TRACKID, trackID);
        activity.startActivity(intent);
//        ActivityOptionsCompat options = ActivityOptionsCompat.makeBasic();
//        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    static {
        UTC_DATE_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Inject
    protected EnviroCarDB mEnvirocarDB;

    @BindView(R.id.activity_track_details_fab)
    protected FloatingActionButton mFAB;
    @BindView(R.id.activity_track_details_header_map)
    protected MapView mMapView;
    @BindView(R.id.activity_track_details_header_toolbar)
    protected Toolbar mToolbar;
    @BindView(R.id.activity_track_details_attr_description_value)
    protected TextView mDescriptionText;
    @BindView(R.id.track_details_attributes_header_duration)
    protected TextView mDurationText;
    @BindView(R.id.track_details_attributes_header_distance)
    protected TextView mDistanceText;
    @BindView(R.id.activity_track_details_attr_begin_value)
    protected TextView mBeginText;
    @BindView(R.id.activity_track_details_attr_end_value)
    protected TextView mEndText;
    @BindView(R.id.activity_track_details_attr_car_value)
    protected TextView mCarText;
    @BindView(R.id.activity_track_details_attr_emission_text)
    protected TextView mEmissionKey;
    @BindView(R.id.activity_track_details_attr_emission_value)
    protected TextView mEmissionText;
    @BindView(R.id.activity_track_details_attr_consumption_text)
    protected TextView mConsumptionKey;
    @BindView(R.id.activity_track_details_attr_consumption_value)
    protected TextView mConsumptionText;
    @BindView(R.id.activity_track_details_appbar_layout)
    protected AppBarLayout mAppBarLayout;
    @BindView(R.id.activity_track_details_scrollview)
    protected NestedScrollView mNestedScrollView;
    @BindView(R.id.activity_track_details_header_map_container)
    protected FrameLayout mMapViewContainer;
    @BindView(R.id.consumption_container)
    protected RelativeLayout mConsumptionContainer;
    @BindView(R.id.co2_container)
    protected RelativeLayout mCo2Container;
    @BindView(R.id.descriptionTv)
    protected TextView descriptionTv;
    @BindView(R.id.activity_track_details_speed_container)
    protected RelativeLayout speedLayout;
    @BindView(R.id.activity_track_details_speed_value)
    protected TextView speedText;
    @BindView(R.id.activity_track_details_stops_container)
    protected RelativeLayout stopsLayout;
    @BindView(R.id.activity_track_details_stops_value)
    protected TextView stopsValue;
    @BindView(R.id.activity_track_details_stoptime_container)
    protected RelativeLayout stoptimeLayout;
    @BindView(R.id.activity_track_details_stoptime_value)
    protected TextView stoptimeValue;

    private Track track;
    TrackMapLayer trackMapOverlay;
    protected MapboxMap mapboxMap;
    protected Style mapStyle;

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActivityTransition();
        setContentView(R.layout.activity_track_details_layout);

        // Inject all annotated views.
        ButterKnife.bind(this);
        mMapView.onCreate(savedInstanceState);
        supportPostponeEnterTransition();

        // Set the toolbar as default actionbar.
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get the track to show.
        int mTrackID = getIntent().getIntExtra(EXTRA_TRACKID, -1);
        Track.TrackId trackid = new Track.TrackId(mTrackID);
        Track track = mEnvirocarDB.getTrack(trackid)
                .subscribeOn(Schedulers.io())
                .blockingFirst();
        this.track = track;

        this.trackMapOverlay = new TrackMapLayer(track);

        String itemTitle = track.getName();
        CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle(itemTitle);
        collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color.transparent));
        collapsingToolbarLayout.setStatusBarScrimColor(getResources().getColor(android.R.color.transparent));

        TextView title = findViewById(R.id.title);
        title.setText(itemTitle);

        // Initialize the mapview and the trackpath
        initMapView();
        initViewValues(track);

        updateStatusBarColor();
        mFAB.setOnClickListener(v -> {
            TrackStatisticsActivity.createInstance(TrackDetailsActivity.this, mTrackID);
        });

        mMapViewContainer.setOnClickListener(v -> MapExpandedActivity.createInstance(TrackDetailsActivity.this, mTrackID));
    }

    private void updateStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Set the statusbar to be transparent with a grey touch
            getWindow().setStatusBarColor(Color.parseColor("#3f3f3f3f"));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
//            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    /**
     * Initializes the activity enter and return transitions of the activity.
     */
    private void initActivityTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Create a new slide transition.
            Slide transition = new Slide();
            transition.excludeTarget(android.R.id.statusBarBackground, true);
            Window window = getWindow();

            // Set the created transition as enter and return transition.
            window.setEnterTransition(transition);
            window.setReturnTransition(transition);
        }
    }

    /**
     * Initializes the MapView, its base layers and settings.
     */
    private void initMapView() {
        final LatLngBounds viewBbox = trackMapOverlay.getViewBoundingBox();
        mMapView.getMapAsync(tep -> {
            tep.getUiSettings().setLogoEnabled(false);
            tep.getUiSettings().setAttributionEnabled(false);
            tep.setStyle(new Style.Builder().fromUrl("https://api.maptiler.com/maps/basic/style.json?key=YJCrA2NeKXX45f8pOV6c "), style -> {
                mapStyle = style;
                style.addSource(trackMapOverlay.getGeoJsonSource());
                style.addLayer(trackMapOverlay.getLineLayer());
                tep.moveCamera(CameraUpdateFactory.newLatLngBounds(viewBbox, 50));
                setUpStartStopIcons(style);
            });
            mapboxMap = tep;
            mapboxMap.setMaxZoomPreference(trackMapOverlay.getMaxZoom());
            mapboxMap.setMinZoomPreference(trackMapOverlay.getMinZoom());
        });
    }

    private void setUpStartStopIcons(@NonNull Style loadedMapStyle) {
        int size = track.getMeasurements().size();
        if (size >= 2) {
            //Set Source with start and stop marker
            Double lng = track.getMeasurements().get(0).getLongitude();
            Double lat = track.getMeasurements().get(0).getLatitude();
            GeoJsonSource geoJsonSource = new GeoJsonSource("marker-source1", Feature.fromGeometry(
                    Point.fromLngLat(lng, lat)));
            loadedMapStyle.addSource(geoJsonSource);

            lng = track.getMeasurements().get(size - 1).getLongitude();
            lat = track.getMeasurements().get(size - 1).getLatitude();
            geoJsonSource = new GeoJsonSource("marker-source2", Feature.fromGeometry(
                    Point.fromLngLat(lng, lat)));
            loadedMapStyle.addSource(geoJsonSource);

            //Set symbol layer to set the icons to be displayed at the start and stop
            loadedMapStyle.addImage("start-marker",
                    BitmapFactory.decodeResource(
                            this.getResources(), R.drawable.start_marker));
            SymbolLayer symbolLayer = new SymbolLayer("marker-layer1", "marker-source1");
            symbolLayer.withProperties(
                    PropertyFactory.iconImage("start-marker"),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true)
            );
            loadedMapStyle.addLayer(symbolLayer);

            loadedMapStyle.addImage("stop-marker",
                    BitmapFactory.decodeResource(
                            this.getResources(), R.drawable.stop_marker));
            symbolLayer = new SymbolLayer("marker-layer2", "marker-source2");
            symbolLayer.withProperties(
                    PropertyFactory.iconImage("stop-marker"),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true)
            );
            loadedMapStyle.addLayerAbove(symbolLayer, "marker-layer1");
        }
    }

    private void initViewValues(Track track) {
        try {
            final String text = UTC_DATE_FORMATTER.format(new Date(track.getDuration()));
            mDistanceText.setText(String.format("%s km", DECIMAL_FORMATTER_TWO_DIGITS.format(((TrackStatisticsProvider) track).getDistanceOfTrack())));
            mDurationText.setText(text);

            String ee = new SimpleDateFormat("EEEE").format(new Date(track.getStartTime()));

            Car car = track.getCar();
            mDescriptionText.setText(String.format(getString(R.string.track_list_details_subtitle_template), car.getManufacturer(), car.getModel(), ee));
            mCarText.setText(CarUtils.carToStringWithLinebreak(track.getCar(), this));
            mBeginText.setText(DATE_FORMAT.format(new Date(track.getStartTime())));
            mEndText.setText(DATE_FORMAT.format(new Date(track.getEndTime())));

            // show consumption and emission either when the fuel type of the track's car is
            // gasoline or the beta setting has been enabled.
            if (!track.hasProperty(Measurement.PropertyKey.SPEED)) {
                mConsumptionKey.setText(mConsumptionKey.getText() + " (GPS)");
                mEmissionKey.setText(mEmissionKey.getText() + " (GPS)");
                mConsumptionContainer.setVisibility(View.GONE);
                mCo2Container.setVisibility(View.GONE);
            }

            if (track.getCar().getFuelType() == Car.FuelType.GASOLINE || ApplicationSettings.isDieselConsumptionEnabled(this)) {
                TrackStatisticsProvider statsProvider = (TrackStatisticsProvider) track;

                // set consumption text.
                String fuelConsumptionText = DECIMAL_FORMATTER_TWO_DIGITS.format(statsProvider.getFuelConsumptionPerHour());
                String litrePerHundredKmText = DECIMAL_FORMATTER_TWO_DIGITS.format(statsProvider.getLiterPerHundredKm());
                this.mConsumptionText.setText(String.format("%s l/h\n%s l/100 km", fuelConsumptionText, litrePerHundredKmText));

                // set emissions
                String emissions = DECIMAL_FORMATTER_TWO_DIGITS.format(statsProvider.getGramsPerKm());
                this.mEmissionText.setText(String.format("%s g/km", emissions));
            } else {
                mEmissionText.setText(R.string.track_list_details_diesel_not_supported);
                mConsumptionText.setText(R.string.track_list_details_diesel_not_supported);
                mEmissionText.setTextColor(Color.RED);
                mConsumptionText.setTextColor(Color.RED);
            }
        } catch (NoMeasurementsException | UnsupportedFuelTypeException e) {
            LOG.error(e);
        }
        catch (FuelConsumptionException e) {
            LOG.error(e);
            if(e.getMissingProperties() != null){
                List<Measurement.PropertyKey> missingProperties = e.getMissingProperties();
                descriptionTv.setText(String.format(getString(R.string.track_list_details_no_fuel_consumption_missing_properties), missingProperties));
            }
            else {
                descriptionTv.setText(getString(R.string.track_list_details_no_fuel_consumption));
            }

            mEmissionText.setText(R.string.track_list_details_diesel_not_supported);
            mConsumptionText.setText(R.string.track_list_details_diesel_not_supported);
            mEmissionText.setTextColor(Color.RED);
            mConsumptionText.setTextColor(Color.RED);

        }

        try {
            Measurement.PropertyKey speedKey = track.hasProperty(Measurement.PropertyKey.SPEED) ? Measurement.PropertyKey.SPEED : Measurement.PropertyKey.GPS_SPEED;
            if (track.hasProperty(speedKey)) {
                double totalSpeed = 0;
                int numMeasurements = 0;

                boolean foundStop = false;
                int numStops = 0;
                long lastBeginOfStop = 0;
                long totalStopTime = 0;
                for (Measurement m : track.getMeasurements()) {
                    if (m.hasProperty(speedKey)) {
                        double speed = m.getProperty(speedKey);
                        totalSpeed += speed;
                        numMeasurements++;

                        if (speed == 0.0 && !foundStop) {
                            foundStop = true;
                            lastBeginOfStop = m.getTime();
                            numStops++;
                        } else if (speed > 0.0 && foundStop) {
                            foundStop = false;
                            totalStopTime += m.getTime() - lastBeginOfStop;
                            lastBeginOfStop = 0;
                        }
                    }
                }

                String avgSpeedText = DECIMAL_FORMATTER_TWO_DIGITS.format(totalSpeed / numMeasurements) + " km/h";
                this.speedText.setText(avgSpeedText);

                String numStopsText = String.format("%d stops", numStops);
                this.stopsValue.setText(numStopsText);

                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(totalStopTime);
                int minutes = c.get(Calendar.MINUTE);
                int seconds = c.get(Calendar.SECOND);

                String totalStopTimeText = "";
                if (minutes == 0) {
                    totalStopTimeText = String.format("%ds", seconds);
                } else {
                    totalStopTimeText = String.format("%dm %ds", minutes, seconds);
                }
                this.stoptimeValue.setText(totalStopTimeText);
            } else {
                // TODO remove exception, only for vacaction
                throw new Exception("No speed value available");
            }
        } catch (Exception e) {
            LOG.error(e);
            // TODO remove exception, only for vacaction
            // just for the case: hide views completely.
            this.speedLayout.setVisibility(View.GONE);
            this.stopsLayout.setVisibility(View.GONE);
            this.stoptimeLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        supportStartPostponedEnterTransition();
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
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapStyle != null) {
            mapStyle.removeLayer(MapLayer.LAYER_NAME);
            mapStyle.removeLayer("marker-layer1");
            mapStyle.removeLayer("marker-layer2");
        } else {
            LOG.info("Style was null.");
        }
        if (mMapView != null) {
            mMapView.onDestroy();
        } else {
            LOG.info("mMapView was null.");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }
}
