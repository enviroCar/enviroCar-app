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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Slide;
import androidx.transition.TransitionManager;

import com.google.android.material.snackbar.Snackbar;

import android.view.Gravity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.base.Preconditions;

import org.envirocar.app.main.BaseApplication;
import org.envirocar.app.main.MainActivityComponent;
import org.envirocar.app.main.MainActivityModule;
import org.envirocar.app.R;
import org.envirocar.app.views.trackdetails.TrackDetailsActivity;
import org.envirocar.app.views.utils.DialogUtils;
import org.envirocar.app.views.utils.ECAnimationUtils;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.trackprocessing.statistics.TrackStatisticsProvider;
import org.envirocar.core.util.TrackMetadata;
import org.envirocar.core.util.Util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import rx.Observable;
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

    private boolean dateFilter = false;
    private boolean carFilter = false;
    private Date startDate;
    private Date endDate;
    private String carName;
    private Integer sortC = 0;
    private Integer sortO = 1;
    private FilterViewModel filterViewModel;
    private SortViewModel sortViewModel;
    private List<Track> localList = Collections.synchronizedList(new ArrayList<>());
    private Boolean mvVisible = true;

    /**
     *
     */
    interface OnTrackUploadedListener {
        void onTrackUploaded(Track track);
    }

    private OnTrackUploadedListener onTrackUploadedListener;

    private Subscription loadTracksSubscription;
    private Subscription uploadTrackSubscription;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivityComponent mainActivityComponent =  BaseApplication.get(getActivity()).getBaseApplicationComponent().plus(new MainActivityModule(getActivity()));
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
            if(sortViewModel.getSortOrder().getValue())
                sortO = 1;
            else
                sortO = -1;
            updateView();
        });

        sortViewModel.getMapActive().observe(this, item->{
            mvVisible = sortViewModel.getMapChoice().getValue();
            updateView();
        });
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

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    // Scroll Down
                    if (mFAB.isShown()) {
                        mFAB.hide();
                    }
                } else if (dy < 0) {
                    // Scroll Up
                    if (!mFAB.isShown()) {
                        mFAB.show();
                    }
                }
            }
        });
    }

    private void uploadAllLocalTracks() {
        uploadTrackSubscription = Observable.defer(() -> mEnvirocarDB.getAllLocalTracks())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .first()
                .concatMap(tracks -> uploadTracksWithDialogObservable(tracks))
                .subscribe(new Subscriber<Track>() {
                    @Override
                    public void onCompleted() {
                        LOG.info("onCompleted()");
                        if (mRecyclerViewAdapter.getItemCount() <= 0) {
                            showNoLocalTracksInfo(false);
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
                .subscribe(new Subscriber<Track>() {
                    @Override
                    public void onCompleted() {
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
                if(track.hasProperty(Measurement.PropertyKey.SPEED)){
                    // Upload the track
                    uploadTrack(track);
                }else {
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
        if(!tracksLoaded)
        {
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

    private Observable<Track> uploadTrackWithDialogObservable(Track track) {
        Preconditions.checkNotNull(track, "Input track cannot be null");
        return Observable.create(new Observable.OnSubscribe<Track>() {
            private MaterialDialog dialog;
            private View contentView;

            @Override
            public void call(Subscriber<? super Track> subscriber) {
                subscriber.add(mTrackUploadHandler.uploadTrackObservable(track, getActivity())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Subscriber<Track>() {
                            @Override
                            public void onStart() {
                                subscriber.onStart();

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
                                                subscriber.unsubscribe();
                                                unsubscribe();
                                            })
                                            .show();
                                });

                            }

                            @Override
                            public void onCompleted() {
                                subscriber.onCompleted();

                                if (dialog != null) dialog.dismiss();
                            }

                            @Override
                            public void onError(Throwable e) {
                                LOG.info("onError()");
                                subscriber.onError(e);

                                if (dialog != null) dialog.dismiss();
                            }

                            @Override
                            public void onNext(Track track) {
                                LOG.info("onNext() track has been successfully uploaded.");
                                subscriber.onNext(track);
                                subscriber.onCompleted();
                            }
                        }));
            }
        });
    }

    private Observable<Track> uploadTracksWithDialogObservable(List<Track> tracks) {
        return Observable.create(new Observable.OnSubscribe<Track>() {
            private int numberOfTracks = tracks.size();
            private int numberOfSuccesses = 0;
            private int numberOfFailures = 0;
            private MaterialDialog dialog;
            private View contentView;

            @Override
            public void call(Subscriber<? super Track> subscriber) {
                subscriber.add(mTrackUploadHandler.uploadTracksObservable(tracks, false,
                        getActivity())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Subscriber<Track>() {
                            protected TextView infoText;
                            protected ProgressBar progressBar;
                            protected TextView percentageText;
                            protected TextView progressText;

                            @Override
                            public void onStart() {
                                subscriber.onStart();

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
                                        .onNegative((dialog, dialogAction) -> {
                                            subscriber.unsubscribe();
                                            unsubscribe();
                                        })
                                        .show();
                            }

                            @Override
                            public void onCompleted() {
                                if (!subscriber.isUnsubscribed())
                                    subscriber.onCompleted();

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
                                if (!subscriber.isUnsubscribed())
                                    subscriber.onError(e);

                                showSnackbar("An error occurred during the upload process.");
                                dialog.dismiss();
                            }

                            @Override
                            public void onNext(Track track) {
                                if (track == null)
                                    numberOfFailures++;
                                else {
                                    numberOfSuccesses++;
                                    subscriber.onNext(track);
                                }

                                updateProgressView(numberOfFailures + numberOfSuccesses);

                                if ((numberOfFailures + numberOfSuccesses) == numberOfTracks) {
                                    onCompleted();
                                }
                            }

                            private void updateProgressView(int progress) {
                                progressBar.setProgress(progress);
                                progressBar.setSecondaryProgress(progress + 1);

                                percentageText.setText((progress / numberOfTracks) * 100 + "%");
                                progressText.setText(progress + " / " + numberOfTracks);
                            }
                        }));
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
                    .subscribe(new Subscriber<List<Track>>() {

                        @Override
                        public void onStart() {
                            LOG.info("onStart() allLocalTracks");
                            showProgressView(R.string.track_list_loading_tracks);
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
                                if (!localList.contains(track)) {
                                    localList.add(track);
                                    newTrackAdded = true;
                                }
                            }
                            setTrackList();
                            hideProgressView();
                            if (newTrackAdded) {
                                updateView();
                            } else if (mTrackList.isEmpty()) {
                                showNoLocalTracksInfo(false);
                            }
                        }
                    });

            return null;
        }
    }

    public void setTrackList(){
        for(Track track : localList){
            if(!mTrackList.contains(track))
                mTrackList.add(track);
        }
    }

    public  void updateView(){

        mRecyclerViewAdapter.setGuideline(mvVisible);
        setTrackList();
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

        switch (sortC){
            case 0:
                Collections.sort(mTrackList, new Comparator<Track>() {
                    @Override
                    public int compare(Track track1, Track track2)
                    {
                        int res = track1.getBegin().compareTo(track2.getBegin());
                        return res*sortO;
                    }
                });break;

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
                        LOG.info(res + " result of compare");

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

        if (mTrackList.isEmpty()) {
            showNoLocalTracksInfo(true);
        }
        else{
            TransitionManager.beginDelayedTransition(mRecyclerView, new Slide(Gravity.LEFT).
                    setDuration(1000).
                    setInterpolator(new FastOutSlowInInterpolator()));
            mRecyclerView.removeAllViews();
            mRecyclerView.setVisibility(View.VISIBLE);
            infoView.setVisibility(View.GONE);
            mRecyclerViewAdapter.notifyDataSetChanged();
            ECAnimationUtils.animateShowView(getActivity(), mFAB,
                    R.anim.translate_slide_in_bottom_fragment);
        }

    }
    private void showNoLocalTracksInfo(Boolean afterFilter) {
        mRecyclerView.setVisibility(View.GONE);
        if(!afterFilter) {
            showText(R.drawable.img_tracks,
                    R.string.track_list_bg_no_local_tracks,
                    R.string.track_list_bg_no_local_tracks_sub);
        } else {
            showText(R.drawable.img_tracks,
                    R.string.track_list_bg_no_tracks_after_filter,
                    R.string.track_list_bg_no_tracks_after_filter_sub);
        }
        infoView.setVisibility(View.VISIBLE);
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
