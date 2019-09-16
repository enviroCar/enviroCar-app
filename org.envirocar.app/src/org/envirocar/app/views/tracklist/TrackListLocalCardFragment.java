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
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.base.Preconditions;

import org.envirocar.app.R;
import org.envirocar.app.main.BaseApplication;
import org.envirocar.app.main.MainActivityComponent;
import org.envirocar.app.main.MainActivityModule;
import org.envirocar.app.views.trackdetails.TrackDetailsActivity;
import org.envirocar.app.views.utils.DialogUtils;
import org.envirocar.app.views.utils.ECAnimationUtils;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.TrackMetadata;
import org.envirocar.core.util.Util;

import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;


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

    private Disposable loadTracksSubscription;
    private Disposable uploadTrackSubscription;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivityComponent mainActivityComponent = BaseApplication.get(getActivity()).getBaseApplicationComponent().plus(new MainActivityModule(getActivity()));
        mainActivityComponent.inject(this);
    }

    @Override
    public void onResume() {
        LOG.info("onResume()");
        super.onResume();

        mFAB.setOnClickListener(v -> DialogUtils.createDefaultDialogBuilder(getActivity(),
                R.string.track_list_upload_all_tracks_title,
                R.drawable.ic_cloud_upload_white_24dp,
                R.string.track_list_upload_all_tracks_content)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .onPositive((materialDialog, dialogAction) -> uploadAllLocalTracks())
                .show());
    }

    private void uploadAllLocalTracks() {
        uploadTrackSubscription = Observable.defer(() -> mEnvirocarDB.getAllLocalTracks())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .firstOrError()
                .toObservable()
                .concatMap(tracks -> uploadTracksWithDialogObservable(tracks))
                .subscribeWith(new DisposableObserver<Track>() {
                    @Override
                    public void onComplete() {
                        LOG.info("onCompleted()");
                        if (mRecyclerViewAdapter.getItemCount() <= 0) {
                            showNoLocalTracksInfo();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.warn(e.getMessage(), e);
                    }

                    @Override
                    public void onNext(Track track) {
                        LOG.info("Track successfully uploaded -> [%s]", track.getName());

                        // Update the lists.
                        mRecyclerViewAdapter.removeItem(track);
                        mRecyclerViewAdapter.notifyDataSetChanged();
                        onTrackUploadedListener.onTrackUploaded(track);
                    }
                });
    }

    private void uploadTrack(Track track) {
        uploadTrackSubscription = uploadTrackWithDialogObservable(track)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Track>() {
                    @Override
                    public void onComplete() {
                        LOG.info("uploadTrack.onCompleted()");
                        showSnackbar(String.format(
                                getString(R.string.track_list_upload_track_success_template),
                                track.getName()));
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.warn(e.getMessage(), e);
                        if (e.getCause() instanceof NoMeasurementsException) {
                            showSnackbar(R.string.track_list_upload_track_no_measurements);
                        } else {
                            showSnackbar(R.string.track_list_upload_track_general_error);
                        }
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
                if (track.hasProperty(Measurement.PropertyKey.SPEED)) {
                    // Upload the track
                    uploadTrack(track);
                } else {
                    showSnackbar(R.string.trackviews_cannot_upload_gps_tracks);
                }
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
        LOG.info("loadDataset()");
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

        if (loadTracksSubscription != null && !loadTracksSubscription.isDisposed()) {
            loadTracksSubscription.dispose();
        }

        if (uploadTrackSubscription != null && !uploadTrackSubscription.isDisposed()) {
            uploadTrackSubscription.dispose();
        }
    }

    private Observable<Track> uploadTrackWithDialogObservable(Track track) {
        Preconditions.checkNotNull(track, "Input track cannot be null");
        return Observable.create(new ObservableOnSubscribe<Track>() {
            private MaterialDialog dialog;
            private View contentView;

            @Override
            public void subscribe(ObservableEmitter<Track> emitter) throws Exception {

                DisposableObserver<Track> disposableObserver = mTrackUploadHandler.uploadTrackObservable(track, getActivity())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableObserver<Track>() {

                            public void onStart() {
//                                emitter.onStart();

                                // Inflate the dialog content view and set the track name.
                                contentView = getActivity().getLayoutInflater().inflate(
                                        R.layout.fragment_tracklist_uploading_single_dialog,
                                        null, false);
                                TextView trackName = contentView.findViewById(
                                        R.id.fragment_tracklist_uploading_single_dialog_trackname);
                                trackName.setText(track.getName());

                                // Create the dialog to show.

                                AndroidSchedulers.mainThread().createWorker().schedule(() -> {
                                    dialog = DialogUtils.createDefaultDialogBuilder(getActivity(),
                                            R.string.track_list_upload_track_uploading,
                                            R.drawable.ic_cloud_upload_white_24dp,
                                            contentView)
                                            .cancelable(false)
                                            .negativeText(R.string.cancel)
                                            .onNegative((materialDialog, dialogAction) -> {
                                                dispose();
                                            })
                                            .show();
                                });

                            }

                            @Override
                            public void onComplete() {
                                emitter.onComplete();

                                if (dialog != null) dialog.dismiss();
                            }

                            @Override
                            public void onError(Throwable e) {
                                LOG.info("onError()");
                                emitter.onError(e);

                                if (dialog != null) dialog.dismiss();
                            }

                            @Override
                            public void onNext(Track track) {
                                LOG.info("onNext() track has been successfully uploaded.");
                                emitter.onNext(track);
                                emitter.onComplete();
                            }
                        });
                emitter.setDisposable(disposableObserver);
            }
        });
    }

    private Observable<Track> uploadTracksWithDialogObservable(List<Track> tracks) {
        return Observable.create(new ObservableOnSubscribe<Track>() {

            private int numberOfTracks = tracks.size();
            private int numberOfSuccesses = 0;
            private int numberOfFailures = 0;
            private MaterialDialog dialog;
            private View contentView;

            @Override
            public void subscribe(ObservableEmitter<Track> emitter) throws Exception {
                DisposableObserver<Track> disposableObserver = mTrackUploadHandler.uploadTracksObservable(tracks, false, getActivity())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableObserver<Track>() {
                            protected TextView infoText;
                            protected ProgressBar progressBar;
                            protected TextView percentageText;
                            protected TextView progressText;

                            @Override
                            public void onStart() {
//                                emitter.onStart();

                                // Create the custom dialog view
                                contentView = getActivity().getLayoutInflater().inflate(R.layout
                                        .fragment_tracklist_uploading_dialog, null, false);

                                infoText = contentView.findViewById(
                                        R.id.fragment_tracklist_uploading_dialog_info);
                                progressBar = contentView.findViewById(
                                        R.id.fragment_tracklist_uploading_dialog_progressbar);
                                percentageText = contentView.findViewById(
                                        R.id.fragment_tracklist_uploading_dialog_percentage);
                                progressText = contentView.findViewById(
                                        R.id.fragment_tracklist_uploading_dialog_progress);

                                // update the Progressbar
                                progressBar.setMax(numberOfTracks);
                                updateProgressView(0);

                                dialog = DialogUtils.createDefaultDialogBuilder(
                                        getActivity(), "Uploading Tracks...",
                                        R.drawable.ic_cloud_upload_white_24dp, contentView)
                                        .cancelable(false)
                                        .negativeText(R.string.cancel)
                                        .onNegative((dialog, dialogAction) -> dispose())
                                        .show();
                            }

                            @Override
                            public void onComplete() {
                                if (!emitter.isDisposed())
                                    emitter.onComplete();

                                if (numberOfFailures > 0) {
                                    showSnackbar(String.format("%s of %s tracks have been " +
                                                    "successfully uploaded. %s tracks had to less" +
                                                    " " +
                                                    "measurements to upload.", numberOfSuccesses,
                                            numberOfTracks, numberOfFailures));
                                }

                                dialog.dismiss();
                            }

                            @Override
                            public void onError(Throwable e) {
                                if (!emitter.isDisposed())
                                    emitter.onError(e);

                                showSnackbar("An error occurred during the upload process.");
                                dialog.dismiss();
                            }

                            @Override
                            public void onNext(Track track) {
                                if (track == null)
                                    numberOfFailures++;
                                else {
                                    numberOfSuccesses++;
                                    emitter.onNext(track);
                                }

                                updateProgressView(numberOfFailures + numberOfSuccesses);

                                if ((numberOfFailures + numberOfSuccesses) == numberOfTracks) {
                                    onComplete();
                                }
                            }

                            private void updateProgressView(int progress) {
                                progressBar.setProgress(progress);
                                progressBar.setSecondaryProgress(progress + 1);

                                percentageText.setText((progress / numberOfTracks) * 100 + "%");
                                progressText.setText(progress + " / " + numberOfTracks);
                            }
                        });
                emitter.setDisposable(disposableObserver);
            }
        });
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

                                ECAnimationUtils.animateShowView(getActivity(), mFAB,
                                        R.anim.translate_slide_in_bottom_fragment);
                            } else if (mTrackList.isEmpty()) {
                                showNoLocalTracksInfo();
                            }
                        }
                    });

            return null;
        }
    }

    private void showNoLocalTracksInfo() {
        showText(R.drawable.img_tracks,
                R.string.track_list_bg_no_local_tracks,
                R.string.track_list_bg_no_local_tracks_sub);
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
