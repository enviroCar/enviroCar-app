package org.envirocar.app.view.preferences;

import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.overlay.PathOverlay;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
import com.mapbox.mapboxsdk.views.MapView;

import org.envirocar.app.R;
import org.envirocar.app.exception.MeasurementsException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.protocol.algorithm.UnsupportedFuelTypeException;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Track;
import org.envirocar.app.view.trackdetails.TrackDetailsActivity;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @author dewall
 */
public class TrackRecyclerViewAdapter extends RecyclerView.Adapter<TrackRecyclerViewAdapter
        .TrackCardViewHolder> {
    private static final Logger LOGGER = Logger.getLogger(TrackRecyclerViewAdapter.class);

    private static final DecimalFormat DECIMAL_FORMATTER_TWO = new DecimalFormat("#.##");
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();
    private static final DateFormat UTC_DATE_FORMATTER = new SimpleDateFormat("HH:mm:ss", Locale
            .ENGLISH);

    static {
        UTC_DATE_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final List<Track> mTrackDataset;

    private Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();

    private WebSourceTileLayer mOSMSourceLayer;

    /**
     * Constructor.
     *
     * @param tracks the list of tracks to show cards for.
     */
    public TrackRecyclerViewAdapter(List<Track> tracks) {
        mTrackDataset = tracks;

        mOSMSourceLayer = new WebSourceTileLayer("openstreetmap", "http://tile" +
                ".openstreetmap.org/{z}/{x}/{y}.png");
        mOSMSourceLayer.setName("OpenStreetMap")
                .setAttribution("OpenStreetMap Contributors")
                .setMinimumZoomLevel(1)
                .setMaximumZoomLevel(18);
    }


    @Override
    public TrackRecyclerViewAdapter.TrackCardViewHolder onCreateViewHolder(
            ViewGroup parent, int viewType) {

        // First inflate the view.
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout
                .fragment_tracklist_cardlayout, parent, false);

        // then return a new view holder for the inflated view.
        return new TrackCardViewHolder(view);
    }


    @Override
    public void onBindViewHolder(final TrackRecyclerViewAdapter.TrackCardViewHolder holder,
                                 int position) {
        final Track track = mTrackDataset.get(position);

        holder.mToolbar.setTitle(track.getName());
        holder.mConsumption.setText("...");
        holder.mDistance.setText("...");
        holder.mDuration.setText("...");
        holder.mEmission.setText("...");

        // set the date
        try {
            String date = UTC_DATE_FORMATTER.format(new Date(
                    track.getDurationInMillis()));
            holder.mDuration.setText(date);
        } catch (MeasurementsException e) {
            e.printStackTrace();
        }

        // Initialize the MapView and the pathoverlay of the track
        Observable.just(true)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .map(aBoolean -> {
                    initMapView(holder);
                    initTrackPath(holder, track);
                    return true;
                })
                .subscribe(aBoolean -> LOGGER.info("MapView initialized"));

        // Set the emission
        Observable.just(true)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map(aBoolean -> DECIMAL_FORMATTER_TWO.format(track.getCO2Average()))
                .subscribe(s -> holder.mEmission.setText(s));

        // set the consumption.
        Observable.just(true)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map(aBoolean -> {
                    try {
                        return DECIMAL_FORMATTER_TWO.format(
                                track.getFuelConsumptionPerHour());
                    } catch (UnsupportedFuelTypeException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).subscribe(consumption -> holder.mConsumption.setText(consumption));

        Observable.just(true)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map(aBoolean -> DECIMAL_FORMATTER_TWO.format(track.getLengthOfTrack()) + " km")
                .subscribe(length -> holder.mDistance.setText(length));


        holder.mToolbar.inflateMenu(R.menu.menu_tracklist_cardlayout);
        holder.mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                LOGGER.info("Item clicked for track " + track.getTrackId());

                switch (item.getItemId()) {
                    case R.id.menu_tracklist_cardlayout_item_details:
                        navigateDetailsView(holder, track);
                        break;
                    case R.id.menu_tracklist_cardlayout_item_delete:
                        break;
                    case R.id.menu_tracklist_cardlayout_item_export:
                        break;
                    case R.id.menu_tracklist_cardlayout_item_upload:
                        break;
                }
                return false;
            }
        });

        holder.mInvisMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LOGGER.info("Clicked on the map. Navigate to the details activity");
                navigateDetailsView(holder, track);
            }
        });
    }

    /**
     * Inits the view transition to a {@link TrackDetailsActivity} showing the details for the
     * given track.
     *
     * @param holder the view holder holding the relevant card sub-views.
     * @param track the track to show the details for.
     */
    private void navigateDetailsView(TrackCardViewHolder holder, Track track) {
        AppCompatActivity activity = (AppCompatActivity) holder.mItemView.getContext();
        View mapTransitionView = holder.mMapView;
        int trackID = (int) track.getTrackId().getId();
        TrackDetailsActivity.navigate(activity, mapTransitionView, trackID);
    }


    @Override
    public int getItemCount() {
        return mTrackDataset.size();
    }

    /**
     * Adds a track to the dataset.
     *
     * @param track the track to insert.
     */
    public void addItem(Track track) {
        mTrackDataset.add(track);
        notifyDataSetChanged();
    }

    /**
     * Removes a track from the dataset.
     *
     * @param track the track to remove.
     */
    public void removeItem(Track track) {
        if (mTrackDataset.contains(track)) {
            mTrackDataset.remove(track);
            notifyDataSetChanged();
        }
    }

    /**
     * Initializes the MapView, its base layers and settings.
     */
    private void initMapView(TrackCardViewHolder holder) {
        // Set the openstreetmap tile layer as baselayer of the map.
        holder.mMapView.setTileSource(mOSMSourceLayer);

        // set the bounding box and min and max zoom level accordingly.
        BoundingBox box = mOSMSourceLayer.getBoundingBox();
        holder.mMapView.setScrollableAreaLimit(box);
        holder.mMapView.setMinZoomLevel(holder.mMapView.getTileProvider().getMinimumZoomLevel());
        holder.mMapView.setMaxZoomLevel(holder.mMapView.getTileProvider().getMaximumZoomLevel());
        holder.mMapView.setCenter(holder.mMapView.getTileProvider().getCenterCoordinate());
        holder.mMapView.setZoom(0);
    }

    /**
     * @param track
     */
    private void initTrackPath(TrackCardViewHolder holder, Track track) {
        // Configure the line representation.
        Paint linePaint = new Paint();
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(Color.BLUE);
        linePaint.setStrokeWidth(5);

        PathOverlay pathOverlay = new PathOverlay().setPaint(linePaint);
        List<Measurement> measurements = track.getMeasurements();

        double maxLatitude = Double.MIN_VALUE;
        double minLatitude = Double.MAX_VALUE;
        double maxLongitude = Double.MIN_VALUE;
        double minLongitude = Double.MAX_VALUE;

        // For each measurement value add the longitude and latitude coordinates as a new
        // mappoint to the overlay network. In addition, try to find out the maximum and minimum
        // lon/lat coordinates for the zoom value of the mapview.
        for (Measurement measurement : measurements) {
            double latitude = measurement.getLatitude();
            double longitude = measurement.getLongitude();
            pathOverlay.addPoint(measurement.getLatitude(), measurement.getLongitude());

            maxLatitude = Math.max(maxLatitude, latitude);
            minLatitude = Math.min(minLatitude, latitude);
            maxLongitude = Math.max(maxLongitude, longitude);
            minLongitude = Math.min(minLongitude, longitude);
        }

        // Adds the path overlay to the mapview.
        holder.mMapView.getOverlays().add(pathOverlay);

        // The bounding box of the pathoverlay.
        BoundingBox bbox = new BoundingBox(maxLatitude, maxLongitude, minLatitude, minLongitude);

        // The view bounding box of the pathoverlay
        BoundingBox viewBbox = new BoundingBox(bbox.getLatNorth() + 0.01, bbox.getLonEast()
                + 0.01, bbox.getLatSouth() - 0.01, bbox.getLonWest() - 0.01);
        holder.mMapView.zoomToBoundingBox(viewBbox, true);

        // The bounding box that limits the scrolling of the mapview.
        BoundingBox scrollableLimit = new BoundingBox(bbox.getLatNorth() + 0.05, bbox.getLonEast()
                + 0.05, bbox.getLatSouth() - 0.05, bbox.getLonWest() - 0.05);
        holder.mMapView.setScrollableAreaLimit(scrollableLimit);
    }


    /**
     *
     */
    static class TrackCardViewHolder extends RecyclerView.ViewHolder {

        protected final View mItemView;

        @InjectView(R.id.fragment_Tracklist_cardlayout_toolbar)
        protected Toolbar mToolbar;
        @InjectView(R.id.fragment_tracklist_cardlayout_consumption)
        protected TextView mConsumption;
        @InjectView(R.id.track_details_attributes_header_distance)
        protected TextView mDistance;
        @InjectView(R.id.fragment_tracklist_cardlayout_emission)
        protected TextView mEmission;
        @InjectView(R.id.track_details_attributes_header_duration)
        protected TextView mDuration;
        @InjectView(R.id.fragment_tracklist_cardlayout_map)
        protected MapView mMapView;
        @InjectView(R.id.fragment_tracklist_cardlayout_invis_mapbutton)
        protected ImageButton mInvisMapButton;


        public TrackCardViewHolder(View itemView) {
            super(itemView);
            this.mItemView = itemView;
            ButterKnife.inject(this, itemView);
        }
    }
}
