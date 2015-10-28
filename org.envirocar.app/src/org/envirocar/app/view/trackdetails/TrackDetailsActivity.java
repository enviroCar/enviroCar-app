package org.envirocar.app.view.trackdetails;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.Toolbar;
import android.transition.Slide;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
import com.mapbox.mapboxsdk.views.MapView;

import org.envirocar.app.R;
import org.envirocar.app.view.utils.MapUtils;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.FuelConsumptionException;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.UnsupportedFuelTypeException;
import org.envirocar.core.injection.BaseInjectorActivity;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.trackprocessing.TrackStatisticsProvider;
import org.envirocar.storage.EnviroCarDB;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
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

    @InjectView(R.id.activity_track_details_fab)
    protected FloatingActionButton mFAB;
    @InjectView(R.id.activity_track_details_header_map)
    protected MapView mMapView;
    @InjectView(R.id.activity_track_details_header_toolbar)
    protected Toolbar mToolbar;
    @InjectView(R.id.activity_track_details_attr_description_value)
    protected TextView mDescriptionText;
    @InjectView(R.id.track_details_attributes_header_duration)
    protected TextView mDurationText;
    @InjectView(R.id.track_details_attributes_header_distance)
    protected TextView mDistanceText;
    @InjectView(R.id.activity_track_details_attr_begin_value)
    protected TextView mBeginText;
    @InjectView(R.id.activity_track_details_attr_end_value)
    protected TextView mEndText;
    @InjectView(R.id.activity_track_details_attr_car_value)
    protected TextView mCarText;
    @InjectView(R.id.activity_track_details_attr_emission_value)
    protected TextView mEmissionText;
    @InjectView(R.id.activity_track_details_attr_consumption_value)
    protected TextView mConsumptionText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActivityTransition();
        setContentView(R.layout.activity_track_details_layout2);

        // Inject all annotated views.
        ButterKnife.inject(this);

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
                .toBlocking()
                .first();

        String itemTitle = track.getName();
        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById
                (R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle(itemTitle);
        collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color
                .transparent));
        collapsingToolbarLayout.setStatusBarScrimColor(getResources().getColor(android.R.color
                .transparent));

        TextView title = (TextView) findViewById(R.id.title);
        title.setText(itemTitle);

        // Initialize the mapview and the trackpath
        initMapView();
        initTrackPath(track);
        initViewValues(track);

        updateStatusBarColor();

        mFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TrackStatisticsActivity.createInstance(TrackDetailsActivity.this, mTrackID);
            }
        });
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

        // set the bounding box and min and max zoom level accordingly.
        BoundingBox box = source.getBoundingBox();
        mMapView.setScrollableAreaLimit(box);
        mMapView.setMinZoomLevel(mMapView.getTileProvider().getMinimumZoomLevel());
        mMapView.setMaxZoomLevel(mMapView.getTileProvider().getMaximumZoomLevel());
        mMapView.setCenter(mMapView.getTileProvider().getCenterCoordinate());
        mMapView.setZoom(0);
    }

    /**
     * @param track
     */
    private void initTrackPath(Track track) {
        // Configure the line representation.
        Paint linePaint = new Paint();
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(Color.BLUE);
        linePaint.setStrokeWidth(5);

        TrackSpeedMapOverlay trackMapOverlay = new TrackSpeedMapOverlay(track);
        trackMapOverlay.setPaint(linePaint);

        // Adds the path overlay to the mapview.
        mMapView.getOverlays().add(trackMapOverlay);

        final BoundingBox viewBbox = trackMapOverlay.getViewBoundingBox();
        final BoundingBox scrollableLimit = trackMapOverlay.getScrollableLimitBox();

        mMapView.setScrollableAreaLimit(scrollableLimit);
        mMapView.setConstraintRegionFit(true);
        mMapView.zoomToBoundingBox(viewBbox, true);

    }

    private void initViewValues(Track track) {
        try {
            final String text = UTC_DATE_FORMATTER.format(new Date(
                    track.getDuration()));
            mDistanceText.setText(String.format("%s km",
                    DECIMAL_FORMATTER_TWO_DIGITS.format(((TrackStatisticsProvider) track)
                            .getDistanceOfTrack())));
            mDurationText.setText(text);

            mDescriptionText.setText(track.getDescription());
            mCarText.setText(track.getCar().toString());
            mBeginText.setText(DATE_FORMAT.format(new Date(track.getStartTime())));
            mEndText.setText(DATE_FORMAT.format(new Date(track.getEndTime())));

            mEmissionText.setText(DECIMAL_FORMATTER_TWO_DIGITS.format(
                    ((TrackStatisticsProvider) track).getGramsPerKm()) + " g/km");
            mConsumptionText.setText(
                    String.format("%s l/h, %s l/100km",
                            DECIMAL_FORMATTER_TWO_DIGITS.format(
                                    ((TrackStatisticsProvider) track).getFuelConsumptionPerHour()),
                            DECIMAL_FORMATTER_TWO_DIGITS.format(
                                    ((TrackStatisticsProvider) track).getLiterPerHundredKm())));
        } catch (FuelConsumptionException e) {
            e.printStackTrace();
        } catch (NoMeasurementsException e) {
            e.printStackTrace();
        } catch (UnsupportedFuelTypeException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Object> getInjectionModules() {
        return new ArrayList<>();
    }

}
