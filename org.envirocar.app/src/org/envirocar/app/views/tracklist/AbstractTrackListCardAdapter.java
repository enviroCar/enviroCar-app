/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.tracklist;
import android.os.AsyncTask;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.jorgecastilloprz.FABProgressCircle;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.sources.TileSet;

import org.envirocar.app.R;
import org.envirocar.app.views.trackdetails.TrackMapLayer;
import org.envirocar.app.views.utils.MapUtils;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.trackprocessing.statistics.TrackStatisticsProvider;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public abstract class AbstractTrackListCardAdapter<E extends
        AbstractTrackListCardAdapter
                .TrackCardViewHolder> extends RecyclerView.Adapter<E> {
    private static final Logger LOG = Logger.getLogger(AbstractTrackListCardAdapter.class);

    protected static final DecimalFormat DECIMAL_FORMATTER_TWO = new DecimalFormat("#.##");
    protected static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();
    protected static final DateFormat UTC_DATE_FORMATTER = new SimpleDateFormat("HH:mm:ss", Locale
            .ENGLISH);

    static {
        UTC_DATE_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    protected final List<Track> mTrackDataset;
    protected Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    protected final OnTrackInteractionCallback mTrackInteractionCallback;

    /**
     * Constructor.
     *
     * @param tracks the list of tracks to show cards for.
     */
    public AbstractTrackListCardAdapter(List<Track> tracks, final OnTrackInteractionCallback
            callback) {
        this.mTrackDataset = tracks;
        this.mTrackInteractionCallback = callback;
    }

    @Override
    public int getItemCount() {
        return mTrackDataset.size();
    }

    @Override
    public long getItemId(int position) {
        return mTrackDataset.get(position).getTrackID().getId();
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


    protected void bindLocalTrackViewHolder(TrackCardViewHolder holder, Track track) {
        holder.mDistance.setText("...");
        holder.mDuration.setText("...");
        LOG.info("bindLocalTrackViewHolder()");
        // First, load the track from the dataset
        holder.mTitleTextView.setText(track.getName());

        // Initialize the mapView.
        initMapView(holder, track);

        // Set all the view parameters.
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // Set the duration text.
                try {
                    String date = UTC_DATE_FORMATTER.format(new Date(
                            track.getDuration()));
                    mMainThreadWorker.schedule(new Action0() {
                        @Override
                        public void call() {
                            holder.mDuration.setText(date);
                        }
                    });

                    // Set the tracklength parameter.
                    double distanceOfTrack = ((TrackStatisticsProvider) track).getDistanceOfTrack();
                    String tracklength = String.format("%s km", DECIMAL_FORMATTER_TWO.format(
                            distanceOfTrack));
                    mMainThreadWorker.schedule(new Action0() {
                        @Override
                        public void call() {
                            holder.mDistance.setText(tracklength);
                        }
                    });

                } catch (NoMeasurementsException e) {
                    LOG.warn(e.getMessage(), e);
                    mMainThreadWorker.schedule(new Action0() {
                        @Override
                        public void call() {
                            holder.mDistance.setText("0 km");
                            holder.mDuration.setText("0:00");
                        }
                    });
                }

                return null;
            }
        }.execute();

        // if the menu is not already inflated, then..
        if (!holder.mToolbar.getMenu().hasVisibleItems()) {
            // Inflate the menu and set an appropriate OnMenuItemClickListener.
            holder.mToolbar.inflateMenu(R.menu.menu_tracklist_cardlayout);
            if (track.isRemoteTrack()) {
                holder.mToolbar.getMenu().removeItem(R.id.menu_tracklist_cardlayout_item_upload);
            }
        }

        holder.mToolbar.setOnMenuItemClickListener(item -> {
            LOG.info("Item clicked for track " + track.getTrackID());

            switch (item.getItemId()) {
                case R.id.menu_tracklist_cardlayout_item_details:
                    mTrackInteractionCallback.onTrackDetailsClicked(track, holder.mMapView);
                    break;
                case R.id.menu_tracklist_cardlayout_item_delete:
                    mTrackInteractionCallback.onDeleteTrackClicked(track);
                    break;
                case R.id.menu_tracklist_cardlayout_item_export:
                    mTrackInteractionCallback.onExportTrackClicked(track);
                    break;
                case R.id.menu_tracklist_cardlayout_item_upload:
                    mTrackInteractionCallback.onUploadTrackClicked(track);
                    break;
            }
            return false;
        });

        // Initialize the OnClickListener for the invisible button that is overlaid
        // over the map view.
        holder.mInvisMapButton.setOnClickListener(v -> {
            LOG.info("Clicked on the map. Navigate to the details activity");
            mTrackInteractionCallback.onTrackDetailsClicked(track, holder.mMapView);
        });
    }


    /**
     * Initializes the MapView, its base layers and settings.
     */
    protected void initMapView(TrackCardViewHolder holder, Track track) {
        // First, clear the overlays in the MapView.
        LOG.info("initMapView()");
        TrackMapLayer trackMapOverlay = new TrackMapLayer(track);
        final LatLngBounds viewBbox = trackMapOverlay.getViewBoundingBox();
        TileSet layer = MapUtils.getOSMTileLayer();
        holder.mMapView.addOnDidFailLoadingMapListener(holder.failLoadingMapListener);
        holder.mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap tep) {
                LOG.info("onMapReady()");
                tep.getUiSettings().setLogoEnabled(false);
                tep.getUiSettings().setAttributionEnabled(false);
                tep.setStyle(new Style.Builder().fromUrl("https://api.maptiler.com/maps/basic/style.json?key=YJCrA2NeKXX45f8pOV6c "), new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        LOG.info("onStyleLoaded()");
                        style.addSource(trackMapOverlay.getGeoJsonSource());
                        style.addLayer(trackMapOverlay.getLineLayer());
                        tep.moveCamera(CameraUpdateFactory.newLatLngBounds(viewBbox, 50));
                    }
                });

            }
        });
    }

    @Override
    public void onViewAttachedToWindow(@NonNull E holder) {
        super.onViewAttachedToWindow(holder);
        holder.mMapView.onStart();
        holder.mMapView.onResume();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull E holder) {
        super.onViewDetachedFromWindow(holder);
        holder.mMapView.onPause();
        holder.mMapView.onStop();
    }

    /**
     *
     */
    static class TrackCardViewHolder extends RecyclerView.ViewHolder {

        protected final View mItemView;

        @BindView(R.id.fragment_tracklist_cardlayout_toolbar)
        protected Toolbar mToolbar;
        @BindView(R.id.fragment_tracklist_cardlayout_toolbar_title)
        protected TextView mTitleTextView;
        @BindView(R.id.fragment_tracklist_cardlayout_content)
        protected View mContentView;
        @BindView(R.id.track_details_attributes_header_distance)
        protected TextView mDistance;
        @BindView(R.id.track_details_attributes_header_duration)
        protected TextView mDuration;
        protected MapboxMap mapboxMap;
        @BindView(R.id.fragment_tracklist_cardlayout_map)
        protected MapView mMapView;
        @BindView(R.id.fragment_tracklist_cardlayout_invis_mapbutton)
        protected ImageButton mInvisMapButton;

        protected MapView.OnDidFailLoadingMapListener failLoadingMapListener;

        /**
         * Constructor.
         *
         * @param itemView the card view of the
         */
        public TrackCardViewHolder(View itemView) {
            super(itemView);
            this.mItemView = itemView;
            ButterKnife.bind(this, itemView);
            failLoadingMapListener = new MapView.OnDidFailLoadingMapListener() {
                @Override
                public void onDidFailLoadingMap(String errorMessage) {
                    LOG.info("Map loading failed : " + errorMessage);
                }
            };
        }
    }

    /**
     * Default view holder for standard local and not uploaded tracks.
     */
    static class LocalTrackCardViewHolder extends TrackCardViewHolder {

        /**
         * Constructor.
         *
         * @param itemView
         */
        public LocalTrackCardViewHolder(View itemView) {
            super(itemView);
        }
    }

    /**
     * Remote track view holder that only contains the views that can be filled with information
     * of a remote track list. (i.e. users/{user}/tracks)
     */
    static class RemoteTrackCardViewHolder extends TrackCardViewHolder {

        @BindView(R.id.fragment_tracklist_cardlayout_remote_progresscircle)
        protected FABProgressCircle mProgressCircle;
        @BindView(R.id.fragment_tracklist_cardlayout_remote_downloadfab)
        protected FloatingActionButton mDownloadButton;
        @BindView(R.id.fragment_tracklist_cardlayout_downloading_notification)
        protected TextView mDownloadNotification;

        /**
         * Constructor.
         *
         * @param itemView the card view of the
         */
        public RemoteTrackCardViewHolder(View itemView) {
            super(itemView);
        }
    }
}
