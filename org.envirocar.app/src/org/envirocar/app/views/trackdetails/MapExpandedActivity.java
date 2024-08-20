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
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.envirocar.app.R;
import org.envirocar.app.databinding.ActivityMapExpandedBinding;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.views.utils.MapProviderRepository;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.EnviroCarDB;
import org.envirocar.map.MapController;
import org.envirocar.map.MapView;
import org.envirocar.map.model.Animation;
import org.envirocar.map.model.AttributionSettings;
import org.envirocar.map.model.LogoSettings;
import org.envirocar.map.model.Polyline;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import io.reactivex.schedulers.Schedulers;

import static android.view.View.GONE;

public class MapExpandedActivity extends BaseInjectorActivity {
    private static final Logger LOG = Logger.getLogger(MapExpandedActivity.class);

    protected static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("#.##");
    private static final String EXTRA_TRACKID = "org.envirocar.app.extraTrackID";
    private static final String EXTRA_TITLE = "org.envirocar.app.extraTitle";

    private ActivityMapExpandedBinding binding;

    @Inject
    protected EnviroCarDB enviroCarDB;

    protected FloatingActionButton mCentreFab;
    protected FloatingActionButton mVisualiseFab;
    protected ImageView mMapViewExpandedCancel;
    protected MapView mMapViewExpanded;
    protected ConstraintLayout mMapViewExpandedContainer;
    protected CardView legendCard;
    protected ImageView legend;
    protected TextView legendStart;
    protected TextView legendMid;
    protected TextView legendEnd;
    protected TextView legendName;

    private Polyline mPolyline;
    private MapController mMapController;
    private Track track;
    private List<Measurement.PropertyKey> options = new ArrayList<>();
    private List<String> spinnerStrings = new ArrayList<>();
    private boolean mIsCentredOnTrack;

    public static void createInstance(Activity activity, int trackID) {
        Intent intent = new Intent(activity, MapExpandedActivity.class);
        intent.putExtra(EXTRA_TRACKID, trackID);
        activity.startActivity(intent);
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapExpandedBinding.inflate(getLayoutInflater());
        final View view = binding.getRoot();
        setContentView(view);

        mCentreFab = binding.activityMapFollowFab;
        mVisualiseFab = binding.activityMapVisualiseFab;
        mMapViewExpandedCancel = binding.activityTrackDetailsExpandedMapCancel;
        mMapViewExpanded = binding.activityTrackDetailsExpandedMap;
        mMapViewExpandedContainer = binding.activityTrackDetailsExpandedMapContainer;
        legendCard = binding.legendCard;
        legend = binding.legend;
        legendStart = binding.legendStart;
        legendMid = binding.legendMid;
        legendEnd = binding.legendEnd;
        legendName = binding.legendUnit;

        mMapViewExpanded.setOnTouchListener((v, event) -> onTouchMapView());
        mCentreFab.setOnClickListener(v -> onClickFollowFab());
        mVisualiseFab.setOnClickListener(v -> onClickVisualiseFab());

        // Get the track to show.
        int trackID = getIntent().getIntExtra(EXTRA_TRACKID, -1);
        Track.TrackId trackid = new Track.TrackId(trackID);
        Track track = enviroCarDB.getTrack(trackid)
                .subscribeOn(Schedulers.io())
                .blockingFirst();
        this.track = track;

        options = track.getSupportedProperties();
        for (Measurement.PropertyKey propertyKey : options) {
            spinnerStrings.add(getString(propertyKey.getStringResource()));
        }
        spinnerStrings.add("None");

        initMapView();

        mIsCentredOnTrack = true;
        mCentreFab.show();
        mMapViewExpandedCancel.setOnClickListener(v -> finish());
    }

    protected boolean onTouchMapView() {
        if (mIsCentredOnTrack) {
            mIsCentredOnTrack = false;
            TransitionManager.beginDelayedTransition(mMapViewExpandedContainer, new androidx.transition.Slide(Gravity.RIGHT));
            mCentreFab.show();
        }
        return false;
    }

    protected void onClickFollowFab() {
        if (!mIsCentredOnTrack) {
            mIsCentredOnTrack = true;
            TransitionManager.beginDelayedTransition(mMapViewExpandedContainer, new androidx.transition.Slide(Gravity.RIGHT));
            mCentreFab.hide();
            if (mMapController != null) {
                mMapController.notifyCameraUpdate(
                    Objects.requireNonNull(new TrackMapFactory(track).getCameraUpdateBasedOnBounds()),
                    new Animation.Builder().withDuration(2500).build()
                );
            }
        }
    }

    protected void onClickVisualiseFab() {
        LOG.info("onClickVisualiseFab");
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(R.string.mapview_extended_select_phenomena);
        b.setItems(spinnerStrings.toArray(new String[0]), (dialogInterface, i) -> {
            dialogInterface.dismiss();
            LOG.info("Choice: " + i);
            addPolyline(i);
        });
        b.show();

    }

    private void addPolyline(int choice) {
        if (mMapController != null) {

            final TrackMapFactory factory = new TrackMapFactory(track);

            if (mPolyline != null) {
                mMapController.removePolyline(mPolyline);
            }

            if (!spinnerStrings.get(choice).equalsIgnoreCase("None")) {

                final Measurement.PropertyKey key = options.get(choice);

                // Display gradient polyline.

                mPolyline = factory.getGradientPolyline(key);
                mMapController.addPolyline(Objects.requireNonNull(mPolyline));

                // Set legend values.

                if (legendCard.getVisibility() != View.VISIBLE) {
                    TransitionManager.beginDelayedTransition(legendCard, new androidx.transition.Slide(Gravity.LEFT));
                    legendCard.setVisibility(View.VISIBLE);
                } else {
                    TransitionManager.beginDelayedTransition(legendCard, new ChangeBounds());
                }

                try {
                    legendStart.setText(DECIMAL_FORMATTER.format(factory.getGradientMinValue(key)));
                    legendEnd.setText(DECIMAL_FORMATTER.format(factory.getGradientMaxValue(key)));
                    Float mid = (Objects.requireNonNull(factory.getGradientMinValue(key)) + Objects.requireNonNull(factory.getGradientMaxValue(key))) / 2;
                    legendMid.setText(DECIMAL_FORMATTER.format(mid));
                    legendName.setText(options.get(choice).getStringResource());
                } catch (Exception e){
                    LOG.error("Error while formatting legend.", e);
                }

            } else {

                // Display polyline.

                mPolyline = factory.getPolyline();
                mMapController.addPolyline(Objects.requireNonNull(mPolyline));

                TransitionManager.beginDelayedTransition(legendCard, new androidx.transition.Slide(Gravity.LEFT));
                legendCard.setVisibility(GONE);

            }
            mMapController.notifyCameraUpdate(
                Objects.requireNonNull(factory.getCameraUpdateBasedOnBounds()),
                null
            );
        }
    }

    private void initMapView() {
        if (mMapController != null) {
            return;
        }
        mMapController = mMapViewExpanded.getController(
                new MapProviderRepository(
                        getApplication(),
                        // Display attribution in top right of the screen.
                        new AttributionSettings.Builder()
                                .withGravity(Gravity.TOP | Gravity.END)
                                .withMargin(new float[]{12.0F, 12.0F, 12.0F, 12.0F})
                                .build(),
                        new LogoSettings.Builder()
                                .withGravity(Gravity.TOP | Gravity.END)
                                .withMargin(new float[]{12.0F, 12.0F, 84.0F, 12.0F})
                                .build()
                ).getValue()
        );
        final TrackMapFactory factory = new TrackMapFactory(track);

        mMapController.setMinZoom(factory.getMinZoom());
        mMapController.setMaxZoom(factory.getMaxZoom());
        mMapController.addMarker(Objects.requireNonNull(factory.getStartMarker()));
        mMapController.addMarker(Objects.requireNonNull(factory.getStopMarker()));
        mMapController.notifyCameraUpdate(Objects.requireNonNull(factory.getCameraUpdateBasedOnBounds()), null);

        if (options.contains(Measurement.PropertyKey.SPEED)) {
            addPolyline(options.indexOf(Measurement.PropertyKey.SPEED));
        } else {
            addPolyline(options.indexOf(Measurement.PropertyKey.GPS_SPEED));
        }
    }
}
