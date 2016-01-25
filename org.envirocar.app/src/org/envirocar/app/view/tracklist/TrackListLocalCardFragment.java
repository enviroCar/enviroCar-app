/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.app.view.tracklist;

import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.R;
import org.envirocar.app.view.trackdetails.TrackDetailsActivity;
import org.envirocar.app.view.utils.ECAnimationUtils;
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
 * TODO JavaDoc
 *
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

    private Subscription loadTracksSubscription;
    private Subscription uploadTrackSubscription;

    private int localTrackSize = 0;


    @Override
    public void onResume() {
        LOG.info("onResume()");
        super.onResume();

        mFAB.setOnClickListener(v -> new MaterialDialog.Builder(getContext())
                .title(R.string.track_list_upload_all_tracks_title)
                .content(R.string.track_list_upload_all_tracks_content)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .onPositive((materialDialog, dialogAction) -> uploadAllLocalTracks())
                .show());
    }

    private void uploadAllLocalTracks() {
//        uploadTrackSubscription = mEnvirocarDB.getAllLocalTracks()
//                .first()
//                .subscribeOn(Schedulers.io())
//                .observeOn(Schedulers.io())
//                .subscribe(new Subscriber<List<Track>>() {
//                    private MaterialDialog dialog;
//
//                    @Override
//                    public void onStart() {
//                        super.onStart();
//
//                        dialog = new MaterialDialog.Builder(getContext())
//                                .title(R.string.track_list_upload_all_tracks_title)
//                                .cancelable(false)
//                                .negativeText(R.string.cancel)
//                                .progress(true, tracksToUpload)
//                                .onNegative((materialDialog1, dialogAction) -> unsubscribe())
//                                .show();
//                    }
//
//                    @Override
//                    public void onCompleted() {
//
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//
//                    }
//
//                    @Override
//                    public void onNext(List<Track> tracks) {
//
//                    }
//                });

        uploadTrackSubscription = mEnvirocarDB.getAllLocalTracks()
                .first()
                .map(tracks -> {
                    LOG.info("Tracks : " + tracks.size());
                    localTrackSize = tracks.size();
                    return tracks;
                })
                .flatMap(tracks -> mTrackUploadHandler.uploadMultipleTracks(tracks)
//                .lift(new Observable.Operator<R, R>() {
//                    @Override
//                    public Subscriber<? super R> call(Subscriber<? super R> subscriber) {
//                        return new Subscriber<R>() {
//                            @Override
//                            public void onCompleted() {
//                                subscriber.onCompleted();
//                            }
//
//                            @Override
//                            public void onError(Throwable e) {
//                                if(e instanceof NoMeasurementsException){
//
//                                } else{
//                                    subscriber.onError(e);
//                                }
//                            }
//
//                            @Override
//                            public void onNext(R track) {
//                                subscriber.onNext(track);
//                            }
//                        };
//                    }
//                })
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(() -> LOG.info("Upload has been canceled"))
                .subscribe(new Subscriber<Track>() {
                    private MaterialDialog materialDialog;
                    private int tracksToUpload = localTrackSize;
                    private int currentTrack = 1;

                    @Override
                    public void onStart() {
                        LOG.info("onStart(): Upload of all local tracks started.");
                        materialDialog = new MaterialDialog.Builder(getContext())
                                .title(R.string.track_list_upload_all_tracks_title)
                                .cancelable(false)
                                .negativeText(R.string.cancel)
                                .progress(true, tracksToUpload)
                                .onNegative((materialDialog1, dialogAction) -> unsubscribe())
                                .show();
                    }

                    @Override
                    public void onCompleted() {
                        LOG.info("onCompleted(): Upload of all local tracks finished.");
                        materialDialog.dismiss();

                        showSnackbar(String.format("%s tracks have been uploaded.",
                                localTrackSize));
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error(e.getMessage(), e);
                        materialDialog.dismiss();

                        showSnackbar(String.format(""));
                    }

                    @Override
                    public void onNext(Track uploaded) {
                        LOG.info("onNext(): Track [%s] has been uploaded -> [%s]",
                                uploaded.getName(), uploaded.getRemoteID());

                        // Update the lists.
                        mRecyclerViewAdapter.removeItem(uploaded);
                        mRecyclerViewAdapter.notifyDataSetChanged();
                        onTrackUploadedListener.onTrackUploaded(uploaded);

                        updateDialogContent();
                    }

                    private void updateDialogContent() {
                        currentTrack++;
                        materialDialog.setContent(String.format(getString(R.string
                                        .track_list_upload_all_tracks_content),
                                currentTrack, tracksToUpload));
                        materialDialog.incrementProgress(1);
                    }
                });
    }

    private void uploadTrack(Track track) {
        uploadTrackSubscription = mTrackUploadHandler
                .uploadSingleTrack(track, getActivity())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Track>() {
                    private MaterialDialog progressDialog;

                    @Override
                    public void onStart() {
                        progressDialog = new MaterialDialog.Builder(getActivity())
                                .title(R.string.track_list_upload_track_uploading)
                                .content(R.string.track_list_upload_track_please_wait)
                                .progress(true, 0)
                                .show();
                    }

                    @Override
                    public void onCompleted() {
                        if (progressDialog != null) progressDialog.dismiss();
                        showSnackbar(String.format(
                                getString(R.string.track_list_upload_track_success_template),
                                track.getName()));
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error(e.getMessage(), e);
                        if (progressDialog != null)
                            progressDialog.dismiss();
                        showSnackbar(e.getMessage());
                    }

                    @Override
                    public void onNext(Track track) {
                        // Update the lists.
                        mRecyclerViewAdapter.removeItem(track);
                        mRecyclerViewAdapter.notifyDataSetChanged();

                        onTrackUploadedListener.onTrackUploaded(track);
                    }
                });
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

        if (loadTracksSubscription != null && !loadTracksSubscription.isUnsubscribed()) {
            loadTracksSubscription.unsubscribe();
        }

        if (uploadTrackSubscription != null && !uploadTrackSubscription.isUnsubscribed()) {
            uploadTrackSubscription.unsubscribe();
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

            loadTracksSubscription = mEnvirocarDB.getAllLocalTracks()
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

                                ECAnimationUtils.animateShowView(getContext(), mFAB,
                                        R.anim.translate_slide_in_bottom_fragment);
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
