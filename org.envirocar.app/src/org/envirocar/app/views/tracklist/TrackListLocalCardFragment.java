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

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.squareup.otto.Subscribe;

import org.envirocar.app.BaseApplication;
import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.injection.components.MainActivityComponent;
import org.envirocar.app.injection.modules.MainActivityModule;
import org.envirocar.app.interactor.UploadAllTracks;
import org.envirocar.app.interactor.UploadTrack;
import org.envirocar.app.views.trackdetails.TrackDetailsActivity;
import org.envirocar.app.views.utils.ECAnimationUtils;
import org.envirocar.core.ContextInternetAccessProvider;
import org.envirocar.core.entity.Track;
import org.envirocar.core.events.TrackFinishedEvent;
import org.envirocar.core.exception.TrackUploadException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.TrackMetadata;
import org.envirocar.core.util.Util;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;


/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class TrackListLocalCardFragment extends AbstractTrackListCardFragment<TrackListLocalCardAdapter> {
    private static final Logger LOG = Logger.getLogger(TrackListLocalCardFragment.class);

    interface OnTrackUploadedListener {
        void onTrackUploaded(Track track);
    }

    @Inject
    protected UploadTrack uploadTrack;
    @Inject
    protected UploadAllTracks uploadAllTracks;

    private OnTrackUploadedListener onTrackUploadedListener;

    private Disposable loadTracksSubscription;
    private Disposable uploadTrackSubscription;
    private final ContextInternetAccessProvider contextInternetAccessProvider = new ContextInternetAccessProvider(getContext());

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivityComponent mainActivityComponent =
                BaseApplication.get(getActivity())
                        .getBaseApplicationComponent()
                        .plus(new MainActivityModule(getActivity()));
        mainActivityComponent.inject(this);
    }

    @Override
    public void onResume() {
        LOG.info("onResume()");
        super.onResume();
        loadDataset();
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

    @OnClick(R.id.fragment_tracklist_fab)
    protected void onUploadTracksFABClicked() {
        new MaterialAlertDialogBuilder(getContext(), R.style.MaterialDialog)
                .setTitle(R.string.track_list_upload_all_tracks_title)
                .setMessage(R.string.track_list_upload_all_tracks_content)
                .setIcon(R.drawable.ic_cloud_upload_white_24dp)
                .setPositiveButton(R.string.ok, this::onUploadAllLocalTracks)
                .setNegativeButton(R.string.cancel,null)
                .show();
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

                // check if device is connected to internet before uploading the track
                if (!contextInternetAccessProvider.isConnected() && getView() != null) {
                    LOG.info("There is no internet connected, no tracks got uploaded");
                    showSnackbar(R.string.track_list_upload_error_no_network_connection);
                    return;
                }
                // Upload the track
                onUploadSingleTrack(track);
            }

            @Override
            public void onShareTrackClicked(Track track) {
                LOG.info(String.format("onExportTrackClicked(%s)", track.getTrackID()));
                if (mUserManager.getUser() != null) {
                    track.updateMetadata(new TrackMetadata(Util.getVersionString(getActivity()),
                            mUserManager.getUser().getTermsOfUseVersion()));
                } else {
                    track.updateMetadata(new TrackMetadata(Util.getVersionString(getActivity()),
                            null));

                }
                shareTrack(track);
            }

            @Override
            public void onDownloadTrackClicked(Track track, AbstractTrackListCardAdapter
                    .TrackCardViewHolder holder) {
                // NOT REQUIRED
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
        if (!tracksLoaded) {
            tracksLoaded = true;
            new LoadLocalTracksTask().execute();
        }
    }

    @Subscribe
    public void onTrackFinishedEvent(TrackFinishedEvent event){
        tracksLoaded = false;
    }

    @Override
    public void onDestroyView() {
        LOG.info("onDestroyView()");
        super.onDestroyView();

        if (loadTracksSubscription != null && !loadTracksSubscription.isDisposed()) {
            loadTracksSubscription.dispose();
        }

        if (uploadTrackSubscription != null && !uploadTrackSubscription.isDisposed()) {
            uploadTrackSubscription.dispose();
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
                    .subscribeWith(new DisposableObserver<List<Track>>() {

                        @Override
                        public void onStart() {
                            LOG.info("onStart() allLocalTracks");
                            mMainThreadWorker.schedule(() -> {
                                mProgressView.setVisibility(View.VISIBLE);
                                mProgressText.setText(R.string.track_list_loading_tracks);
                            });

                        }

                        @Override
                        public void onComplete() {
                            LOG.info("onCompleted() allLocalTracks");
                        }

                        @Override
                        public void onError(Throwable e) {
                            LOG.error(e.getMessage(), e);

                            showText(R.drawable.img_alert,
                                    R.string.track_list_bg_error,
                                    R.string.track_list_bg_error_sub);

                            showSnackbar(R.string.track_list_loading_tracks_error_snackbar);
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

                                ECAnimationUtils.animateShowView(getActivity(), mFAB,
                                        R.anim.translate_slide_in_bottom_fragment);
                            } else if (mTrackList.isEmpty()) {
                                showNoTracksInfo();
                            }
                        }
                    });

            return null;
        }
    }

    @Override
    protected void showNoTracksInfo() {
        showText(R.drawable.img_tracks,
                R.string.track_list_bg_no_local_tracks,
                R.string.track_list_bg_no_local_tracks_sub);
        ECAnimationUtils.animateHideView(getActivity(), mFAB,
                R.anim.translate_slide_out_bottom);
    }

    /**
     * Sets the {@link OnTrackUploadedListener}.
     *
     * @param listener the listener to set.
     */
    public void setOnTrackUploadedListener(OnTrackUploadedListener listener) {
        this.onTrackUploadedListener = listener;
    }

    private void onUploadSingleTrack(Track track) {
        if (uploadTrackSubscription != null && !uploadTrackSubscription.isDisposed()) {
            uploadTrackSubscription.dispose();
            uploadTrackSubscription = null;
        }

        uploadTrackSubscription = uploadTrack.execute(new UploadTrack.Params(track, getActivity()))
                .subscribeWith(new UploadTrackDialogObserver(track));
    }

    private void onUploadAllLocalTracks(DialogInterface dialog, int which) {
        // check if device is connected to internet before uploading the track
        if (!contextInternetAccessProvider.isConnected() && getView() != null) {
            LOG.info("There is no internet connected, no tracks got uploaded");
            showSnackbar(R.string.track_list_upload_error_no_network_connection);
            return;
        }
        if (uploadTrackSubscription != null && !uploadTrackSubscription.isDisposed()) {
            uploadTrackSubscription.dispose();
            uploadTrackSubscription = null;
        }

        int localTracksCount = mEnvirocarDB.getAllLocalTracksCount().blockingFirst();
        uploadTrackSubscription = uploadAllTracks.execute(getActivity())
                .subscribeWith(new UploadTracksDialogObserver(localTracksCount));
    }

    /**
     * Observer class for handling the UI handling of uploading a single track.
     */
    private class UploadTrackDialogObserver extends DisposableObserver<Track> {
        private View contentView;
        private MaterialDialog dialog;
        private Track track;

        /**
         * Constructor.
         *
         * @param track the track to upload.
         */
        public UploadTrackDialogObserver(Track track) {
            this.track = track;

            // Inflate the dialog content view and set the track name.
            this.contentView = getActivity().getLayoutInflater().inflate(R.layout.fragment_tracklist_uploading_single_dialog,
                    null, false);
            TextView trackName = contentView.findViewById(R.id.fragment_tracklist_uploading_single_dialog_trackname);
            trackName.setText(track.getName());
        }

        @Override
        protected void onStart() {
            super.onStart();
            // Create the dialog to show.
            this.dialog = new MaterialDialog.Builder(getContext())
                    .title(R.string.track_list_upload_track_uploading)
                    .customView(contentView, false)
                    .cancelable(false)
                    .negativeText(R.string.cancel)
                    .onNegative((materialDialog, dialogAction) -> {
                        dispose();
                    })
                    .show();
        }

        @Override
        public void onNext(Track track) {
            // Update the lists.
            mRecyclerViewAdapter.removeItem(track);
            mRecyclerViewAdapter.notifyDataSetChanged();

            if (onTrackUploadedListener != null)
                onTrackUploadedListener.onTrackUploaded(track);
        }

        @Override
        public void onError(Throwable e) {
            LOG.warn(e.getMessage(), e);
            if (e instanceof TrackUploadException) {
                switch (((TrackUploadException) e).getReason()) {
                    case NOT_ENOUGH_MEASUREMENTS:
                        showSnackbar(R.string.track_list_upload_error_no_measurements);
                        break;
                    case TRACK_WITH_NO_VALID_CAR:
                        showSnackbar(R.string.track_list_upload_error_no_valid_car);
                        break;
                    case TRACK_ALREADY_UPLOADED:
                        showSnackbar(R.string.track_list_upload_error_already_uploaded);
                        break;
                    case NO_NETWORK_CONNECTION:
                        showSnackbar(R.string.track_list_upload_error_no_network_connection);
                        break;
                    case NOT_LOGGED_IN:
                        showSnackbar(R.string.track_list_upload_error_not_logged_in);
                        break;
                    case NO_CAR_ASSIGNED:
                        showSnackbar(R.string.track_list_upload_error_no_valid_car);
                        break;
                    case GPS_TRACKS_NOT_ALLOWED:
                        showSnackbar(R.string.track_list_upload_error_gps_track);
                        break;
                    case UNAUTHORIZED:
                        showSnackbar(R.string.track_list_upload_error_unauthorized);
                        break;
                    case UNKNOWN:
                        showSnackbar(R.string.track_list_upload_track_general_error);
                        break;
                }
            } else {
                showSnackbar(R.string.track_list_upload_track_general_error);
            }
            dialog.dismiss();
        }

        @Override
        public void onComplete() {
            LOG.info("Received uploadTrack.onComplete() event.");
            showSnackbar(String.format(getString(R.string.track_list_upload_track_success_template), track.getName()));
            dialog.dismiss();
            if (mTrackList.isEmpty()) {
                showNoTracksInfo();
            }
        }
    }

    /**
     * Observer class for handling the dialog for uploading all local tracks.
     */
    private class UploadTracksDialogObserver extends DisposableObserver<UploadAllTracks.Result> {
        private int numTracks;
        private int numberOfSuccesses = 0;
        private int numberOfFailures = 0;

        private TextView infoText;
        private ProgressBar progressBar;
        private TextView percentageText;
        private TextView progressText;
        private MaterialDialog dialog;
        private View contentView;

        /**
         * Constructor.
         *
         * @param numTracks
         */
        public UploadTracksDialogObserver(int numTracks) {
            this.numTracks = numTracks;

            // Inflate the custom dialog view.
            this.contentView = getLayoutInflater().inflate(R.layout.fragment_tracklist_uploading_dialog, null, false);
            this.infoText = contentView.findViewById(R.id.fragment_tracklist_uploading_dialog_info);
            this.progressBar = contentView.findViewById(R.id.fragment_tracklist_uploading_dialog_progressbar);
            this.percentageText = contentView.findViewById(R.id.fragment_tracklist_uploading_dialog_percentage);
            this.progressText = contentView.findViewById(R.id.fragment_tracklist_uploading_dialog_progress);

            this.progressBar.setMax(numTracks);
        }

        @Override
        public void onStart() {
            if (isDisposed())
                return;

            updateProgressView(0);
            dialog = new MaterialDialog.Builder(getContext())
                    .title(R.string.track_list_upload_track_uploading)
                    .customView(contentView, false)
                    .negativeText(R.string.cancel)
                    .onNegative((dialog, dialogAction) -> dispose())
                    .show();
        }

        @Override
        public void onComplete() {
            LOG.info("Received uploadAllTracks.onComplete() event.");
            if (isDisposed())
                return;

            if (numberOfFailures > 0) {
                String snackbarText = String.format(getString(R.string.track_list_upload_all_tracks_complete_with_failure_template),
                        numberOfSuccesses, numTracks, numberOfFailures);
                showSnackbar(snackbarText);
            } else {
                String snackbarText = String.format(getString(R.string.track_list_upload_all_tracks_complete_template),
                        numTracks);
                showSnackbar(snackbarText);
            }

            dialog.dismiss();
            if (mTrackList.isEmpty()) {
                showNoTracksInfo();
            }
        }

        @Override
        public void onNext(UploadAllTracks.Result result) {
            if (isDisposed())
                return;

            if (result.isSuccessful()) {
                numberOfSuccesses++;
                // Update the lists.
                mRecyclerViewAdapter.removeItem(result.getTrack());
                mRecyclerViewAdapter.notifyDataSetChanged();

                if (onTrackUploadedListener != null)
                    onTrackUploadedListener.onTrackUploaded(result.getTrack());
            } else {
                numberOfFailures++;
            }

            updateProgressView(numberOfFailures + numberOfSuccesses);
            if ((numberOfFailures + numberOfSuccesses) == numTracks) {
                onComplete();
            }
        }

        @Override
        public void onError(Throwable e) {
            if (isDisposed())
                return;

            showSnackbar(R.string.track_list_local_track_general_error);
            dialog.dismiss();
        }

        private void updateProgressView(int progress) {
            progressBar.setProgress(progress);
            progressBar.setSecondaryProgress(progress + 1);

            percentageText.setText((progress / numTracks) * 100 + "%");
            progressText.setText(progress + " / " + numTracks);
        }
    }
}
