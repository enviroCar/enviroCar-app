package org.envirocar.app.view.tracklist;

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
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.collect.Lists;

import org.envirocar.app.R;
import org.envirocar.app.TrackHandler;
import org.envirocar.app.application.TermsOfUseManager;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.injection.DAOProvider;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.view.preferences.PreferenceConstants;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.injection.BaseInjectorFragment;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.Util;
import org.json.JSONException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @author dewall
 */
public abstract class AbstractTrackListCardFragment<T extends Track, E extends RecyclerView.Adapter>
        extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(AbstractTrackListCardFragment.class);

    @Inject
    protected UserManager mUserManager;
    @Inject
    protected DbAdapter mDBAdapter;
    @Inject
    protected TermsOfUseManager mTermsOfUseManager;
    @Inject
    protected DAOProvider mDAOProvider;
    @Inject
    protected TrackHandler mTrackHandler;

    @InjectView(R.id.fragment_tracklist_notification)
    protected TextView mTextView;
    @InjectView(R.id.fragment_tracklist_recycler_view)
    protected RecyclerView mRecyclerView;
    protected E mRecyclerViewAdapter;
    protected RecyclerView.LayoutManager mRecylcerViewLayoutManager;

    protected final List<T> mTrackList = Collections.synchronizedList(Lists.newArrayList());

    // Different workers for main and background threads.
    protected Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    protected Scheduler.Worker mBackgroundWorker = Schedulers.computation().createWorker();


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

        return view;
    }

    /**
     * @return
     */
    public abstract E getRecyclerViewAdapter();

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
            Uri shareBody = Uri.fromFile(Util.saveTrackAndReturnFile(track,
                    isObfuscationEnabled).getFile());
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "EnviroCar Track " +
                    track.getName());
            sharingIntent.putExtra(android.content.Intent.EXTRA_STREAM, shareBody);

            // Wrap the intent with a chooser.
            startActivity(Intent.createChooser(sharingIntent, "Share via"));
            //        } catch (TrackWithoutMeasurementsException e) {
            //            LOG.warn(e.getMessage(), e);
            //            Snackbar.make(getView(), R.string.error_json, Snackbar.LENGTH_LONG)
            // .show();
        } catch (JSONException e) {
            LOG.warn(e.getMessage(), e);
            Snackbar.make(getView(), R.string.error_io, Snackbar.LENGTH_LONG).show();
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
        final Track upToDateRef = mDBAdapter.getTrack(track.getTrackID(), true);

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

    protected void deleteRemoteTrack(Track track) {
        LOG.info("deleteRemoteTrack()");

        // Get the up to date reference of the current track.
        Track upToDateRef = mDBAdapter.getTrack(track.getTrackID(), true);

        if (!upToDateRef.isRemoteTrack()) {
            return;
        }
        try {
            mTrackHandler.deleteRemoteTrack(upToDateRef);

            // Show a snackbar notification
            Snackbar.make(getView(), R.string.trackviews_delete_track_snackbar_success,
                    Snackbar.LENGTH_LONG).show();

            // and update the view elements
            mMainThreadWorker.schedule(() -> {
                mTrackList.remove(track);
                mRecyclerViewAdapter.notifyDataSetChanged();
            });

        } catch (UnauthorizedException e) {
            LOG.severe("The logged in user is not authorized to do that.", e);
            Snackbar.make(getView(), "Unable to delete the track, because the user is not " +
                    "authorized to do that", Snackbar.LENGTH_LONG).show();
        } catch (NotConnectedException e) {
            LOG.severe("Not connected", e);
            Snackbar.make(getView(), "Unable to communicate with the server",
                    Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * Deletes a local track in the database.
     *
     * @param track
     */
    protected void deleteLocalTrack(Track track) {
        // Get the up to date reference of the current track.
        Track upToDateRef = mDBAdapter.getTrack(track.getTrackID(), true);

        // If the track has been successfully deleted.
        if (upToDateRef.isLocalTrack() && mTrackHandler
                .deleteLocalTrack(upToDateRef.getTrackID())) {
            // Show a snackbar notification
            Snackbar.make(getView(), R.string.trackviews_delete_track_snackbar_success,
                    Snackbar.LENGTH_LONG).show();

            mMainThreadWorker.schedule(() -> {
                // and update the view elements
                mTrackList.remove(track);
                mRecyclerViewAdapter.notifyDataSetChanged();
            });

            LOG.info("deleteLocalTrack: Successfully delete track with id=" + track.getTrackID());
        }
    }
}
