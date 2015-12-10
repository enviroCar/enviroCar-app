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
package org.envirocar.app.view.tracklist;

import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.R;
import org.envirocar.app.handler.TrackHandler;
import org.envirocar.app.view.trackdetails.TrackDetailsActivity;
import org.envirocar.core.entity.Track;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.TrackMetadata;
import org.envirocar.core.util.Util;

import java.util.Collections;
import java.util.List;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @author dewall
 */
public class TrackListLocalCardFragment extends AbstractTrackListCardFragment<
        TrackListLocalCardAdapter> {
    private static final Logger LOG = Logger.getLogger(TrackListLocalCardFragment.class);

    /**
     *
     */
    interface OnTrackUploadedListener {
        void onTrackUploaded(Track track);
    }

    private OnTrackUploadedListener onTrackUploadedListener;
    private Subscription subscription;

    private void uploadTrack(Track track) {
        mBackgroundWorker.schedule(() -> mTrackHandler.uploadTrack(getActivity(), track,
                new TrackHandler
                        .TrackUploadCallback() {

                    private MaterialDialog mProgressDialog;

                    @Override
                    public void onUploadStarted(Track track) {
                        mMainThreadWorker.schedule(() ->
                                mProgressDialog = new MaterialDialog.Builder(getActivity())
                                        .title(R.string.track_list_upload_track_uploading)
                                        .content(R.string.track_list_upload_track_please_wait)
                                        .progress(true, 0)
                                        .show());
                    }

                    @Override
                    public void onSuccessfulUpload(Track track) {
                        if (mProgressDialog != null) mProgressDialog.dismiss();
                        showSnackbar(String.format(
                                getString(R.string.track_list_upload_track_success_template),
                                track.getName()));

                        // Update the lists.
                        mMainThreadWorker.schedule(() -> {
                            mRecyclerViewAdapter.removeItem(track);
                            mRecyclerViewAdapter.notifyDataSetChanged();
                        });

                        onTrackUploadedListener.onTrackUploaded(track);
                    }

                    @Override
                    public void onError(Track track, String message) {
                        if (mProgressDialog != null)
                            mProgressDialog.dismiss();
                        showSnackbar(message);
                    }
                }));
    }

    @Override
    public TrackListLocalCardAdapter getRecyclerViewAdapter() {
        return new TrackListLocalCardAdapter(mTrackList, new OnTrackInteractionCallback() {

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
                uploadTrack(track);
            }

            @Override
            public void onExportTrackClicked(Track track) {
                LOG.info(String.format("onExportTrackClicked(%s)", track.getTrackID()));
                track.updateMetadata(new TrackMetadata(Util.getVersionString(getActivity()),
                        mUserManager.getUser().getTermsOfUseVersion()));
                exportTrack(track);
            }

            @Override
            public void onDownloadTrackClicked(Track track, AbstractTrackListCardAdapter
                    .TrackCardViewHolder holder) {
                // NOT REQUIRED
            }
        });
    }

    @Override
    protected void loadDataset() {
        // Do not load the dataset twice.
        if (!tracksLoaded) {
            tracksLoaded = true;
            new LoadLocalTracksTask().execute();
        }
    }

    @Override
    public void onDestroyView() {
        LOG.info("onDestroyView()");
        super.onDestroyView();

        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
    }


    private final class LoadLocalTracksTask extends AsyncTask<Void, Void, Void> {

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

            subscription = mEnvirocarDB.getAllLocalTracks()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<List<Track>>() {

                        @Override
                        public void onStart() {
                            LOG.info("onStart() allLocalTracks");
                            mMainThreadWorker.schedule(() -> {
                                mProgressView.setVisibility(View.VISIBLE);
                                mProgressText.setText(R.string.track_list_loading_tracks);
                            });

                        }

                        @Override
                        public void onCompleted() {
                            LOG.info("onCompleted() allLocalTracks");
                        }

                        @Override
                        public void onError(Throwable e) {
                            LOG.error(e.getMessage(), e);

                            showText(R.drawable.img_alert,
                                    R.string.track_list_bg_error,
                                    R.string.track_list_bg_error_sub);

                            Snackbar.make(getView(),
                                    R.string.track_list_loading_tracks_error_snackbar,
                                    Snackbar.LENGTH_LONG).show();
                        }

                        @Override
                        public void onNext(List<Track> tracks) {
                            LOG.info(String.format("onNext(%s)", tracks.size()));

                            boolean newTrackAdded = false;
                            for (Track track : tracks) {
                                if (!mTrackList.contains(track)) {
                                    mTrackList.add(track);
                                    newTrackAdded = true;
                                }
                            }

                            mProgressView.setVisibility(View.INVISIBLE);
                            if (newTrackAdded) {
                                Collections.sort(mTrackList);

                                mRecyclerView.setVisibility(View.VISIBLE);
                                infoView.setVisibility(View.GONE);
                                mRecyclerViewAdapter.notifyDataSetChanged();
                            } else if (mTrackList.isEmpty()) {
                                showText(R.drawable.img_tracks,
                                        R.string.track_list_bg_no_local_tracks,
                                        R.string.track_list_bg_no_local_tracks_sub);
                            }
                        }
                    });

            return null;
        }
    }

    /**
     * Sets the {@link OnTrackUploadedListener}.
     *
     * @param listener the listener to set.
     */
    public void setOnTrackUploadedListener(OnTrackUploadedListener listener) {
        this.onTrackUploadedListener = listener;
    }
}
