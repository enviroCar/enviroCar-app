package org.envirocar.app.view.tracklist;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.collect.Lists;

import org.envirocar.app.R;
import org.envirocar.app.TrackHandler;
import org.envirocar.app.handler.TermsOfUseManager;
import org.envirocar.app.handler.UserHandler;
import org.envirocar.app.injection.DAOProvider;
import org.envirocar.app.handler.PreferenceConstants;
import org.envirocar.app.view.utils.ECAnimationUtils;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.injection.BaseInjectorFragment;
import org.envirocar.core.injection.Injector;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.serializer.TrackSerializer;
import org.envirocar.storage.EnviroCarDB;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * @author dewall
 */
public abstract class AbstractTrackListCardFragment<E extends RecyclerView.Adapter>
        extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(AbstractTrackListCardFragment.class);

    @Inject
    protected UserHandler mUserManager;
    @Inject
    protected EnviroCarDB mEnvirocarDB;
    @Inject
    protected TermsOfUseManager mTermsOfUseManager;
    @Inject
    protected DAOProvider mDAOProvider;
    @Inject
    protected TrackHandler mTrackHandler;

    @InjectView(R.id.fragment_tracklist_notification)
    protected TextView mTextView;
    @InjectView(R.id.fragment_tracklist_progress_view)
    protected View mProgressView;
    @InjectView(R.id.fragment_tracklist_progress_text)
    protected TextView mProgressText;
    @InjectView(R.id.fragment_tracklist_progress_progressBar)
    protected ProgressBar mProgressBar;
    @InjectView(R.id.fragment_tracklist_recycler_view)
    protected RecyclerView mRecyclerView;

    protected E mRecyclerViewAdapter;
    protected RecyclerView.LayoutManager mRecylcerViewLayoutManager;

    protected boolean tracksLoaded = false;
    protected final List<Track> mTrackList = Collections.synchronizedList(Lists.newArrayList());

    // Different workers for main and background threads.
    protected Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    protected Scheduler.Worker mBackgroundWorker = Schedulers.computation().createWorker();

    protected Object attachingActivityLock = new Object();
    protected boolean isAttached = false;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((Injector) activity).injectObjects(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Inflate the view and inject the annotated view.
        View view = inflater.inflate(R.layout.fragment_tracklist, container, false);
        ButterKnife.inject(this, view);

        // Initiate the recyclerview
        mRecyclerView.setHasFixedSize(true);
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

    /**
     * @return
     */
    public abstract E getRecyclerViewAdapter();

    /**
     * This method is responsible for loading the track dataset into the cardlist.
     */
    protected abstract void loadDataset();

    protected void exportTrack(Track track) {
        // First get the obfuscation setting from the shared preferences
        boolean isObfuscationEnabled =
                PreferenceManager
                        .getDefaultSharedPreferences(getActivity())
                        .getBoolean(PreferenceConstants.OBFUSCATE_POSITION, false);

        try {
            // Create an sharing intent.
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("application/json");
            Uri shareBody = Uri.fromFile(TrackSerializer.exportTrack(track).getFile());

            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                    "EnviroCar Track " + track.getName());
            sharingIntent.putExtra(android.content.Intent.EXTRA_STREAM, shareBody);

            // Wrap the intent with a chooser.
            startActivity(Intent.createChooser(sharingIntent, "Share via"));
            //        } catch (TrackWithoutMeasurementsException e) {
            //            LOG.warn(e.getMessage(), e);
            //            Snackbar.make(getView(), R.string.error_json, Snackbar.LENGTH_LONG)
            // .show();
//        } catch (JSONException e) {
//            LOG.warn(e.getMessage(), e);
//            Snackbar.make(getView(), R.string.error_io, Snackbar.LENGTH_LONG).show();
        } catch (IOException e) {
            LOG.warn(e.getMessage(), e);
            if (isObfuscationEnabled) {
                Snackbar.make(getView(),
                        R.string.uploading_track_no_measurements_after_obfuscation_long,
                        Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(getView(),
                        R.string.uploading_track_no_measurements_after_obfuscation_long,
                        Snackbar.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Creates a Dialog for the deletion of a track. On a positive click, the track gets deleted.
     *
     * @param track the track to delete.
     */
    protected void createDeleteTrackDialog(Track track) {
        // Get the up to date reference of the current track.
        // Create a dialog that deletes on click on the positive button the track.
        final Track upToDateRef = mEnvirocarDB.getTrack(track.getTrackID())
                .toBlocking()
                .first();

        // Create a dialog that deletes on click on the positive button the track.
        new MaterialDialog.Builder(getActivity())
                .title(R.string.trackviews_delete_track_dialog_headline)
                .content(String.format(getResources().getString(R.string
                        .trackviews_delete_track_dialog_content), track.getName()))
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        mBackgroundWorker.schedule(() -> {
                            // On a positive button click, then delete the track.
                            if (upToDateRef.isLocalTrack())
                                deleteLocalTrack(track);
                            else
                                deleteRemoteTrack(track);
                        });
                    }
                })
                .show();
    }

    protected void showText(String text) {
        if (mTrackList.isEmpty()) {
            mMainThreadWorker.schedule(new Action0() {
                @Override
                public void call() {
                    mTextView.setVisibility(View.VISIBLE);
                    mTextView.setText(text);
                }
            });
        }
    }

    protected void deleteRemoteTrack(Track track) {
        LOG.info("deleteRemoteTrack()");

        mEnvirocarDB.getTrack(track.getTrackID())
                .map(new Func1<Track, Boolean>() {
                    @Override
                    public Boolean call(Track upToDateRef) {
                        if (upToDateRef.isLocalTrack()) {
                            LOG.info("Track to delete is a local track");
                            return false;
                        }

                        try {
                            mTrackHandler.deleteRemoteTrack(upToDateRef);
                            return true;
                        } catch (Exception e) {
                            OnErrorThrowable.from(e);
                        }
                        return false;
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
        mEnvirocarDB.getTrack(track.getTrackID())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(upToDateRef -> {
                    // If the track is a local track, then delete and return whether it was
                    // successful.
                    return upToDateRef.isLocalTrack() && mTrackHandler.deleteLocalTrack
                            (upToDateRef.getTrackID());
                })
                .subscribe(getDeleteTrackSubscriber(track));
    }

    protected Subscriber<Boolean> getDeleteTrackSubscriber(final Track track) {
        return new Subscriber<Boolean>() {
            @Override
            public void onStart() {
                LOG.info(String.format("onStart() delete track -> [%s]", track.getName()));
                showProgressView("Deleting Track...");
            }

            @Override
            public void onCompleted() {
                LOG.info(String.format("onCompleted() delete track -> [%s]",
                        track.getName()));

            }

            @Override
            public void onError(Throwable e) {
                LOG.error(String.format("onError() delete track -> [%s]",
                        track.getName()), e);

                if (e instanceof UnauthorizedException) {
                    LOG.error("The logged in user is not authorized to do that.", e);
                    showSnackbar("Unable to delete the track, because the " +
                            "user is not authorized to do that");
                } else if (e instanceof NotConnectedException) {
                    LOG.error("Not connected", e);
                    showSnackbar("Unable to communicate with the server");
                } else {
                    showSnackbar(String.format(
                            "Error while deleting track \"%s\".", track.getName()));
                }

                hideProgressView();
            }

            @Override
            public void onNext(Boolean success) {
                LOG.info("onNext() -> " + track.getName());
                if (success) {
                    LOG.info("deleteLocalTrack: Successfully delete track with" +
                            " id=" + track.getTrackID());

                    mTrackList.remove(track);
                    mRecyclerViewAdapter.notifyDataSetChanged();
                    showSnackbar(String.format(
                            "%s has been successfully deleted.", track.getName()));
                    hideProgressView();
                } else {
                    showSnackbar(String.format(
                            "Error while deleting track \"%s\".", track.getName()));
                }
            }
        };
    }

    protected void showSnackbar(final String message) {
        mMainThreadWorker.schedule(new Action0() {
            @Override
            public void call() {
                if (getView() != null) {
                    Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
                }
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
        ECAnimationUtils.animateHideView(getContext(), mProgressView, R.anim.fade_out);
    }
}
