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
package org.envirocar.app.views.tracklist;

import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jorgecastilloprz.FABProgressCircle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import org.envirocar.app.R;
import org.envirocar.app.databinding.FragmentTracklistCardlayoutBinding;
import org.envirocar.app.databinding.FragmentTracklistCardlayoutRemoteBinding;
import org.envirocar.app.views.trackdetails.TrackMapLayer;
import org.envirocar.core.entity.Track;
import org.envirocar.core.logging.Logger;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;

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
        int pos = mTrackDataset.indexOf(track);
        notifyItemInserted(pos);
    }

    /**
     * Removes a track from the dataset.
     *
     * @param track the track to remove.
     */
    public void removeItem(Track track) {
        if (mTrackDataset.contains(track)) {
            mTrackDataset.remove(track);
            int pos = mTrackDataset.indexOf(track);
            notifyItemRemoved(pos);
        }
    }


    protected void bindLocalTrackViewHolder(TrackCardViewHolder holder, Track track) {
        holder.getDistance().setText("...");
        holder.getDuration().setText("...");
        LOG.info("bindLocalTrackViewHolder()");
        // First, load the track from the dataset
        holder.getTitleTextView().setText(track.getName());

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
                    mMainThreadWorker.schedule(() -> holder.getDuration().setText(date));

                    // Set the tracklength parameter.

                    double distanceOfTrack = track.getLength();
                    String tracklength = String.format("%s km", DECIMAL_FORMATTER_TWO.format(
                            distanceOfTrack));
                    mMainThreadWorker.schedule(() -> holder.getDistance().setText(tracklength));

                } catch (Exception e) {
                    LOG.warn(e.getMessage(), e);
                    mMainThreadWorker.schedule(() -> {
                        holder.getDistance().setText("0 km");
                        holder.getDuration().setText("0:00");
                    });
                }

                return null;
            }
        }.execute();

        // if the menu is not already inflated, then..
        if (!holder.getToolbar().getMenu().hasVisibleItems()) {
            // Inflate the menu and set an appropriate OnMenuItemClickListener.
            holder.getToolbar().inflateMenu(R.menu.menu_tracklist_cardlayout);
            if (track.isRemoteTrack()) {
                holder.getToolbar().getMenu().removeItem(R.id.menu_tracklist_cardlayout_item_upload);
            }
        }

        holder.getToolbar().setOnMenuItemClickListener(item -> {
            LOG.info("Item clicked for track " + track.getTrackID());

            if (item.getItemId() == R.id.menu_tracklist_cardlayout_item_details) {
                mTrackInteractionCallback.onTrackDetailsClicked(track, holder.getMapView());
            } else if (item.getItemId() == R.id.menu_tracklist_cardlayout_item_delete) {
                mTrackInteractionCallback.onDeleteTrackClicked(track);
            } else if (item.getItemId() == R.id.menu_tracklist_cardlayout_item_share) {
                mTrackInteractionCallback.onShareTrackClicked(track);
            } else if (item.getItemId() == R.id.menu_tracklist_cardlayout_item_upload) {
                mTrackInteractionCallback.onUploadTrackClicked(track);
            }

            return false;
        });

        // Initialize the OnClickListener for the invisible button that is overlaid
        // over the map view.
        holder.getInvisMapButton().setOnClickListener(v -> {
            LOG.info("Clicked on the map. Navigate to the details activity");
            mTrackInteractionCallback.onTrackDetailsClicked(track, holder.getMapView());
        });

        holder.getCardViewLayout().setOnLongClickListener(view -> {
            mTrackInteractionCallback.onLongPressedTrack(track);
            return true;
        });

        holder.getInvisMapButton().setOnLongClickListener(view -> {
            mTrackInteractionCallback.onLongPressedTrack(track);
            return true;
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
        holder.getMapView().addOnDidFailLoadingMapListener(holder.failLoadingMapListener);
        holder.getMapView().getMapAsync(new OnMapReadyCallback() {
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
        holder.getMapView().onStart();
        holder.getMapView().onResume();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull E holder) {
        super.onViewDetachedFromWindow(holder);
        holder.getMapView().onPause();
        holder.getMapView().onStop();
    }

//    private void initRouteCoordinates(Track track) {
//        // Create a list to store our line coordinates.
//        routeCoordinates.clear();
//        List<Measurement> temp = track.getMeasurements();
//        for (Measurement measurement : temp) {
//            routeCoordinates.add(Point.fromLngLat(measurement.getLongitude(), measurement.getLatitude()));
//        }
//
//        latLngs.clear();
//        for (int i = 0; i < routeCoordinates.size(); ++i) {
//            latLngs.add(new LatLng(routeCoordinates.get(i).latitude(), routeCoordinates.get(i).longitude()));
//        }
//
//        if (latLngs.size() == 1) {
//            LatLng latLng = latLngs.get(0);
//            mViewBoundingBox = LatLngBounds.from(
//                    latLng.getLatitude() + 0.01,
//                    latLng.getLongitude() + 0.01,
//                    latLng.getLatitude() - 0.01,
//                    latLng.getLongitude() - 0.01);
//        } else {
//            mTrackBoundingBox = new LatLngBounds.Builder()
//                    .includes(latLngs)
//                    .build();
//
//            double latRatio = Math.max(mTrackBoundingBox.getLatitudeSpan() / 10.0, 0.01);
//            double lngRatio = Math.max(mTrackBoundingBox.getLongitudeSpan() / 10.0, 0.01);
//
//            // The view bounding box of the pathoverlay
//            mViewBoundingBox = LatLngBounds.from(
//                    mTrackBoundingBox.getLatNorth() + latRatio,
//                    mTrackBoundingBox.getLonEast() + lngRatio,
//                    mTrackBoundingBox.getLatSouth() - latRatio,
//                    mTrackBoundingBox.getLonWest() - lngRatio);
//        }
//
//    }

    /**
     *
     */
    static abstract class TrackCardViewHolder extends RecyclerView.ViewHolder {
        protected MapView.OnDidFailLoadingMapListener failLoadingMapListener;

        abstract Toolbar getToolbar();
        abstract TextView getTitleTextView();
        abstract View getContentView();
        abstract TextView getDistance();
        abstract TextView getDuration();
        abstract MapView getMapView();
        abstract ImageButton getInvisMapButton();
        abstract LinearLayout getCardViewLayout();

        public TrackCardViewHolder(View itemView) {
            super(itemView);
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
        FragmentTracklistCardlayoutBinding binding;

        @Override
        Toolbar getToolbar() {
            return binding.fragmentTracklistCardlayoutToolbar;
        }

        @Override
        TextView getTitleTextView() {
            return binding.fragmentTracklistCardlayoutToolbarTitle;
        }

        @Override
        View getContentView() {
            return binding.fragmentTracklistCardlayoutContent.fragmentTracklistCardlayoutContent;
        }

        @Override
        TextView getDistance() {
            return binding.fragmentTracklistCardlayoutContent.trackDetailsAttributesHeaderDistance;
        }

        @Override
        TextView getDuration() {
            return binding.fragmentTracklistCardlayoutContent.trackDetailsAttributesHeaderDuration;
        }

        @Override
        MapView getMapView() {
            return binding.fragmentTracklistCardlayoutContent.fragmentTracklistCardlayoutMap;
        }

        @Override
        ImageButton getInvisMapButton() {
            return binding.fragmentTracklistCardlayoutContent.fragmentTracklistCardlayoutInvisMapbutton;
        }

        @Override
        LinearLayout getCardViewLayout() {
            return binding.fragmentLayoutCardView;
        }

        public LocalTrackCardViewHolder(FragmentTracklistCardlayoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    /**
     * Remote track view holder that only contains the views that can be filled with information
     * of a remote track list. (i.e. users/{getUserStatistic}/tracks)
     */
    static class RemoteTrackCardViewHolder extends TrackCardViewHolder {
        FragmentTracklistCardlayoutRemoteBinding binding;

        @Override
        Toolbar getToolbar() {
            return binding.fragmentTracklistCardlayoutToolbar;
        }

        @Override
        TextView getTitleTextView() {
            return binding.fragmentTracklistCardlayoutToolbarTitle;
        }

        @Override
        View getContentView() {
            return binding.fragmentTracklistCardlayoutContent.fragmentTracklistCardlayoutContent;
        }

        @Override
        TextView getDistance() {
            return binding.fragmentTracklistCardlayoutContent.trackDetailsAttributesHeaderDistance;
        }

        @Override
        TextView getDuration() {
            return binding.fragmentTracklistCardlayoutContent.trackDetailsAttributesHeaderDuration;
        }

        @Override
        MapView getMapView() {
            return binding.fragmentTracklistCardlayoutContent.fragmentTracklistCardlayoutMap;
        }

        @Override
        ImageButton getInvisMapButton() {
            return binding.fragmentTracklistCardlayoutContent.fragmentTracklistCardlayoutInvisMapbutton;
        }

        @Override
        LinearLayout getCardViewLayout() {
            return binding.fragmentLayoutCardView;
        }

        FABProgressCircle getProgressCircle() {
            return binding.fragmentTracklistCardlayoutRemoteProgresscircle;
        }

        FloatingActionButton getDownloadButton() {
            return binding.fragmentTracklistCardlayoutRemoteDownloadfab;
        }

        TextView getDownloadNotification() {
            return binding.fragmentTracklistCardlayoutDownloadingNotification;
        }

        public RemoteTrackCardViewHolder(FragmentTracklistCardlayoutRemoteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
