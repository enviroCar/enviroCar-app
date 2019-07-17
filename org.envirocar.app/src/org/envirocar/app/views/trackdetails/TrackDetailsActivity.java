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
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.transition.Slide;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
import com.mapbox.mapboxsdk.views.MapView;

import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.views.utils.MapUtils;
import org.envirocar.core.entity.Track;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.trackprocessing.TrackStatisticsProvider;
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

    public static void navigate(Activity activity, View transition, int trackID) {
        Intent intent = new Intent(activity, TrackDetailsActivity.class);
        intent.putExtra(EXTRA_TRACKID, trackID);

        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(activity, transition, "transition_track_details");
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    private static final DecimalFormat DECIMAL_FORMATTER_TWO_DIGITS = new DecimalFormat("#.##");
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();
    private static final DateFormat UTC_DATE_FORMATTER = new SimpleDateFormat("HH:mm:ss", Locale
            .ENGLISH);

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
    @BindView(R.id.activity_track_details_expanded_map)
    protected MapView mMapViewExpanded;
    @BindView(R.id.activity_track_details_expanded_map_container)
    protected RelativeLayout mMapViewExpandedContainer;
    @BindView(R.id.activity_track_details_appbar_layout)
    protected AppBarLayout mAppBarLayout;
    @BindView(R.id.activity_track_details_scrollview)
    protected NestedScrollView mNestedScrollView;
    @BindView(R.id.activity_track_details_expanded_map_cancel)
    protected ImageView mMapViewExpandedCancel;
    @BindView(R.id.activity_track_details_header_map_container)
    protected FrameLayout mMapViewContainer;
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
    protected Track track;
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
        initTrackPath();
        initViewValues();

        updateStatusBarColor();

        mFAB.setOnClickListener(v -> {
           TrackStatisticsActivity.createInstance(TrackDetailsActivity.this, mTrackID);
        });

        //closing the expanded mapview on "cancel" button clicked
        mMapViewExpandedCancel.setOnClickListener(v-> closeExpandedMapView());
        //expanding the expandable mapview on clicking the framelayout which is surrounded by header map view in collapsingtoolbarlayout
        mMapViewContainer.setOnClickListener(v-> expandMapView(track));
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
    protected void onResume() {
        super.onResume();
        supportStartPostponedEnterTransition();
    }

    @Override
    public void onBackPressed() {
        //if the expandable mapview is expanded, then close it first
        if(mMapViewExpandedContainer != null && mMapViewExpandedContainer.getVisibility() == View.VISIBLE){
            closeExpandedMapView();
        }else{
            super.onBackPressed();
        }
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
        // Set the openstreetmap tile layer as baselayer of the map.
        WebSourceTileLayer source = MapUtils.getOSMTileLayer();
        mMapView.setTileSource(source);
        mMapViewExpanded.setTileSource(source);

        // set the bounding box and min and max zoom level accordingly.
        BoundingBox box = source.getBoundingBox();
        mMapView.setScrollableAreaLimit(box);
        mMapView.setMinZoomLevel(mMapView.getTileProvider().getMinimumZoomLevel());
        mMapView.setMaxZoomLevel(mMapView.getTileProvider().getMaximumZoomLevel());
        mMapView.setCenter(mMapView.getTileProvider().getCenterCoordinate());
        mMapView.setZoom(0);
    }

    private void initTrackPath() {
        // Configure the line representation.
        Paint linePaint = new Paint();
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(Color.BLUE);
        linePaint.setStrokeWidth(5);

        TrackSpeedMapOverlay trackMapOverlay = new TrackSpeedMapOverlay(track);
        trackMapOverlay.setPaint(linePaint);

        // Adds the path overlay to the mapview.
        mMapView.getOverlays().add(trackMapOverlay);
        mMapViewExpanded.getOverlays().add(trackMapOverlay);

        final BoundingBox viewBbox = trackMapOverlay.getViewBoundingBox();
        final BoundingBox scrollableLimit = trackMapOverlay.getScrollableLimitBox();

        mMapView.setScrollableAreaLimit(scrollableLimit);
        mMapView.setConstraintRegionFit(true);
        mMapView.zoomToBoundingBox(viewBbox, true);
        mMapViewExpanded.zoomToBoundingBox(viewBbox, true);
    }

    //function which expands the mapview
    private void expandMapView(Track track){
        TrackSpeedMapOverlay trackMapOverlay = new TrackSpeedMapOverlay(track);
        final BoundingBox viewBbox = trackMapOverlay.getViewBoundingBox();
        mMapViewExpanded.zoomToBoundingBox(viewBbox, true);

        animateShowView(mMapViewExpandedContainer,R.anim.translate_slide_in_top_fragment);
        animateHideView(mAppBarLayout,R.anim.translate_slide_out_top_fragment);
        animateHideView(mNestedScrollView,R.anim.translate_slide_out_bottom);
        animateHideView(mFAB,R.anim.fade_out);
    }

    //function which closes the expanded mapview
    private void closeExpandedMapView(){
        animateHideView(mMapViewExpandedContainer,R.anim.translate_slide_out_top_fragment);
        animateShowView(mAppBarLayout,R.anim.translate_slide_in_top_fragment);
        animateShowView(mNestedScrollView,R.anim.translate_slide_in_bottom_fragment);
        animateShowView(mFAB,R.anim.fade_in);
    }

    //general function to animate the view and hide it
    private void animateHideView(View view, int animResource){
       Animation animation = AnimationUtils.loadAnimation(getApplicationContext(),animResource);
       view.startAnimation(animation);
       view.setVisibility(View.GONE);
    }

    //general function to animate and show the view
    private void animateShowView(View view, int animResource){
        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(),animResource);
        view.setVisibility(View.VISIBLE);
        view.startAnimation(animation);
    }

    private void initViewValues() {
            mDistanceText.setText(String.format("%s",
                    DECIMAL_FORMATTER_TWO_DIGITS.format(((TrackStatisticsProvider) track)
                            .getDistanceOfTrack())));
            convertMillisToDate();

    }
}
