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
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.envirocar.app.R;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.FuelConsumptionException;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.UnsupportedFuelTypeException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.trackprocessing.TrackStatisticsProvider;
import org.envirocar.core.utils.CarUtils;

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
    private boolean isDieselEnabled;

    /**
     * Constructor.
     *  @param tracks the list of tracks to show cards for.
     *
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


    @SuppressLint("StaticFieldLeak") //The leak is handled by the scheduler
    protected void bindLocalTrackViewHolder(TrackCardViewHolder holder, Track track) {

        // First, load the track from the dataset
        String[] titleArray = getDateAndTime(track.getName());

        holder.mDateTitleTextView.setText(titleArray[0]);
        holder.mTimeTitleTextView.setText(titleArray[1]);

        // Set all the view parameters.
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
                                holder.mBeginValue.setText(DATE_FORMAT.format(new Date(track.getStartTime())));
                                holder.mEndValue.setText(DATE_FORMAT.format(new Date(track.getEndTime())));
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
    }

    /**
     * Get parsed date and time from the title
     *
     * @param title the title
     * @return the string [ ]
     */
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
     * The Track card view holder.
     */
    static class TrackCardViewHolder extends RecyclerView.ViewHolder {
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
        @BindView(R.id.popup_menu_button_layout)
        protected LinearLayout popupMenuButton;

        /**
         * Constructor.
         *
         * @param itemView the card view of the
         */
        public TrackCardViewHolder(View itemView) {
            super(itemView);
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
         * Constructor.
         *
         * @param itemView the card view of the
         */
        public RemoteTrackCardViewHolder(View itemView) {
            super(itemView);
        }
    }


    /**
     * Should be called when arrow button is clicked
     *
     * @param expandableLayout the expandable layout
     * @param buttonLayout     the button layout
     * @param completeCard     the complete card
     * @param i                the
     * @param expandState      the expand state
     */
    public void onClickArrowButton(final LinearLayout expandableLayout, final RelativeLayout buttonLayout, RelativeLayout completeCard, final int i, SparseBooleanArray expandState) {


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
