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

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.envirocar.app.BuildConfig;
import org.envirocar.app.R;
import org.envirocar.app.databinding.FragmentTracklistBinding;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.TrackDAOHandler;
import org.envirocar.app.handler.TrackUploadHandler;
import org.envirocar.app.handler.agreement.AgreementManager;
import org.envirocar.app.handler.preferences.UserPreferenceHandler;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.views.utils.ECAnimationUtils;
import org.envirocar.core.EnviroCarDB;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.FileWithMetadata;
import org.envirocar.remote.serde.TrackSerde;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * @author dewall
 */
public abstract class AbstractTrackListCardFragment<E extends RecyclerView.Adapter> extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(AbstractTrackListCardFragment.class);

    private FragmentTracklistBinding binding;

    @Inject
    protected UserPreferenceHandler mUserManager;
    @Inject
    protected EnviroCarDB mEnvirocarDB;
    @Inject
    protected AgreementManager mAgreementManager;
    @Inject
    protected DAOProvider mDAOProvider;
    @Inject
    protected TrackDAOHandler mTrackDAOHandler;
    @Inject
    protected TrackUploadHandler mTrackUploadHandler;

    protected View infoView;
    protected ImageView infoImg;
    protected TextView infoText;
    protected TextView infoSubtext;

    protected View mProgressView;
    protected TextView mProgressText;
    protected ProgressBar mProgressBar;
    protected RecyclerView mRecyclerView;
    protected FloatingActionButton mFAB;

    protected E mRecyclerViewAdapter;
    protected RecyclerView.LayoutManager mRecylcerViewLayoutManager;

    protected boolean tracksLoaded = false;
    protected final List<Track> mTrackList = Collections.synchronizedList(new ArrayList<>());

    // Different workers for main and background threads.
    protected Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    protected Scheduler.Worker mBackgroundWorker = Schedulers.computation().createWorker();

    protected final Object attachingActivityLock = new Object();
    protected boolean isAttached = false;

    private int REQUEST_STORAGE_PERMISSION_REQUEST_CODE = 109;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        binding = FragmentTracklistBinding.inflate(inflater, container, false);
        final View view = binding.getRoot();

        infoView = binding.fragmentTracklistInfo.getRoot();
        infoImg = binding.fragmentTracklistInfo.fragmentTracklistInfoImg;
        infoText = binding.fragmentTracklistInfo.fragmentTracklistInfoText;
        infoSubtext = binding.fragmentTracklistInfo.fragmentTracklistInfoSubtext;
        mProgressView = binding.fragmentTracklistProgressView;
        mProgressText = binding.fragmentTracklistProgressText;
        mProgressBar = binding.fragmentTracklistProgressProgressBar;
        mRecyclerView = binding.fragmentTracklistRecyclerView;
        mFAB = binding.fragmentTracklistFab;

        // Initiate the recyclerview
//        mRecyclerView.setHasFixedSize(true);
        mRecylcerViewLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mRecylcerViewLayoutManager);

        // setup the adapter for the recyclerview.
        mRecyclerViewAdapter = getRecyclerViewAdapter();
        mRecyclerView.setAdapter(mRecyclerViewAdapter);

        // notify the waiting thread that the activity has been attached.
        synchronized (attachingActivityLock) {
            isAttached = true;
            attachingActivityLock.notifyAll();
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * @return
     */
    public abstract E getRecyclerViewAdapter();

    /**
     * This method is responsible for loading the track dataset into the cardlist.
     */
    protected abstract void loadDataset();

    protected abstract void showNoTracksInfo();

    protected void shareTrack(Track track) {

        try {
            // Create export file
            FileWithMetadata trackFile = TrackSerde.createTrackFile(track,
                    String.valueOf(getContext().getExternalCacheDir()));

            // Create an sharing intent.
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("application/json");
            //  Uri shareBody = Uri.fromFile(TrackSerde.exportTrack(track).getFile());
            Uri shareBody = FileProvider.getUriForFile(getContext(),
                    "org.envirocar.app.provider", trackFile.getFile());
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                    "EnviroCar Track " + track.getName());
            sharingIntent.putExtra(android.content.Intent.EXTRA_STREAM, shareBody);
            sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            // Wrap the intent with a chooser.
            startActivity(Intent.createChooser(sharingIntent, "Share via"));

        } catch (IOException e) {
            LOG.warn(e.getMessage(), e);
            Snackbar.make(getView(),
                    R.string.general_error_please_report,
                    Snackbar.LENGTH_LONG).show();
        }
    }


    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        LOG.info("onRequestPermissionResult");
        if (requestCode == REQUEST_STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If getUserStatistic interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                LOG.info("User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                LOG.info("Storage permission granted");
            } else {
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, view -> {
                            // Build intent that displays the App settings screen.
                            Intent intent = new Intent();
                            intent.setAction(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package",
                                    BuildConfig.APPLICATION_ID, null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        });
            }
        }
    }


    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                getActivity().findViewById(R.id.navigation),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * Creates a Dialog for the deletion of a track. On a positive click, the track gets deleted.
     *
     * @param track the track to delete.
     */
    protected void createDeleteTrackDialog(Track track) {
        // Get the up to date reference of the current track.
        // Create a dialog that deletes on click on the positive button the track.
        final Track upToDateRef = mEnvirocarDB.getTrack(track.getTrackID()).blockingFirst();

        View contentView = getActivity().getLayoutInflater().inflate(
                R.layout.fragment_tracklist_delete_track_dialog, null, false);
        ((TextView) contentView.findViewById(
                R.id.fragment_tracklist_delete_track_dialog_trackname)).setText(track.getName());

        // Create a dialog that deletes on click on the positive button the track.
        new MaterialAlertDialogBuilder(getActivity(), R.style.MaterialDialog)
                .setView(contentView)
                .setTitle(R.string.trackviews_delete_track_dialog_headline)
                .setIcon(R.drawable.ic_delete_white_24dp)
                .setPositiveButton(R.string.ok,
                        (materialDialog, dialogAction) ->
                                mBackgroundWorker.schedule(() -> {
                                    // On a positive button click, then delete the track.
                                    if (upToDateRef.isLocalTrack())
                                        deleteLocalTrack(track);
                                    else
                                        deleteRemoteTrack(track);
                                }))
                .setNegativeButton(R.string.cancel,null)
                .show();
    }

    protected void showText(int imgResource, int textResource, int subtextResource) {
        if (mTrackList.isEmpty()) {
            mMainThreadWorker.schedule(() -> {
                infoImg.setImageResource(imgResource);
                infoText.setText(textResource);
                infoSubtext.setText(subtextResource);
                ECAnimationUtils.animateShowView(getActivity(), infoView, R.anim.fade_in);
            });
        }
    }

    protected void deleteRemoteTrack(Track track) {
        LOG.info("deleteRemoteTrack()");

        mEnvirocarDB.getTrack(track.getTrackID())
                .map(upToDateRef -> {
                    if (upToDateRef.isLocalTrack()) {
                        LOG.info("Track to delete is a local track");
                        return false;
                    }

                    try {
                        mTrackDAOHandler.deleteRemoteTrack(upToDateRef);
                        return true;
                    } catch (Exception e) {
                        throw e;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getDeleteTrackSubscriber(track));
    }

    /**
     * Deletes a local track in the database.
     *
     * @param track
     */
    protected void deleteLocalTrack(final Track track) {
        // Get the up to date reference of the current track and delete it
        Observable.defer(() -> mEnvirocarDB.getTrack(track.getTrackID()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(upToDateRef -> {
                    // If the track is a local track, then delete and return whether it was
                    // successful.
                    return upToDateRef.isLocalTrack() &&
                            mTrackDAOHandler.deleteLocalTrack(upToDateRef.getTrackID());
                })
                .subscribe(getDeleteTrackSubscriber(track));
    }

    protected DisposableObserver<Boolean> getDeleteTrackSubscriber(final Track track) {
        return new DisposableObserver<Boolean>() {
            @Override
            public void onStart() {
                LOG.info(String.format("onStart() delete track -> [%s]", track.getName()));
                showProgressView(getString(R.string.track_list_deleting_track));
            }

            @Override
            public void onComplete() {
                LOG.info(String.format("onCompleted() delete track -> [%s]",
                        track.getName()));
            }

            @Override
            public void onError(Throwable e) {
                LOG.error(String.format("onError() delete track -> [%s]",
                        track.getName()), e);

                if (e instanceof UnauthorizedException) {
                    LOG.error("The logged in getUserStatistic is not authorized to do that.", e);
                    showSnackbar(R.string.track_list_deleting_track_unauthorized);
                } else if (e instanceof NotConnectedException) {
                    LOG.error("Not connected", e);
                    showSnackbar(R.string.track_list_communication_error);
                } else {
                    showSnackbar(String.format(
                            getString(R.string.track_list_delete_track_error_template),
                            track.getName()));
                }

                hideProgressView();
            }

            @Override
            public void onNext(Boolean success) {
                LOG.info("onNext() -> " + track.getName());
                if (success) {
                    LOG.info("deleteLocalTrack: Successfully deleted track with" +
                            " id=" + track.getTrackID());

                    mTrackList.remove(track);
                    mRecyclerViewAdapter.notifyDataSetChanged();
                    showSnackbar(String.format(getString(R.string
                            .track_list_delete_track_success_template), track.getName()));
                    hideProgressView();

                    if (mTrackList.isEmpty()) {
                        showNoTracksInfo();
                    }
                } else {
                    showSnackbar(String.format(
                            getString(R.string.track_list_delete_track_error_template),
                            track.getName()));
                }
            }
        };
    }

    protected void showSnackbar(final int message) {
        mMainThreadWorker.schedule(() -> {
            if (getView() != null) {
                Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    protected void showSnackbar(final String message) {
        mMainThreadWorker.schedule(() -> {
            if (getView() != null) {
                Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    protected void showProgressView(String text) {
        mMainThreadWorker.schedule(() -> {
            mProgressView.setVisibility(View.VISIBLE);
            mProgressText.setText(text);
        });
    }

    protected void hideProgressView() {
        ECAnimationUtils.animateHideView(getActivity(), mProgressView, R.anim.fade_out);
    }
}
