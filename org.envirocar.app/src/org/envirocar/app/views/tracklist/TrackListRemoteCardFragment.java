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
package org.envirocar.app.views.tracklist;

import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.ViewModelProviders;
import androidx.transition.Fade;
import androidx.transition.Slide;
import androidx.transition.TransitionManager;

import android.view.Gravity;
import android.view.View;

import com.squareup.otto.Subscribe;

import org.envirocar.app.main.BaseApplication;
import org.envirocar.app.main.MainActivityComponent;
import org.envirocar.app.main.MainActivityModule;
import org.envirocar.app.R;
import org.envirocar.app.views.trackdetails.TrackDetailsActivity;
import org.envirocar.app.views.utils.ECAnimationUtils;
import org.envirocar.core.entity.Track;
import org.envirocar.core.events.NewUserSettingsEvent;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.trackprocessing.statistics.TrackStatisticsProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * @author dewall
 */
public class TrackListRemoteCardFragment extends AbstractTrackListCardFragment<
        TrackListRemoteCardAdapter> implements TrackListLocalCardFragment.OnTrackUploadedListener {
    private static final Logger LOG = Logger.getLogger(TrackListRemoteCardFragment.class);

    private CompositeSubscription subscriptions = new CompositeSubscription();

    private boolean hasLoadedRemote = false;
    private boolean hasLoadedStored = false;
    private boolean dateFilter = false;
    private boolean carFilter = false;
    private boolean mvVisible = true;
    private Date startDate;
    private Date endDate;
    private String carName;
    private Integer sortC = 0;
    private Integer sortO = 1;
    private FilterViewModel filterViewModel;
    private SortViewModel sortViewModel;
    private List<Track> remoteList = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivityComponent mainActivityComponent = BaseApplication.get(getActivity()).getBaseApplicationComponent().plus(new MainActivityModule(getActivity()));
        mainActivityComponent.inject(this);
        filterViewModel = ViewModelProviders.of(this.getActivity()).get(FilterViewModel.class);
        sortViewModel = ViewModelProviders.of(this.getActivity()).get(SortViewModel.class);

        filterViewModel.getFilterActive().observe(this, item -> {
            dateFilter = filterViewModel.getFilterDate().getValue();
            startDate = filterViewModel.getFilterDateStart().getValue();
            endDate = filterViewModel.getFilterDateEnd().getValue();
            carFilter = filterViewModel.getFilterCar().getValue();
            carName = filterViewModel.getFilterCarName().getValue();
            updateView();
        });

        sortViewModel.getSortActive().observe(this, item -> {
            sortC = sortViewModel.getSortChoice().getValue();
            if (sortViewModel.getSortOrder().getValue())
                sortO = 1;
            else
                sortO = -1;
            updateView();
        });

        sortViewModel.getMapActive().observe(this, item-> {
            mvVisible = sortViewModel.getMapChoice().getValue();
            updateView();
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mUserManager.isLoggedIn()) {
            mRecyclerView.setVisibility(View.VISIBLE);
            infoView.setVisibility(View.GONE);
        } else {
            showText(R.drawable.img_logged_out,
                    R.string.track_list_bg_not_logged_in,
                    R.string.track_list_bg_not_logged_in_sub);
            mProgressView.setVisibility(View.INVISIBLE);
            mRecyclerView.setVisibility(View.GONE);
            mRecyclerViewAdapter.mTrackDataset.clear();
            mRecyclerViewAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mRecyclerViewAdapter.onLowMemory();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRecyclerViewAdapter.onDestroy();
    }

    @Override
    public void onDestroyView() {
        LOG.info("onDestroyView()");
        super.onDestroyView();

        if (!subscriptions.isUnsubscribed()) {
            subscriptions.unsubscribe();
        }
    }

    @Override
    public TrackListRemoteCardAdapter getRecyclerViewAdapter() {
        return new TrackListRemoteCardAdapter(getActivity(), mTrackList,
                new OnTrackInteractionCallback() {

                    /**
                     * Inits the view transition to a {@link TrackDetailsActivity} showing the
                     * details for the given track.
                     *
                     * @param track the track to show the details for.
                     * @param transitionView the transitionView used for scene transition.
                     */
                    @Override
                    public void onTrackDetailsClicked(Track track, View transitionView) {
                        LOG.info(String.format("onTrackDetailsClicked(%s)", track.getTrackID()
                                .toString()));
                        int trackID = (int) track.getTrackID().getId();
                        TrackDetailsActivity.navigate(getActivity(), transitionView, trackID);
                    }

                    @Override
                    public void onDeleteTrackClicked(Track track) {
                        LOG.info(String.format("onDeleteTrackClicked(%s)", track.getTrackID()));
                        // create a dialog
                        createDeleteTrackDialog(track);
                    }

                    @Override
                    public void onUploadTrackClicked(Track track) {
                        LOG.info(String.format("onUploadTrackClicked(%s)", track.getTrackID()));
                        // Upload the track
                        LOG.warn("onUploadTrackClicked() on remote tracks has no effect.");
                    }

                    @Override
                    public void onExportTrackClicked(Track track) {
                        LOG.info(String.format("onExportTrackClicked(%s)", track.getTrackID()));
                        exportTrack(track);
                    }

                    @Override
                    public void onDownloadTrackClicked(Track track, AbstractTrackListCardAdapter
                            .TrackCardViewHolder viewHolder) {
                        onDownloadTrackClickedInner(track, viewHolder);
                    }
                });
    }

    @Override
    protected void loadDataset() {
        LOG.info("loadDataset()");
        // Do not load the dataset twice.
        if (mUserManager.isLoggedIn() && !tracksLoaded) {
            tracksLoaded = true;
            new LoadRemoteTracksTask().execute();
        }
    }

    @Subscribe
    public void onReceiveNewUserSettingsEvent(NewUserSettingsEvent event) {
        if (!event.mIsLoggedIn) {
            mMainThreadWorker.schedule(() -> {
                mRecyclerViewAdapter.mTrackDataset.clear();
                mRecyclerViewAdapter.notifyDataSetChanged();
                tracksLoaded = false;
            });
        }
    }

    private void onDownloadTrackClickedInner(final Track track, AbstractTrackListCardAdapter
            .TrackCardViewHolder viewHolder) {
        AbstractTrackListCardAdapter.RemoteTrackCardViewHolder holder =
                (AbstractTrackListCardAdapter.RemoteTrackCardViewHolder) viewHolder;

        // Show the downloading text notification.
        ECAnimationUtils.animateShowView(getActivity(), holder.mDownloadNotification,
                R.anim.fade_in);
        //holder.mProgressCircle.show();
        track.setDownloadState(Track.DownloadState.DOWNLOADING);

        mTrackDAOHandler.fetchRemoteTrackObservable(track)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Track>() {

                    @Override
                    public void onCompleted() {
                        LOG.info("Track " + track.getRemoteID() + " downloaded. Setting ViewHolder");
                        mRecyclerViewAdapter.bindTrackViewHolder(holder, track, true);

                        // and hide the download button
                        ECAnimationUtils.animateHideView(getActivity(), R.anim.fade_out,
                                holder.mDownloadButton, holder
                                        .mDownloadNotification);
                        /*
                        holder.mProgressCircle.beginFinalAnimation();
                        holder.mProgressCircle.attachListener(() -> {
                            // When the visualization is finished, then Init the
                            // content view including its mapview and track details.
                            mRecyclerViewAdapter.bindTrackViewHolder(holder, track, true);

                            // and hide the download button
                            ECAnimationUtils.animateHideView(getActivity(), R.anim.fade_out,
                                    holder.mProgressCircle, holder.mDownloadButton, holder
                                            .mDownloadNotification);
                            //ECAnimationUtils.animateShowView(getActivity(), holder.mContentView, R
                            //        .anim.fade_in);
                        });
                        */
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error("Not connected exception", e);
                        showSnackbar(R.string.track_list_communication_error);
                        //holder.mProgressCircle.hide();
                        track.setDownloadState(Track.DownloadState.DOWNLOADING);
                        holder.mDownloadNotification.setText(
                                R.string.track_list_error_while_downloading);
                    }

                    @Override
                    public void onNext(Track remoteTrack) {
                        LOG.info("Successfully fetched remote track:");
                    }
                });
    }

    private final class LoadRemoteTracksTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            // Wait until the activity has been attached.
            synchronized (attachingActivityLock) {
                while (!isAttached) {
                    try {
                        attachingActivityLock.wait();
                    } catch (InterruptedException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            }

            subscriptions.add(mEnvirocarDB.getAllRemoteTracks()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<List<Track>>() {

                        @Override
                        public void onStart() {
                            LOG.info("onStart() tracks in db");
                            showProgressView(R.string.track_list_loading_tracks);
                        }

                        @Override
                        public void onCompleted() {
                            LOG.info("onCompleted() tracks in db");
                        }

                        @Override
                        public void onError(Throwable e) {
                            LOG.error(e.getMessage(), e);
                            showSnackbar(R.string.track_list_loading_lremote_tracks_error);
                        }

                        @Override
                        public void onNext(List<Track> tracks) {
                            LOG.info("onNext(" + tracks.size() + ") locally stored tracks");

                            for (Track track : tracks) {
                                if (track.getMeasurements() != null &&
                                        !track.getMeasurements().isEmpty()) {
                                    if (remoteList.contains(track)) {
                                        remoteList.set(remoteList.indexOf(track), track);
                                    } else {
                                        remoteList.add(track);
                                    }
                                }
                            }

                            hasLoadedStored = true;
                            // Sort the list and update the list
                            updateView();
                        }
                    }));

            subscriptions.add(mDAOProvider.getTrackDAO().getTrackIdsObservable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<List<Track>>() {

                        @Override
                        public void onStart() {
                            LOG.info("onStart() tracks in db");
                            showProgressView(R.string.track_list_loading_tracks);
                        }

                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            LOG.error(e.getMessage(), e);

                            if (e instanceof NotConnectedException) {
                                showSnackbar(R.string.track_list_loading_remote_tracks_error);
                                if (remoteList.isEmpty()) {
                                    showText(R.drawable.img_disconnected,
                                            R.string.track_list_bg_no_connection,
                                            R.string.track_list_bg_no_connection_sub);
                                }
                            } else if (e instanceof UnauthorizedException) {
                                showSnackbar(R.string.track_list_bg_unauthorized);
                                if (remoteList.isEmpty()) {
                                    showText(R.drawable.img_logged_out,
                                            R.string.track_list_bg_unauthorized,
                                            R.string.track_list_bg_unauthorized_sub);
                                }
                            }
                            hideProgressView();
                        }

                        @Override
                        public void onNext(List<Track> tracks) {
                            LOG.info("onNext(" + tracks.size() + ") remotely stored tracks");
                            // Add all tracks to the track list that are not in the
                            // list so far
                            for (Track track : tracks) {
                                if (!remoteList.contains(track)) {
                                    remoteList.add(track);
                                }
                            }
                            hasLoadedRemote = true;
                            // Sort the list and update the list
                            updateView();

                        }
                    }));

            return null;
        }
    }

    public void setTrackList() {
        for (Track track : remoteList) {
            if (!mTrackList.contains(track))
                mTrackList.add(track);
        }
    }

    @Override
    public void onTrackUploaded(Track track) {
        mEnvirocarDB.getTrack(track.getTrackID())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(track1 -> {
                    mTrackList.add(track1);
                    mRecyclerViewAdapter.notifyDataSetChanged();
                });
    }

    private void updateView() {
        LOG.info("updateView()");
        if (hasLoadedStored && hasLoadedRemote) {
            LOG.info("Tracked loaded.");
            setTrackList();
            hideProgressView();
            if (mTrackList.isEmpty()) {
                LOG.info("No tracks.");
                showNoTracksInfo(false);
            } else {
                mRecyclerView.removeAllViews();
                mRecyclerViewAdapter.setGuideline(mvVisible);
                TransitionManager.beginDelayedTransition(mRecyclerView, new Slide(Gravity.LEFT).
                        setDuration(1000).
                        setInterpolator(new FastOutSlowInInterpolator()));
                if (dateFilter) {
                    for (int i = 0; i < mTrackList.size(); ++i) {
                        Track track = mTrackList.get(i);
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                        try {
                            Date trackDateStart = simpleDateFormat.parse(track.getBegin());
                            Date trackDateEnd = simpleDateFormat.parse(track.getEnd());
                            if (trackDateStart.before(startDate) || trackDateEnd.after(endDate)) {
                                mTrackList.remove(i);
                                i--;
                            }
                        } catch (Exception e) {
                            LOG.error("Error: ", e);
                        }
                    }
                }
                if (carFilter) {
                    for (int i = 0; i < mTrackList.size(); ++i) {
                        Track track = mTrackList.get(i);
                        String trackCarName = track.getCar().getManufacturer() + " " + track.getCar().getModel();
                        if (!carName.equalsIgnoreCase(trackCarName)) {
                            mTrackList.remove(i);
                            i--;
                        }
                    }
                }

                switch (sortC) {
                    case 0:
                        Collections.sort(mTrackList, new Comparator<Track>() {
                            @Override
                            public int compare(Track track1, Track track2) {
                                int res = track1.getBegin().compareTo(track2.getBegin());
                                return res*sortO;
                            }
                        });
                        break;

                    case 1:
                        Collections.sort(mTrackList, new Comparator<Track>() {
                            @Override
                            public int compare(Track lhs, Track rhs) {
                                Double lhsLen;
                                if (lhs.getLength() == null)
                                    lhsLen = (((TrackStatisticsProvider) lhs).getDistanceOfTrack());
                                else
                                    lhsLen = lhs.getLength();

                                Double rhsLen;
                                if (rhs.getLength() == null)
                                    rhsLen = (((TrackStatisticsProvider) rhs).getDistanceOfTrack());
                                else
                                    rhsLen = rhs.getLength();

                                int res = lhsLen.compareTo(rhsLen);
                                return res*sortO;
                            }
                        });
                        break;

                    case 2:
                        Collections.sort(mTrackList, new Comparator<Track>() {
                            @Override
                            public int compare(Track lhs, Track rhs) {
                                int res = Long.compare(lhs.getTimeInMillis(), rhs.getTimeInMillis());
                                return res*sortO;
                            }
                        });
                        break;

                    case 3:
                        Collections.sort(mTrackList, new Comparator<Track>() {
                            @Override
                            public int compare(Track lhs, Track rhs) {
                                int res = lhs.getCar().getManufacturer().compareTo(rhs.getCar().getManufacturer());
                                return res*sortO;
                            }
                        });
                        break;

                }
                if (!mTrackList.isEmpty()) {
                    mRecyclerView.setVisibility(View.VISIBLE);
                    infoView.setVisibility(View.GONE);
                    mRecyclerViewAdapter.notifyDataSetChanged();
                } else {
                    showNoTracksInfo(true);
                }
            }
        }
    }

    private void showNoTracksInfo(Boolean afterFilter) {
        TransitionManager.beginDelayedTransition(tracklistLayout, new Fade());
        mRecyclerView.setVisibility(View.GONE);
        if (!afterFilter) {
            showText(R.drawable.img_tracks,
                    R.string.track_list_bg_no_remote_tracks,
                    R.string.track_list_bg_no_remote_tracks_sub);
        } else {
            showText(R.drawable.img_tracks,
                    R.string.track_list_bg_no_tracks_after_filter,
                    R.string.track_list_bg_no_tracks_after_filter_sub);
        }
        infoView.setVisibility(View.VISIBLE);
    }
}
