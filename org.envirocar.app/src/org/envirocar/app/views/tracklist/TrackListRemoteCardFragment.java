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
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.squareup.otto.Subscribe;

import org.envirocar.app.BaseApplication;
import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.events.TrackchunkEndUploadedEvent;
import org.envirocar.app.injection.components.MainActivityComponent;
import org.envirocar.app.injection.modules.MainActivityModule;
import org.envirocar.app.views.trackdetails.TrackDetailsActivity;
import org.envirocar.app.views.utils.ECAnimationUtils;
import org.envirocar.core.entity.Track;
import org.envirocar.core.events.NewUserSettingsEvent;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.logging.Logger;

import java.util.Collections;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;


/**
 * @author dewall
 */
public class TrackListRemoteCardFragment extends AbstractTrackListCardFragment<TrackListRemoteCardAdapter>
        implements TrackListLocalCardFragment.OnTrackUploadedListener {
    private static final Logger LOG = Logger.getLogger(TrackListRemoteCardFragment.class);

    private CompositeDisposable subscriptions = new CompositeDisposable();

    private boolean hasLoadedRemote = false;
    private boolean hasLoadedStored = false;
    private boolean isSorted = false;
    private boolean connectionError = false;

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivityComponent mainActivityComponent = BaseApplication.get(getActivity()).getBaseApplicationComponent().plus(new MainActivityModule(getActivity()));
        mainActivityComponent.inject(this);
        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDataset();
    }

    @Override
    public void onDestroyView() {
        LOG.info("onDestroyView()");
        super.onDestroyView();

        if (!subscriptions.isDisposed()) {
            subscriptions.dispose();
        }
    }

    @Override
    public TrackListRemoteCardAdapter getRecyclerViewAdapter() {
        return new TrackListRemoteCardAdapter(mTrackList,
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
                    public void onShareTrackClicked(Track track) {
                        LOG.info(String.format("onExportTrackClicked(%s)", track.getTrackID()));
                        shareTrack(track);
                    }

                    @Override
                    public void onDownloadTrackClicked(Track track, AbstractTrackListCardAdapter
                            .TrackCardViewHolder viewHolder) {
                        onDownloadTrackClickedInner(track, viewHolder);
                    }

                    @Override
                    public void onLongPressedTrack(Track track) {
                        createDeleteTrackDialog(track);
                    }
                });
    }

    @Override
    protected void loadDataset() {
        LOG.info("loadDataset()");
        // Do not load the dataset twice.
        if (/*mUserManager.isLoggedIn() && */!tracksLoaded) {
            tracksLoaded = true;
            new LoadRemoteTracksTask().execute();
        }
    }

    @Subscribe
    public void onReceiveNewUserSettingsEvent(NewUserSettingsEvent event) {
        if (!event.mIsLoggedIn) {
            mMainThreadWorker.schedule(() -> {
                mRecyclerViewAdapter.clearTracks();
                tracksLoaded = false;
            });
        }
    }

    @Subscribe
    public void onReceiveTrackchunkEndUploadedEvent(TrackchunkEndUploadedEvent event) {
        LOG.info("Received TrackchunkEndUploadedEvent");
        mMainThreadWorker.schedule(() -> {
            mRecyclerViewAdapter.clearTracks();
            tracksLoaded = false;
        });
    }

    private void onDownloadTrackClickedInner(final Track track, AbstractTrackListCardAdapter
            .TrackCardViewHolder viewHolder) {
        AbstractTrackListCardAdapter.RemoteTrackCardViewHolder holder =
                (AbstractTrackListCardAdapter.RemoteTrackCardViewHolder) viewHolder;

        // Show the downloading text notification.
        ECAnimationUtils.animateShowView(
                getActivity(),
                holder.getDownloadingNotification(),
                R.anim.fade_in
        );
        holder.getProgressCircle().show();
        track.setDownloadState(Track.DownloadState.DOWNLOADING);

        mTrackDAOHandler.fetchRemoteTrackObservable(track)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<Track>() {

                    @Override
                    public void onComplete() {
                        holder.getProgressCircle().beginFinalAnimation();
                        holder.getProgressCircle().attachListener(() -> {
                            // When the visualization is finished, then Init the
                            // content view including its mapview and track details.
                            mRecyclerViewAdapter.bindTrackCardViewHolder(holder, track);

                            // and hide the download button
                            ECAnimationUtils.animateHideView(
                                    getActivity(),
                                    R.anim.fade_out,
                                    holder.getProgressCircle(),
                                    holder.getDownloadFab(),
                                    holder.getDownloadingNotification()
                            );
                            ECAnimationUtils.animateShowView(
                                    getActivity(),
                                    holder.getContentView(),
                                    R.anim.fade_in
                            );
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error("Not connected exception", e);
                        showSnackbar(R.string.track_list_communication_error);
                        holder.getProgressCircle().hide();
                        track.setDownloadState(Track.DownloadState.DOWNLOADING);
                        holder.getDownloadingNotification().setText(R.string.track_list_error_while_downloading);
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

            connectionError = false;

            subscriptions.add(mEnvirocarDB.getAllRemoteTracks()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableObserver<List<Track>>() {

                        @Override
                        public void onStart() {
                            LOG.info("onStart() tracks in db");
                            mMainThreadWorker.schedule(() -> {
                                mProgressView.setVisibility(View.VISIBLE);
                                mProgressText.setText(R.string.track_list_loading_tracks);
                            });
                        }

                        @Override
                        public void onComplete() {
                            sortTrackList();
                        }

                        @Override
                        public void onError(Throwable e) {
                            LOG.error(e.getMessage(), e);
                            showSnackbar(R.string.track_list_loading_lremote_tracks_error+e.getMessage());
                        }

                        @Override
                        public void onNext(List<Track> tracks) {
                            LOG.info("onNext(" + tracks.size() + ") locally stored tracks");
                            for (Track track : tracks) {
                                if (track.getMeasurements() != null && !track.getMeasurements().isEmpty()) {
                                    if (mTrackList.contains(track)) {
                                        mTrackList.set(mTrackList.indexOf(track), track);
                                    } else {
                                        mTrackList.add(track);
                                    }
                                }
                            }

                            hasLoadedStored = true;
                            updateView();
                        }
                    }));

            subscriptions.add(mDAOProvider.getTrackDAO().getTrackIdsObservable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableObserver<List<Track>>() {

                        @Override
                        public void onStart() {
                            LOG.info("onStart() tracks in db");
                            mMainThreadWorker.schedule(() -> {
                                mProgressView.setVisibility(View.VISIBLE);
                                mProgressText.setText(R.string.track_list_loading_tracks);
                            });
                        }

                        @Override
                        public void onComplete() {
                            sortTrackList();
                        }

                        @Override
                        public void onError(Throwable e) {
                            LOG.error(e.getMessage(), e);

                            if (e instanceof UnauthorizedException) {
                                showSnackbar(R.string.track_list_bg_unauthorized);

                            } else if (e instanceof NotConnectedException) {
                                showSnackbar(R.string.track_list_loading_remote_tracks_error);

                                connectionError = true;
                                tracksLoaded = false;
                            }

                            updateInfoBackground();
                            ECAnimationUtils.animateHideView(getActivity(), mProgressView,
                                    R.anim.fade_out);
                        }

                        @Override
                        public void onNext(List<Track> tracks) {
                            LOG.info("onNext(" + tracks.size() + ") remotely stored tracks");

                            // Add all tracks to the track list that are not in the
                            // list so far
                            for (Track track : tracks) {
                                if (!mTrackList.contains(track)) {
                                    mTrackList.add(track);
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

    @Override
    public void onTrackUploaded(Track track) {

        subscriptions.add(mEnvirocarDB.getTrack(track.getTrackID())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Track>() {
                    @Override
                    public void onNext(@NonNull Track track) {
                        mTrackList.add(track);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LOG.error(e.getMessage(), e);
                    }

                    @Override
                    public void onComplete() {
                        sortTrackList();
                        mRecyclerViewAdapter.notifyDataSetChanged();
                        updateInfoBackground();
                    }
                }));
    }

    private void updateView() {
        if (hasLoadedStored && hasLoadedRemote) {
            if (!isSorted) {
                isSorted = true;
                sortTrackList();
            }
            ECAnimationUtils.animateHideView(getActivity(), mProgressView, R.anim.fade_out);
        }

        updateInfoBackground();

        if (!mTrackList.isEmpty()) {
            mRecyclerViewAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void showNoTracksInfo() {
        showText(R.drawable.img_tracks,
                R.string.track_list_bg_no_remote_tracks,
                R.string.track_list_bg_no_remote_tracks_sub);
    }

    private void sortTrackList(){
        Collections.sort(mTrackList, (o1, o2) -> {
            if (o1.getStartTime() < o2.getStartTime())
                return 1;
            if (o1.getStartTime() > o2.getStartTime())
                return -1;
            return 0;
        });
    }

    private void updateInfoBackground(){

        if (!mUserManager.isLoggedIn()){
            showText(R.drawable.img_logged_out,
                    R.string.track_list_bg_not_logged_in,
                    R.string.track_list_bg_not_logged_in_sub);
            mProgressView.setVisibility(View.INVISIBLE);
            mRecyclerView.setVisibility(View.GONE);
            mRecyclerViewAdapter.clearTracks();
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);

            if(mTrackList.isEmpty() && connectionError && hasLoadedStored) {
                showText(R.drawable.img_disconnected,
                        R.string.track_list_bg_no_connection,
                        R.string.track_list_bg_no_connection_sub);
            } else if(hasLoadedStored && hasLoadedRemote && mTrackList.isEmpty()){
                showNoTracksInfo();
            } else {
                infoView.setVisibility(View.GONE);
            }
        }
    }
}
