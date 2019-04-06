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
package org.envirocar.app.views.tracklist;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.jorgecastilloprz.FABProgressCircle;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
import com.mapbox.mapboxsdk.views.MapView;

import org.envirocar.app.R;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.views.trackdetails.TrackDetailsActivity;
import org.envirocar.app.views.trackdetails.TrackSpeedMapOverlay;
import org.envirocar.app.views.trackdetails.TrackStatisticsActivity;
import org.envirocar.app.views.utils.MapUtils;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.FuelConsumptionException;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.UnsupportedFuelTypeException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.trackprocessing.TrackStatisticsProvider;
import org.envirocar.core.utils.CarUtils;
import org.envirocar.storage.EnviroCarDB;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.inject.Inject;

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
    private boolean isDieselEnabled;

    /**
     * Constructor.
     *
     * @param tracks the list of tracks to show cards for.
     */
    public AbstractTrackListCardAdapter(List<Track> tracks, final OnTrackInteractionCallback
            callback, Boolean isDieselEnabled) {
        this.mTrackDataset = tracks;
        this.mTrackInteractionCallback = callback;
        this.isDieselEnabled = isDieselEnabled;
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


    @SuppressLint("StaticFieldLeak")
    protected void bindLocalTrackViewHolder(TrackCardViewHolder holder, Track track) {

        // First, load the track from the dataset
        String[] titleArray = getDateAndTime(track.getName());

        holder.mDateTitleTextView.setText(titleArray[0]);
        holder.mTimeTitleTextView.setText(titleArray[1]);

        // Initialize the mapView.
//        initMapView(holder, track);

        // Set all the view parameters. This might lead to data leak
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // Set the duration text.
                try {
                    String date = UTC_DATE_FORMATTER.format(new Date(
                            track.getDuration()));
                    final DecimalFormat DECIMAL_FORMATTER_TWO_DIGITS = new DecimalFormat("#.##");
                    // Set the tracklength parameter.
                    double distanceOfTrack = ((TrackStatisticsProvider) track).getDistanceOfTrack();
                    String tracklength = String.format("%s km", DECIMAL_FORMATTER_TWO.format(
                            distanceOfTrack));

                    mMainThreadWorker.schedule(new Action0() {
                        @Override
                        public void call() {
                            holder.mDuration.setText(date);
                            //Car details
                            holder.mCarDetails.setText(CarUtils.carToStringWithLinebreak(track.getCar()));
                            //Begin, End values
                            try {
                                holder.mBeginValue.setText(DATE_FORMAT.format(new Date(track.getStartTime())));                            holder.mEndValue.setText(DATE_FORMAT.format(new Date(track.getEndTime())));
                                holder.mEndValue.setText(DATE_FORMAT.format(new Date(track.getEndTime())));
                            } catch (NoMeasurementsException e) {
                                e.printStackTrace();
                            }
                            //Consumption and Emission
                            if (track.getCar().getFuelType() == Car.FuelType.GASOLINE ||
                                    isDieselEnabled) {
                                try {
                                    holder.mEmission.setText(String.format("%s g/km", DECIMAL_FORMATTER_TWO_DIGITS.format(
                                            ((TrackStatisticsProvider) track).getGramsPerKm())));
                                    holder.mFuelConsumption.setText(
                                            String.format("%s l/h\n%s l/100 km",
                                                    DECIMAL_FORMATTER_TWO_DIGITS.format(
                                                            ((TrackStatisticsProvider) track)
                                                                    .getFuelConsumptionPerHour()),

                                                    DECIMAL_FORMATTER_TWO_DIGITS.format(
                                                            ((TrackStatisticsProvider) track).getLiterPerHundredKm())));
                                } catch (FuelConsumptionException e) {
                                    e.printStackTrace();
                                } catch (NoMeasurementsException e) {
                                    e.printStackTrace();
                                } catch (UnsupportedFuelTypeException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                holder.mEmission.setText(R.string.track_list_details_diesel_not_supported);
                                holder.mFuelConsumption.setText(R.string.track_list_details_diesel_not_supported);
                                holder.mEmission.setTextColor(Color.RED);
                                holder.mFuelConsumption.setTextColor(Color.RED);
                            }
                            //Distance
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

        /*// if the menu is not already inflated, then..
        if (!holder.mToolbar.getMenu().hasVisibleItems()) {
            // Inflate the menu and set an appropriate OnMenuItemClickListener.
            holder.mToolbar.inflateMenu(R.menu.menu_tracklist_cardlayout);
            if (track.isRemoteTrack()) {
                holder.mToolbar.getMenu().removeItem(R.id.menu_tracklist_cardlayout_item_upload);
            }
        }*/

        /*holder.mToolbar.setOnMenuItemClickListener(item -> {
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
        });*/
    }

    protected String[] getDateAndTime(String title){
        // Two types of formats 1. Track Apr 2, 2019 9:34:53 AM 2. Track 13.05.16 16:51:56 should parse accordingly
        String[] dateTimeArray = new String[2];
        String[] titleSplit = title.split(" ");
        StringBuilder dateBuilder = new StringBuilder();
        if(Character.isLetter(titleSplit[1].charAt(0))){
            //For 1
            dateBuilder.append(titleSplit[1]).append(" ").append(titleSplit[2]).append(" ").append(titleSplit[3]); // Date
            dateTimeArray[0] = dateBuilder.toString();
            dateTimeArray[1] = titleSplit[4] + titleSplit[5];
        }else{
            //For 2
            dateTimeArray[0] = titleSplit[1];
            dateTimeArray[1] = titleSplit[2];
        }
        return dateTimeArray;
    }


    /**
     *
     */
    static class TrackCardViewHolder extends RecyclerView.ViewHolder {
        protected final View mItemView;

        /*@BindView(R.id.fragment_tracklist_cardlayout_toolbar)
        protected Toolbar mToolbar;*/

        @BindView(R.id.card_title_date)
        protected TextView mDateTitleTextView;
        @BindView(R.id.card_title_time)
        protected TextView mTimeTitleTextView;
        @BindView(R.id.expand_layout)
        protected LinearLayout expandableLayout;
        @BindView(R.id.dropdown_button)
        protected RelativeLayout buttonLayout;
        @BindView(R.id.tracklist_cardlayout)
        protected RelativeLayout completeCard;
        @BindView(R.id.map_button)
        protected Button mapButton;
        @BindView(R.id.stats_button)
        protected Button statsButton;
        @BindView(R.id.track_details_attributes_header_distance)
        protected TextView mDistance;
        @BindView(R.id.track_details_attributes_header_duration)
        protected TextView mDuration;
        @BindView(R.id.activity_track_details_attr_car_value)
        protected TextView mCarDetails;
        @BindView(R.id.activity_track_details_attr_begin_value)
        protected TextView mBeginValue;
        @BindView(R.id.activity_track_details_attr_end_value)
        protected TextView mEndValue;
        @BindView(R.id.activity_track_details_attr_consumption_value)
        protected TextView mFuelConsumption;
        @BindView(R.id.activity_track_details_attr_emission_value)
        protected TextView mEmission;
        @BindView(R.id.button_arrow)
        protected View buttonArrow;
        @BindView(R.id.button_download)
        protected View buttonDownload;
        @BindView(R.id.download_progress)
        protected ProgressBar downloadProgress;

        /**
         * Constructor.
         *
         * @param itemView the card view of the
         */
        public TrackCardViewHolder(View itemView) {
            super(itemView);
            this.mItemView = itemView;
            ButterKnife.bind(this, itemView);
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

        /**
         * Initializes the MapView, its base layers and settings.
         */
    /*protected void initMapView(TrackCardViewHolder holder, Track track) {
        // First, clear the overlays in the MapView.
        holder.mMapView.getOverlays().clear();

        // Set the openstreetmap tile layer as baselayer of the map.
        WebSourceTileLayer layer = MapUtils.getOSMTileLayer();
        holder.mMapView.setTileSource(layer);

        // set the bounding box and min and max zoom level accordingly.
        BoundingBox box = layer.getBoundingBox();
        holder.mMapView.setDiskCacheEnabled(true);
        holder.mMapView.setScrollableAreaLimit(box);
        holder.mMapView.setMinZoomLevel(holder.mMapView.getTileProvider().getMinimumZoomLevel());
        holder.mMapView.setMaxZoomLevel(holder.mMapView.getTileProvider().getMaximumZoomLevel());
//        holder.mMapView.setCenter(holder.mMapView.getTileProvider().getCenterCoordinate());
        holder.mMapView.setZoom(0);

        if (track.getMeasurements().size() > 0) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    // Configure the line representation.
                    Paint linePaint = new Paint();
                    linePaint.setStyle(Paint.Style.STROKE);
                    linePaint.setColor(Color.BLUE);
                    linePaint.setStrokeWidth(5);

                    TrackSpeedMapOverlay trackMapOverlay = new TrackSpeedMapOverlay(track);
                    trackMapOverlay.setPaint(linePaint);

                    final BoundingBox bbox = trackMapOverlay.getTrackBoundingBox();
                    final BoundingBox viewBbox = trackMapOverlay.getViewBoundingBox();
                    final BoundingBox scrollableLimit = trackMapOverlay.getScrollableLimitBox();

                    LOG.warn("trying to zoom to track bbox");
                    mMainThreadWorker.schedule(new Action0() {
                        @Override
                        public void call() {
                            holder.mMapView.getOverlays().add(trackMapOverlay);
                            LOG.warn("bbox " + bbox);
                            // Set the computed parameters on the main thread.
                            holder.mMapView.setScrollableAreaLimit(scrollableLimit);
                            LOG.warn("scrollable limit " + scrollableLimit.toString());
                            holder.mMapView.setConstraintRegionFit(true);
                            holder.mMapView.zoomToBoundingBox(viewBbox, true);
                            LOG.warn("zooming to " + viewBbox.toString());
                        }
                    });
                    return null;
                }
            }.execute();
        }
    }*/

        /*@BindView(R.id.fragment_tracklist_cardlayout_remote_progresscircle)
        protected FABProgressCircle mProgressCircle;
        @BindView(R.id.fragment_tracklist_cardlayout_remote_downloadfab)
        protected FloatingActionButton mDownloadButton;
        @BindView(R.id.fragment_tracklist_cardlayout_downloading_notification)
        protected TextView mDownloadNotification;*/

        /**
         * Constructor.
         *
         * @param itemView the card view of the
         */
        public RemoteTrackCardViewHolder(View itemView) {
            super(itemView);
        }
    }

    public void onClickButton(final LinearLayout expandableLayout, final RelativeLayout buttonLayout, RelativeLayout completeCard, final int i, SparseBooleanArray expandState) {


        //Simply set View to Gone if not expanded
        //Not necessary but I put simple rotation on button layout
        if (expandableLayout.getVisibility() == View.VISIBLE) {
            createRotateAnimator(buttonLayout, 180f, 0f).start();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                //Transition
                final ChangeBounds transition = new ChangeBounds();
                transition.setDuration(300); // Sets a duration of 600 milliseconds

                TransitionManager.beginDelayedTransition(completeCard, transition);
            }

            expandableLayout.setVisibility(View.GONE);
            expandState.put(i, false);
        } else {
            createRotateAnimator(buttonLayout, 0f, 180f).start();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                //Transition
                final ChangeBounds transition = new ChangeBounds();
                transition.setDuration(300); // Sets a duration of 600 milliseconds

                TransitionManager.beginDelayedTransition(completeCard, transition);
            }

            expandableLayout.setVisibility(View.VISIBLE);
            expandState.put(i, true);
        }
    }

    //Code to rotate button
    public ObjectAnimator createRotateAnimator(final View target, final float from, final float to) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(target, "rotation", from, to);
        animator.setDuration(300);
        animator.setInterpolator(new LinearInterpolator());
        return animator;
    }
}
