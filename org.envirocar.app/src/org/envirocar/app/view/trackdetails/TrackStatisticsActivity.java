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
package org.envirocar.app.view.trackdetails;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.envirocar.app.R;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.injection.BaseInjectorActivity;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
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
    protected DbAdapter mDBAdapter;
    @InjectView(R.id.activity_track_statistics_toolbar)
    protected Toolbar mToolbar;

    private Track mTrack;

    private PlaceholderFragment mPlaceholderFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_statistics_layout);

        int trackID = getIntent().getIntExtra(EXTRA_TRACKID, -1);
        Track.TrackId trackid = new Track.TrackId(trackID);
        mTrack = mDBAdapter.getTrack(trackid);

        // Inject all annotated views.
        ButterKnife.inject(this);

        // Initializes the Toolbar.
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Track Statistics");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            mPlaceholderFragment = new PlaceholderFragment(mTrack);
            getFragmentManager().beginTransaction()
                    .add(R.id.activity_track_statistics_layout_container,
                            mPlaceholderFragment).commit();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        for(Measurement.PropertyKey key : Measurement.PropertyKey.values()){
            menu.add(key.toString());
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // click on the home button in the toolbar.
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        for(Measurement.PropertyKey key : Measurement.PropertyKey.values()){
            if(key.toString().equals(item.getTitle())){
                mPlaceholderFragment.generateData(key);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public static class PlaceholderFragment extends Fragment {

        @InjectView(R.id.activity_track_statistics_fragment_chart)
        protected LineChartView mChart;
        @InjectView(R.id.activity_track_statistics_fragment_chart_preview)
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
            ButterKnife.inject(this, rootView);

            generateData(Measurement.PropertyKey.SPEED);

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
            List<PointValue> values = new ArrayList<PointValue>();

            List<Measurement> measurements = mTrack.getMeasurements();

            for (int i = 0; i < measurements.size(); i++) {
                Measurement m = measurements.get(i);
                if (m != null && m.hasProperty(propertyKey)) {
                    values.add(new PointValue(i, m.getProperty(propertyKey).floatValue()));
                }
            }

            Line line = new Line(values);
            line.setColor(getResources().getColor(R.color.green_dark_cario));
            line.setHasPoints(false);

            List<Line> lines = new ArrayList<Line>();
            lines.add(line);

            mChartData = new LineChartData(lines);
            mChartData.setAxisXBottom(new Axis());
            mChartData.setAxisYLeft(new Axis().setHasLines(true));

            mPreviewChartData = new LineChartData(mChartData);
            mPreviewChartData.getLines().get(0).setColor(ChartUtils.DEFAULT_DARKEN_COLOR);

            // Set the data in the charts.
            mChart.setLineChartData(mChartData);
            mPreviewChart.setLineChartData(mPreviewChartData);

            // set the preview extent
            previewX();
        }

        private void previewX(){
            Viewport tempViewport = new Viewport(mChart.getMaximumViewport());
            float dx = tempViewport.width() / 4;
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

        private void generateDefaultData() {
            int numValues = 50;

            List<PointValue> values = new ArrayList<PointValue>();
            for (int i = 0; i < numValues; ++i) {
                values.add(new PointValue(i, (float) Math.random() * 100f));
            }

            Line line = new Line(values);
            line.setColor(ChartUtils.COLOR_GREEN);
            line.setHasPoints(false);// too many values so don't draw points.

            List<Line> lines = new ArrayList<Line>();
            lines.add(line);

            mChartData = new LineChartData(lines);
            mChartData.setAxisXBottom(new Axis());
            mChartData.setAxisYLeft(new Axis().setHasLines(true));

            // prepare preview data, is better to use separate deep copy for preview chart.
            // Set color to grey to make preview area more visible.
            mPreviewChartData = new LineChartData(mChartData);
            mPreviewChartData.getLines().get(0).setColor(ChartUtils.DEFAULT_DARKEN_COLOR);
        }
    }
}
