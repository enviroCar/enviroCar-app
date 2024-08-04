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

import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.databinding.ActivityTrackDetailsLayoutBinding;
import org.envirocar.app.handler.ApplicationSettings;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.views.utils.MapProviderRepository;
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
import org.envirocar.map.MapController;
import org.envirocar.map.MapView;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import javax.inject.Inject;

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

    private ActivityTrackDetailsLayoutBinding binding;

    @Inject
    protected EnviroCarDB mEnvirocarDB;

    protected FloatingActionButton mFAB;
    protected MapView mMapView;
    protected Toolbar mToolbar;
    protected TextView mDescriptionText;
    protected TextView mDurationText;
    protected TextView mDistanceText;
    protected TextView mBeginText;
    protected TextView mEndText;
    protected TextView mCarText;
    protected TextView mEmissionKey;
    protected TextView mEmissionText;
    protected TextView mConsumptionKey;
    protected TextView mConsumptionText;
    protected AppBarLayout mAppBarLayout;
    protected NestedScrollView mNestedScrollView;
    protected FrameLayout mMapViewContainer;
    protected RelativeLayout mConsumptionContainer;
    protected RelativeLayout mCo2Container;
    protected TextView descriptionTv;
    protected RelativeLayout speedLayout;
    protected TextView speedText;
    protected RelativeLayout stopsLayout;
    protected TextView stopsValue;
    protected RelativeLayout stoptimeLayout;
    protected TextView stoptimeValue;

    private Track track;
    private MapController mMapController;

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActivityTransition();

        binding = ActivityTrackDetailsLayoutBinding.inflate(getLayoutInflater());
        final View view = binding.getRoot();
        setContentView(view);

        mFAB = binding.activityTrackDetailsFab;
        mMapView = binding.activityTrackDetailsHeaderMap;
        mToolbar = binding.activityTrackDetailsHeaderToolbar;
        mDescriptionText = binding.activityTrackDetailsAttrDescriptionValue;
        mDurationText = binding.activityTrackDetailsAttributesHeader.trackDetailsAttributesHeaderDuration;
        mDistanceText = binding.activityTrackDetailsAttributesHeader.trackDetailsAttributesHeaderDistance;
        mBeginText = binding.activityTrackDetailsAttributes.activityTrackDetailsAttrBeginValue;
        mEndText = binding.activityTrackDetailsAttributes.activityTrackDetailsAttrEndValue;
        mCarText = binding.activityTrackDetailsAttributes.activityTrackDetailsAttrCarValue;
        mEmissionKey = binding.activityTrackDetailsAttributes.activityTrackDetailsAttrEmissionText;
        mEmissionText = binding.activityTrackDetailsAttributes.activityTrackDetailsAttrEmissionValue;
        mConsumptionKey = binding.activityTrackDetailsAttributes.activityTrackDetailsAttrConsumptionText;
        mConsumptionText = binding.activityTrackDetailsAttributes.activityTrackDetailsAttrConsumptionValue;
        mAppBarLayout = binding.activityTrackDetailsAppbarLayout;
        mNestedScrollView = binding.activityTrackDetailsScrollview;
        mMapViewContainer = binding.activityTrackDetailsHeaderMapContainer;
        mConsumptionContainer = binding.activityTrackDetailsAttributes.consumptionContainer;
        mCo2Container = binding.activityTrackDetailsAttributes.co2Container;
        descriptionTv = binding.activityTrackDetailsAttributes.descriptionTv;
        speedLayout = binding.activityTrackDetailsAttributes.activityTrackDetailsSpeedContainer;
        speedText = binding.activityTrackDetailsAttributes.activityTrackDetailsSpeedValue;
        stopsLayout = binding.activityTrackDetailsAttributes.activityTrackDetailsStopsContainer;
        stopsValue = binding.activityTrackDetailsAttributes.activityTrackDetailsStopsValue;
        stoptimeLayout = binding.activityTrackDetailsAttributes.activityTrackDetailsStoptimeContainer;
        stoptimeValue = binding.activityTrackDetailsAttributes.activityTrackDetailsStoptimeValue;

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

        String itemTitle = track.getName();
        CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle(itemTitle);
        collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color.transparent));
        collapsingToolbarLayout.setStatusBarScrimColor(getResources().getColor(android.R.color.transparent));

        TextView title = findViewById(R.id.title);
        title.setText(itemTitle);

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
        // TODO(alexmercerind): Retrieve currently selected provider from a common repository.
        if (mMapController != null) {
            return;
        }
        mMapController = mMapView.getController(new MapProviderRepository(getApplication()).getValue());

        final TrackMapFactory factory = new TrackMapFactory(track);

        mMapController.setMinZoom(factory.getMinZoom());
        mMapController.setMaxZoom(factory.getMaxZoom());
        mMapController.addPolyline(Objects.requireNonNull(factory.getPolyline()));
        mMapController.addMarker(Objects.requireNonNull(factory.getStartMarker()));
        mMapController.addMarker(Objects.requireNonNull(factory.getStopMarker()));
        mMapController.notifyCameraUpdate(Objects.requireNonNull(factory.getCameraUpdateBasedOnBounds()), null);
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
    public void onResume() {
        super.onResume();
        supportStartPostponedEnterTransition();
    }
}
