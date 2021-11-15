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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.core.EnviroCarDB;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.BindView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import lecho.lib.hellocharts.formatter.SimpleAxisValueFormatter;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.DummyVieportChangeListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.LineChartView;
import lecho.lib.hellocharts.view.PreviewLineChartView;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class TrackStatisticsActivity extends BaseInjectorActivity {
    private static final String EXTRA_TRACKID = "org.envirocar.app.extraTrackID";

    public static void createInstance(Activity activity, int trackID) {
        Intent intent = new Intent(activity, TrackStatisticsActivity.class);
        intent.putExtra(EXTRA_TRACKID, trackID);
        activity.startActivity(intent);
    }

    @Inject
    protected EnviroCarDB enviroCarDB;

    @BindView(R.id.activity_track_statistics_toolbar)
    protected Toolbar mToolbar;

    private Track mTrack;
    private PlaceholderFragment mPlaceholderFragment;

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_statistics_layout);

        int trackID = getIntent().getIntExtra(EXTRA_TRACKID, -1);
        Track.TrackId trackid = new Track.TrackId(trackID);

        enviroCarDB.getTrack(trackid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(track -> {
                    mTrack = track;
                    if (savedInstanceState == null) {
                        mPlaceholderFragment = new PlaceholderFragment(mTrack);
                        getFragmentManager().beginTransaction()
                                .add(R.id.activity_track_statistics_layout_container,
                                        mPlaceholderFragment).commit();
                    }

                    // Workaround... for fast smartphones, the menu gets manually inflated to
                    // fast such that no menu gets rendered. Therefore, the inflatation is
                    // postponed by 100 milliseconds.
                    AndroidSchedulers.mainThread().createWorker().schedule(
                            () -> inflateMenuProperties(track), 100, TimeUnit.MILLISECONDS);
                });

        // Inject all annotated views.
        ButterKnife.bind(this);

        // Initializes the Toolbar.
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.track_statistics);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        for (Measurement.PropertyKey key : Measurement.PropertyKey.values()) {
//            menu.add(key.getStringResource());
//        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // click on the home button in the toolbar.
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        for (Measurement.PropertyKey key : Measurement.PropertyKey.values()) {
            if (getString(key.getStringResource()).equals(item.getTitle())) {
                mPlaceholderFragment.generateData(key);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void inflateMenuProperties(Track track) {
        Menu menu = mToolbar.getMenu();
        menu.clear();
        if (mTrack != null && !mTrack.getMeasurements().isEmpty()) {
            for (Measurement.PropertyKey key : mTrack.getSupportedProperties()) {
                menu.add(key.getStringResource());
            }
        }

        mToolbar.postInvalidate();
    }

    public static class PlaceholderFragment extends Fragment {

        @BindView(R.id.activity_track_statistics_fragment_chart)
        protected LineChartView mChart;
        @BindView(R.id.activity_track_statistics_fragment_chart_preview)
        protected PreviewLineChartView mPreviewChart;

        private LineChartData mChartData;
        private LineChartData mPreviewChartData;

        private final Track mTrack;

        public PlaceholderFragment() {
            this(null);
        }

        @SuppressLint("ValidFragment")
        public PlaceholderFragment(Track track) {
            this.mTrack = track;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
                savedInstanceState) {
            View rootView = inflater.inflate(R.layout.activity_track_statistics_fragment,
                    container, false);

            // Inject all annotated views.
            ButterKnife.bind(this, rootView);

            if(mTrack.hasProperty(Measurement.PropertyKey.SPEED)){
                generateData(Measurement.PropertyKey.SPEED);
            }else{
                generateData(Measurement.PropertyKey.GPS_SPEED);
            }

            mChart.setZoomEnabled(false);
            mChart.setScrollEnabled(false);


            mPreviewChart.setViewportChangeListener(new DummyVieportChangeListener() {
                @Override
                public void onViewportChanged(Viewport viewport) {
                    mChart.setCurrentViewport(viewport);
                }
            });

            return rootView;
        }

        private void generateData(Measurement.PropertyKey propertyKey) {
            // Generate the PointValues for the Graph.
            List<PointValue> values = generateDistancedBasedData(propertyKey, mTrack);

            Line line = new Line(values);
            line.setColor(getResources().getColor(R.color.green_dark_cario));
            line.setHasPoints(false);

            List<Line> lines = new ArrayList<>();
            lines.add(line);

            mChartData = new LineChartData(lines);
            mChartData.setAxisXBottom(new Axis());
            mChartData.setAxisYLeft(new Axis().setHasLines(true));
            setDistanceAxis(mChartData);
            setYAxis(propertyKey, mChartData);

            mPreviewChartData = new LineChartData(mChartData);
            mPreviewChartData.getLines().get(0).setColor(ChartUtils.DEFAULT_DARKEN_COLOR);

            Axis axisXBottom = mPreviewChartData.getAxisXBottom();
            axisXBottom.setHasSeparationLine(false);
            axisXBottom.setHasTiltedLabels(true);
            axisXBottom.setTextColor(ChartUtils.DEFAULT_DARKEN_COLOR);

            mPreviewChartData.getAxisYLeft().setTextColor(ChartUtils.DEFAULT_DARKEN_COLOR);

            // Set the data in the charts.
            mChart.setLineChartData(mChartData);
            mPreviewChart.setLineChartData(mPreviewChartData);

            // set the preview extent
            previewX();
        }

        private List<PointValue> generateDistancedBasedData(Measurement.PropertyKey propertyKey,
                                                            Track track) {
            List<PointValue> values = new ArrayList<PointValue>();

            // temporary array for computing distances.
            float[] tmp = new float[1];
            float distance = 0;

            // temporary value for the last measurement
            Measurement lastMeasurement = null;

            for (Measurement m : track.getMeasurements()) {
                if (lastMeasurement != null) {
                    Location.distanceBetween(lastMeasurement.getLatitude(), lastMeasurement
                            .getLongitude(), m.getLatitude(), m.getLongitude(), tmp);
                    distance += tmp[0] / 1000f; // we need km not meters.
                }
                if (m != null && m.hasProperty(propertyKey)) {
                    values.add(new PointValue(distance, m.getProperty(propertyKey).floatValue()));
                }
                lastMeasurement = m;
            }

            return values;
        }

        private void setDistanceAxis(LineChartData data) {
            Axis distAxis = new Axis();
            distAxis.setName(getString(R.string.track_statistics_distance));
            distAxis.setTextColor(getResources().getColor(R.color.blue_dark_cario));
            distAxis.setMaxLabelChars(8);
            distAxis.setFormatter(new SimpleAxisValueFormatter()
                    .setAppendedText("km".toCharArray()));
            distAxis.setHasLines(true);
            distAxis.setHasTiltedLabels(true);
            distAxis.setTextSize(10);
            distAxis.setHasSeparationLine(false);
            data.setAxisXBottom(distAxis);
        }

        private void setYAxis(Measurement.PropertyKey key, LineChartData data) {
            Axis yAxis = new Axis();
            yAxis.setName(getString(key.getStringResource()));
            yAxis.setTextColor(getResources().getColor(R.color.blue_dark_cario));
            yAxis.setMaxLabelChars(5);
            yAxis.setHasLines(true);
            yAxis.setTextSize(10);
            yAxis.setFormatter(new SimpleAxisValueFormatter());
            yAxis.setInside(false);
            yAxis.setHasSeparationLine(false);
            data.setAxisYLeft(yAxis);
        }

        private void previewX() {
            Viewport tempViewport = new Viewport(mChart.getMaximumViewport());
            float dx = tempViewport.width() / 3;
            tempViewport.inset(dx, 0);
            mPreviewChart.setCurrentViewportWithAnimation(tempViewport);
            mPreviewChart.setZoomType(ZoomType.HORIZONTAL);
        }

        private void previewY() {
            Viewport tempViewport = new Viewport(mChart.getMaximumViewport());
            float dy = tempViewport.height() / 4;
            tempViewport.inset(0, dy);
            mPreviewChart.setCurrentViewportWithAnimation(tempViewport);
            mPreviewChart.setZoomType(ZoomType.VERTICAL);
        }
    }
}
