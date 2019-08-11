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
package org.envirocar.app.views.trackdetails;

import android.animation.FloatEvaluator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.transition.Slide;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.core.entity.Track;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.trackprocessing.statistics.TrackStatisticsProvider;
import org.envirocar.storage.EnviroCarDB;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.schedulers.Schedulers;


/**
 * @author dewall
 */
public class TrackDetailsActivity extends BaseInjectorActivity {
    private static final Logger LOG = Logger.getLogger(TrackDetailsActivity.class);

    private static final String EXTRA_TRACKID = "org.envirocar.app.extraTrackID";
    private static final String EXTRA_TITLE = "org.envirocar.app.extraTitle";
    private static final DecimalFormat DECIMAL_FORMATTER_TWO_DIGITS = new DecimalFormat("#.##");
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();
    private static final DateFormat UTC_DATE_FORMATTER = new SimpleDateFormat("HH:mm:ss", Locale
            .ENGLISH);

    public static void navigate(Activity activity, View transition, int trackID) {
        Intent intent = new Intent(activity, TrackDetailsActivity.class);
        intent.putExtra(EXTRA_TRACKID, trackID);
        ActivityOptionsCompat options = ActivityOptionsCompat
                .makeBasic();
        ActivityCompat.startActivity(activity, intent, options.toBundle());
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
    @BindView(R.id.track_details_attributes_header_duration1)
    protected TextView mDurationText1;
    @BindView(R.id.track_details_attributes_header_duration2)
    protected TextView mDurationText2;
    @BindView(R.id.track_details_attributes_header_distance)
    protected TextView mDistanceText;

    @BindView(R.id.track_details_attributes_header_date)
    protected TextView mDateText;
    @BindView(R.id.track_details_attributes_header_time)
    protected TextView mTimeText;
    @BindView(R.id.activity_track_details_appbar_layout)
    protected AppBarLayout mAppBarLayout;

    @BindView(R.id.activity_track_details_scrollview)
    protected NestedScrollView mNestedScrollView;

    @BindView(R.id.title)
    protected TextView title;
    @BindView(R.id.timeImage)
    protected ImageView timeImage;
    @BindView(R.id.time1)
    protected TextView timeTV1;
    @BindView(R.id.time2)
    protected TextView timeTV2;
    @BindView(R.id.viewPager)
    protected ViewPager viewPager;
    @BindView(R.id.indicator_0)
    protected Button indicator0;
    @BindView(R.id.indicator_1)
    protected Button indicator1;

    @BindView(R.id.activity_track_details_header_map_container)
    protected FrameLayout mMapViewContainer;

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
        track = mEnvirocarDB.getTrack(trackid)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .first();
        this.track = track;

        trackMapOverlay = new TrackMapLayer(track);
        Bundle bundle = new Bundle();
        bundle.putString("ID", track.getRemoteID());
        bundle.putInt("track", mTrackID);
        viewPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager(), bundle));
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                FloatEvaluator evaluator = new FloatEvaluator();
                Float update1 = evaluator.evaluate(positionOffset, 1f, 0.2f);
                Float update2 = evaluator.evaluate(positionOffset, 0.2f, 1f);
                if(position == 0)
                {
                    indicator0.setAlpha(update1);
                    indicator1.setAlpha(update2);
                }
                else{
                    indicator0.setAlpha(update2);
                    indicator1.setAlpha(update1);
                }
            }

            @Override
            public void onPageSelected(int position) {
                if(position == 0)
                {
                    indicator0.setAlpha(1);
                    indicator1.setAlpha(0.2f);
                }
                else{
                    indicator0.setAlpha(0.2f);
                    indicator1.setAlpha(1);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        CollapsingToolbarLayout collapsingToolbarLayout = findViewById
                (R.id.collapsing_toolbar);

        collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color
                .transparent));
        collapsingToolbarLayout.setStatusBarScrimColor(getResources().getColor(android.R.color
                .transparent));

        setTitleAndAttr();
        collapsingToolbarLayout.setTitle(title.getText().toString());
        // Initialize the mapview and the trackpath
        initMapView();
        initViewValues();

        updateStatusBarColor();
        mFAB.setOnClickListener(v -> {
           TrackStatisticsActivity.createInstance(TrackDetailsActivity.this, mTrackID);
        });

        mMapViewContainer.setOnClickListener(v-> MapExpandedActivity.createInstance(TrackDetailsActivity.this, mTrackID));
    }

    private void setTitleAndAttr(){
        try{
            Date trackDate = new Date(track.getStartTime());
            String carName = " with the " + track.getCar().getModel();
            Integer hh = Integer.parseInt(new SimpleDateFormat("HH", Locale.getDefault()).format(trackDate));
            if(hh < 4 || hh > 19) {
                title.setText("Your Night Track" + carName);
                timeImage.setImageResource(R.drawable.night);
            }
            else if(hh >= 4 && hh < 9) {
                title.setText("Your Morning Track" + carName);
                timeImage.setImageResource(R.drawable.morning);
            }
            else if(hh > 9 && hh < 15) {
                title.setText("Your Afternoon Track" + carName);
                timeImage.setImageResource(R.drawable.afternoon);
            }
            else {
                title.setText("Your Evening Track" + carName);
                timeImage.setImageResource(R.drawable.evening);
            }
            String trackDateS = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(trackDate);
            String trackTimeS = new SimpleDateFormat("KK:mm a", Locale.getDefault()).format(trackDate);
            mDateText.setText(trackDateS);
            mTimeText.setText(trackTimeS);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void convertMillisToDate(){
        try {
            long timeInMillis = track.getDuration();
            long diffSeconds = timeInMillis / 1000 % 60;
            long diffMinutes = timeInMillis / (60 * 1000) % 60;
            long diffHours = timeInMillis / (60 * 60 * 1000) % 24;
            long diffDays = timeInMillis / (24 * 60 * 60 * 1000);
            if (diffDays != 0) {
                mDurationText1.setText(diffDays+"");
                timeTV1.setText("D");
                if (diffHours > 1) {
                    mDurationText2.setVisibility(View.VISIBLE);
                    timeTV2.setVisibility(View.VISIBLE);
                    mDurationText2.setText(diffHours+"");
                    timeTV2.setText("H");
                }
                else{
                    mDurationText2.setVisibility(View.INVISIBLE);
                    timeTV2.setVisibility(View.INVISIBLE);
                }
            } else {
                if (diffHours != 0) {
                    mDurationText1.setVisibility(View.VISIBLE);
                    timeTV1.setVisibility(View.VISIBLE);
                    mDurationText1.setText(diffHours+"");
                    timeTV1.setText("H");
                    if (diffMinutes != 0) {
                        mDurationText2.setVisibility(View.VISIBLE);
                        timeTV2.setVisibility(View.VISIBLE);
                        mDurationText2.setText(new DecimalFormat("00").format(diffMinutes)+"");
                        timeTV2.setText("M");
                    } else{
                        mDurationText2.setVisibility(View.INVISIBLE);
                        timeTV2.setVisibility(View.INVISIBLE);
                    }
                } else {
                    if (diffMinutes != 0) {
                        mDurationText1.setVisibility(View.VISIBLE);
                        timeTV1.setVisibility(View.VISIBLE);
                        mDurationText1.setText(diffMinutes+"");
                        timeTV1.setText("M");
                        if (diffSeconds != 0) {
                            mDurationText2.setVisibility(View.VISIBLE);
                            timeTV2.setVisibility(View.VISIBLE);
                            mDurationText2.setText(new DecimalFormat("00").format(diffSeconds)+"");
                            timeTV2.setText("S");
                        } else{
                            mDurationText2.setVisibility(View.INVISIBLE);
                            timeTV2.setVisibility(View.INVISIBLE);
                        }
                    } else {
                            mDurationText1.setVisibility(View.VISIBLE);
                            timeTV1.setVisibility(View.VISIBLE);
                            mDurationText1.setText(diffSeconds+"");
                            timeTV1.setText("S");
                            mDurationText2.setVisibility(View.INVISIBLE);
                            timeTV2.setVisibility(View.INVISIBLE);
                    }
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
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
            finish();
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
        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap tep) {

                tep.getUiSettings().setLogoEnabled(false);
                tep.getUiSettings().setAttributionEnabled(false);
                tep.setStyle(new Style.Builder().fromUrl("https://api.maptiler.com/maps/basic/style.json?key=YJCrA2NeKXX45f8pOV6c "), new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        mapStyle = style;
                        style.addSource(trackMapOverlay.getGeoJsonSource());
                        style.addLayer(trackMapOverlay.getLineLayer());
                        tep.moveCamera(CameraUpdateFactory.newLatLngBounds(viewBbox, 50));
                        setUpStartStopIcons(style);
                    }
                });
                mapboxMap = tep;
                mapboxMap.setMaxZoomPreference(trackMapOverlay.getMaxZoom());
                mapboxMap.setMinZoomPreference(trackMapOverlay.getMinZoom());
            }
        });
    }

    private void setUpStartStopIcons(@NonNull Style loadedMapStyle) {
        int size = track.getMeasurements().size();
        if(size>=2 && trackMapOverlay.hasLatLng())
        {
            //Set Source with start and stop marker
            Double lng = track.getMeasurements().get(0).getLongitude();
            Double lat = track.getMeasurements().get(0).getLatitude();
            GeoJsonSource geoJsonSource = new GeoJsonSource("marker-source1", Feature.fromGeometry(
                    Point.fromLngLat(lng, lat)));
            loadedMapStyle.addSource(geoJsonSource);

            lng = track.getMeasurements().get(size-1).getLongitude();
            lat = track.getMeasurements().get(size-1).getLatitude();
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

    private void initViewValues() {
            mDistanceText.setText(String.format("%s",
                    DECIMAL_FORMATTER_TWO_DIGITS.format(((TrackStatisticsProvider) track)
                            .getDistanceOfTrack())));
            convertMillisToDate();

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
        if(mapStyle != null)
        {
            mapStyle.removeLayer(MapLayer.LAYER_NAME);
            mapStyle.removeLayer("marker-layer1");
            mapStyle.removeLayer("marker-layer2");
        } else {
            LOG.info("Style was null.");
        }
        if(mMapView != null)
        {
            mMapView.onDestroy();
        } else{
            LOG.info("mMapView was null.");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }
}
