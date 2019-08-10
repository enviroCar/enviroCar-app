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

import androidx.constraintlayout.widget.Guideline;

import androidx.annotation.NonNull;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.jorgecastilloprz.FABProgressCircle;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import org.envirocar.app.R;
import org.envirocar.app.views.trackdetails.TrackMapLayer;
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
                .TrackCardViewHolder> extends RecyclerView.Adapter<E> implements AbstractTrackListCardFragment.GuidelineInterface {
    private static final Logger LOG = Logger.getLogger(AbstractTrackListCardAdapter.class);

    protected static final DecimalFormat DECIMAL_FORMATTER_TWO = new DecimalFormat("#.##");
    protected static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("##");
    protected static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();
    protected static final DateFormat UTC_DATE_FORMATTER = new SimpleDateFormat("HH:mm:ss", Locale
            .ENGLISH);

    static {
        UTC_DATE_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    protected final List<Track> mTrackDataset;

    protected Boolean mvVisible;

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

    String convertMillisToDate(Long timeInMillis){
        long diffSeconds = timeInMillis / 1000 % 60;
        long diffMinutes = timeInMillis / (60 * 1000) % 60;
        long diffHours = timeInMillis / (60 * 60 * 1000) % 24;
        long diffDays = timeInMillis / (24 * 60 * 60 * 1000);
        StringBuilder stringBuilder = new StringBuilder();
        if(diffDays != 0) {
            stringBuilder.append(diffDays);
            stringBuilder.append(":");
            if (diffHours > 1) {
                stringBuilder.append(DECIMAL_FORMATTER.format(diffHours));
            }
            stringBuilder.append("D");
        }
        else {
            if (diffHours != 0) {
                stringBuilder.append(diffHours);
                if (diffMinutes != 0){
                    stringBuilder.append(":");
                    stringBuilder.append(DECIMAL_FORMATTER.format(diffMinutes));
                }
                stringBuilder.append("H");
            }
            else {
                if (diffMinutes!=0) {
                    stringBuilder.append(diffMinutes);
                    if(diffSeconds!=0){
                        stringBuilder.append(":");
                        stringBuilder.append(DECIMAL_FORMATTER.format(diffSeconds));
                    }
                    stringBuilder.append("M");
                }
                else{
                    stringBuilder.append(diffSeconds);
                    stringBuilder.append("S");

                }
            }

        }
        return stringBuilder.toString();
    }

    public void setGuideline(Boolean bool){
        mvVisible = bool;
    }

    protected void bindTrackViewHolder(TrackCardViewHolder holder, Track track, Boolean isDownloadedTrack) {
        holder.mDistance.setText("...");
        holder.mDuration.setText("...");
        LOG.info("bindLocalTrackViewHolder()");
        holder.mDurationAdd.setText("H");
        holder.mDate.setText("...");
        holder.mTime.setText("...");
        holder.mTimeAdd.setText("PM");
        if(!isDownloadedTrack)
        {
            holder.guideline.setGuidelinePercent(0.37f);
            holder.mDistance.setVisibility(View.GONE);
            holder.distanceBox.setVisibility(View.GONE);
            holder.mDistanceImg.setVisibility(View.GONE);
            holder.mDuration.setVisibility(View.GONE);
            holder.mDurationAdd.setVisibility(View.GONE);
            holder.mDurationImg.setVisibility(View.GONE);
            holder.mCarName.setVisibility(View.GONE);
        } else {
            if(mvVisible)
                holder.guideline.setGuidelinePercent(0.37f);
            else
                holder.guideline.setGuidelinePercent(0f);
            holder.mDistance.setVisibility(View.VISIBLE);
            holder.distanceBox.setVisibility(View.VISIBLE);
            holder.mDistanceImg.setVisibility(View.VISIBLE);
            holder.mDuration.setVisibility(View.VISIBLE);
            holder.mDurationAdd.setVisibility(View.VISIBLE);
            holder.mDurationImg.setVisibility(View.VISIBLE);
            holder.mCarName.setVisibility(View.VISIBLE);
        }

        // First, load the track from the dataset
        //holder.mTitleTextView.setText(track.getName());
        // Initialize the mapView.
        if(isDownloadedTrack)
        {
            holder.mMapView.setVisibility(View.VISIBLE);
            initMapView(holder, track);
        }
        else
            holder.mMapView.setVisibility(View.GONE);
        // Set all the view parameters.
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                try {
                    //Set Track Header and TimeImg
                    Date trackDate;
                    SimpleDateFormat formatter;
                    if(isDownloadedTrack)
                        trackDate = new Date(track.getStartTime());
                    else
                    {
                        formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                        try
                        {
                            trackDate = formatter.parse(track.getBegin());
                        }catch (Exception e){
                            LOG.error("Unable to parse date", e);
                            trackDate = new Date();
                        }
                    }
                    formatter = new SimpleDateFormat("HH", Locale.getDefault());
                    Integer hh = Integer.parseInt(formatter.format(trackDate));
                    mMainThreadWorker.schedule(new Action0() {
                        @Override
                        public void call() {
                            if(hh < 4 || hh > 19) {
                                holder.mTitleTextView.setText("Your Night Track");
                                holder.mTimeImg.setImageResource(R.drawable.night);
                            }
                            else if(hh >= 4 && hh < 9) {
                                holder.mTitleTextView.setText("Your Morning Track");
                                holder.mTimeImg.setImageResource(R.drawable.morning);
                            }
                            else if(hh > 9 && hh < 15) {
                                holder.mTitleTextView.setText("Your Afternoon Track");
                                holder.mTimeImg.setImageResource(R.drawable.afternoon);
                            }
                            else {
                                holder.mTitleTextView.setText("Your Evening Track");
                                holder.mTimeImg.setImageResource(R.drawable.evening);
                            }
                        }
                    });

                    // Set Car Name
                    String carName = track.getCar().getModel();
                    mMainThreadWorker.schedule(new Action0() {
                        @Override
                        public void call() {
                            holder.mCarName.setText(carName);
                        }
                    });

                    //Set Date and Time of Track
                    String trackDateS = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(trackDate);
                    String trackTimeS = new SimpleDateFormat("KK:mm", Locale.getDefault()).format(trackDate);
                    String trackTimeS1 = new SimpleDateFormat("a", Locale.getDefault()).format(trackDate);
                    mMainThreadWorker.schedule(new Action0() {
                        @Override
                        public void call() {
                            holder.mDate.setText(trackDateS);
                            holder.mTime.setText(trackTimeS);
                            holder.mTimeAdd.setText(trackTimeS1);
                        }
                    });

                    if(isDownloadedTrack)
                    {
                        // Set the duration text.
                        String temp = convertMillisToDate(track.getTimeInMillis());
                        mMainThreadWorker.schedule(new Action0() {
                            @Override
                            public void call() {
                                if(temp != "" || temp !=null)
                                {
                                    String t1 = temp.substring(0, temp.length()-1);
                                    String t2 = temp.substring(temp.length()-1);
                                    holder.mDuration.setText(t1);
                                    holder.mDurationAdd.setText(t2);
                                }
                            }
                        });

                        // Set the tracklength parameter.
                        double distanceOfTrack = ((TrackStatisticsProvider) track).getDistanceOfTrack();
                        String tracklength = String.format("%s", DECIMAL_FORMATTER_TWO.format(
                                distanceOfTrack));
                        mMainThreadWorker.schedule(new Action0() {
                            @Override
                            public void call() {
                                holder.mDistance.setText(tracklength);
                            }
                        });
                    }

                } catch (NoMeasurementsException e) {
                    LOG.warn(e.getMessage(), e);
                    mMainThreadWorker.schedule(new Action0() {
                        @Override
                        public void call() {
                            holder.mDistance.setText("0");
                            holder.mDuration.setText("0:00");
                            holder.mDate.setText("Jan 1, 2019");
                            holder.mTime.setText("12:00");
                            holder.mTimeAdd.setText("AM");
                            holder.mCarName.setText("NA");
                            holder.mTitleTextView.setText("Your Track");
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
        @BindView(R.id.track_details_attributes_header_title)
        protected TextView mTitleTextView;
        @BindView(R.id.track_details_attributes_header_car)
        protected TextView mCarName;
        @BindView(R.id.guideline3)
        protected Guideline guideline;
        @BindView(R.id.track_details_attributes_header_date)
        protected TextView mDate;
        @BindView(R.id.track_details_attributes_header_time)
        protected TextView mTime;
        @BindView(R.id.track_details_attributes_header_time_add)
        protected TextView mTimeAdd;
        @BindView(R.id.track_details_attributes_image_time)
        protected ImageView mTimeImg;
        @BindView(R.id.track_details_attributes_header_distance)
        protected TextView mDistance;
        @BindView(R.id.track_details_attributes_image_distance)
        protected ImageView mDistanceImg;
        @BindView(R.id.distanceBox)
        protected LinearLayout distanceBox;
        @BindView(R.id.track_details_attributes_header_duration)
        protected TextView mDuration;
        @BindView(R.id.track_details_attributes_header_duration_add)
        protected TextView mDurationAdd;
        @BindView(R.id.track_details_attributes_image_duration)
        protected ImageView mDurationImg;
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
