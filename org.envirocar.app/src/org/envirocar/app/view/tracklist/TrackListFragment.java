package org.envirocar.app.view.tracklist;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.R;
import org.envirocar.app.TrackHandler;
import org.envirocar.app.activity.ListTracksFragment;
import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.application.TermsOfUseManager;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.json.TrackWithoutMeasurementsException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.dao.DAOProvider;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.RemoteTrack;
import org.envirocar.app.storage.Track;
import org.envirocar.app.util.Util;
import org.envirocar.app.view.trackdetails.TrackDetailsActivity;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

/**
 * @author dewall
 */
public class TrackListFragment extends BaseInjectorFragment {
    private static final Logger LOGGER = Logger.getLogger(ListTracksFragment.class);

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

    @InjectView(R.id.fragment_tracklist_recycler_view)
    protected RecyclerView mRecyclerView;
    protected RecyclerView.Adapter mRecyclerViewAdapter;
    protected RecyclerView.LayoutManager mRecylcerViewLayoutManager;

    private Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();

    private List<Track> mTrackList = Collections.synchronizedList(new ArrayList<Track>());

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_tracklist, container, false);

        ButterKnife.inject(this, view);
        mRecyclerView.setHasFixedSize(true);

        mRecylcerViewLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mRecylcerViewLayoutManager);

        mRecyclerViewAdapter = new TrackListLocalCardAdapter(mTrackList,
                mOnTrackInteractionCallback);
        mRecyclerView.setAdapter(mRecyclerViewAdapter);

        mRecyclerView.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), TrackDetailsActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        new RemoteDownloadTracksTask().execute();
    }

    private OnTrackInteractionCallback mOnTrackInteractionCallback =
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
                    LOGGER.info(String.format("onTrackDetailsClicked(%s)", track.getTrackId()
                            .toString()));
                    int trackID = (int) track.getTrackId().getId();
                    TrackDetailsActivity.navigate(getActivity(), transitionView, trackID);
                }

                @Override
                public void onDeleteTrackClicked(Track track) {
                    LOGGER.info(String.format("onDeleteTrackClicked(%s)", track.getTrackId()));
                    // create a dialog
                    createDeleteTrackDialog(track);
                }

                @Override
                public void onUploadTrackClicked(Track track) {
                    LOGGER.info(String.format("onUploadTrackClicked(%s)", track.getTrackId()));
                    // Upload the track
                    uploadTrack(track);
                }

                @Override
                public void onExportTrackClicked(Track track) {
                    LOGGER.info(String.format("onExportTrackClicked(%s)", track.getTrackId()));
                    exportTrack(track);
                }

                @Override
                public void onDownloadTrackClicked(RemoteTrack track, AbstractTrackListCardAdapter
                        .TrackCardViewHolder holder) {

                }


            };

    private void uploadTrack(Track track) {
        mTrackHandler.uploadTrack(getActivity(), track, new TrackHandler
                .TrackUploadCallback() {

            private MaterialDialog mProgressDialog;

            @Override
            public void onUploadStarted(Track track) {
                mMainThreadWorker.schedule(() ->
                        mProgressDialog = new MaterialDialog.Builder(getActivity())
                                .title("Progress Dialog")
                                .content("Please wait...")
                                .progress(true, 0)
                                .show());
            }

            @Override
            public void onSuccessfulUpload(Track track) {
                if (mProgressDialog != null)
                    mProgressDialog.dismiss();
                Snackbar.make(getView(), "Track upload was successful", Snackbar
                        .LENGTH_LONG).show();

                // Update the lists.
            }

            @Override
            public void onError(Track track, String message) {
                if (mProgressDialog != null)
                    mProgressDialog.dismiss();
                Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Creates a Dialog for the deletion of a track. On a positive click, the track gets deleted.
     *
     * @param track the track to delete.
     */
    private void createDeleteTrackDialog(Track track) {
        // Get the up to date reference of the current track.
        Track upToDateRef = mDBAdapter.getTrack(track.getTrackId(), true);

        // If the track is a local track
        if (upToDateRef.isLocalTrack()) {
            new MaterialDialog.Builder(getActivity())
                    .title(R.string.trackviews_delete_track_dialog_headline)
                    .content(String.format(getResources().getString(R.string
                            .trackviews_delete_track_dialog_content), track.getName()))
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            // On a positive button click, then delete the track.
                            deleteTrack(track);
                        }
                    })
                    .show();
        } else {

        }
    }

    private void deleteTrack(Track track) {
        // Get the up to date reference of the current track.
        Track upToDateRef = mDBAdapter.getTrack(track.getTrackId(), true);

        // If the track has been successfully deleted.
        if (upToDateRef.isLocalTrack() && mTrackHandler.deleteLocalTrack(upToDateRef.getTrackId())) {
            // Show a snackbar notification
            Snackbar.make(getView(), R.string
                            .trackviews_delete_track_snackbar_success,
                    Snackbar.LENGTH_LONG).show();

            // and update the view elements
            mTrackHandler.deleteLocalTrack(upToDateRef);
            mTrackList.remove(track);
            mRecyclerViewAdapter.notifyDataSetChanged();

            LOGGER.info("deleteLocalTrack: Successfully delete track with id=" + track.getTrackId());
        }
    }

    private void exportTrack(Track track) {
        // First get the obfuscation setting from the shared preferences
        boolean isObfuscationEnabled =
                PreferenceManager
                        .getDefaultSharedPreferences(getActivity())
                        .getBoolean(SettingsActivity.OBFUSCATE_POSITION, false);

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
        } catch (TrackWithoutMeasurementsException e) {
            LOGGER.warn(e.getMessage(), e);
            Snackbar.make(getView(), R.string.error_json, Snackbar.LENGTH_LONG).show();
        } catch (JSONException e) {
            LOGGER.warn(e.getMessage(), e);
            Snackbar.make(getView(), R.string.error_io, Snackbar.LENGTH_LONG).show();
        } catch (IOException e) {
            LOGGER.warn(e.getMessage(), e);
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

    private final class RemoteDownloadTracksTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Thread.currentThread().setName("TrackList-TrackRetriever" + Thread.currentThread()
                    .getId());

            //fetch db tracks (local+remote)
            List<Track> tracks = mDBAdapter.getAllTracks(true);
            for (Track t : tracks) {
                mTrackList.add(t);
            }

            Collections.sort(mTrackList);

            getActivity().runOnUiThread(() -> mRecyclerViewAdapter.notifyDataSetChanged());


            //            if (mUserManager.isLoggedIn()) {
            //                setProgressStatusText(R.string.fetching_tracks_remote);
            //                downloadTracks();
            //            } else {
            //                updateStatusLayout();
            //            }

            return null;
        }
    }
}
