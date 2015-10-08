package org.envirocar.app.view.tracklist;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.TrackHandler;
import org.envirocar.app.view.trackdetails.TrackDetailsActivity;
import org.envirocar.core.entity.Track;
import org.envirocar.core.logging.Logger;

import java.util.Collections;
import java.util.List;

/**
 * @author dewall
 */
public class TrackListLocalCardFragment extends AbstractTrackListCardFragment<Track,
        TrackListLocalCardAdapter> {
    private static final Logger LOGGER = Logger.getLogger(TrackListLocalCardFragment.class);

    /**
     *
     */
    interface OnTrackUploadedListener{
        void onTrackUploaded(Track track);
    }

    private OnTrackUploadedListener onTrackUploadedListener;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        new LoadLocalTracksTask().execute();
    }

    private void uploadTrack(Track track) {
        mTrackHandler.uploadTrack(getActivity(), track, new TrackHandler.TrackUploadCallback() {

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
                if (mProgressDialog != null) mProgressDialog.dismiss();
                Snackbar.make(getView(), "Track upload was successful", Snackbar
                        .LENGTH_LONG).show();

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
                Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
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
                LOGGER.info(String.format("onTrackDetailsClicked(%s)", track.getTrackID()
                        .toString()));
                int trackID = (int) track.getTrackID().getId();
                TrackDetailsActivity.navigate(getActivity(), transitionView, trackID);
            }

            @Override
            public void onDeleteTrackClicked(Track track) {
                LOGGER.info(String.format("onDeleteTrackClicked(%s)", track.getTrackID()));
                // create a dialog
                createDeleteTrackDialog(track);
            }

            @Override
            public void onUploadTrackClicked(Track track) {
                LOGGER.info(String.format("onUploadTrackClicked(%s)", track.getTrackID()));
                // Upload the track
                uploadTrack(track);
            }

            @Override
            public void onExportTrackClicked(Track track) {
                LOGGER.info(String.format("onExportTrackClicked(%s)", track.getTrackID()));
                exportTrack(track);
            }

            @Override
            public void onDownloadTrackClicked(Track track, AbstractTrackListCardAdapter
                    .TrackCardViewHolder holder) {
                // NOT REQUIRED
            }
        });
    }

    private final class LoadLocalTracksTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Thread.currentThread().setName("TrackList-TrackRetriever" + Thread.currentThread()
                    .getId());

            //fetch db tracks (local+remote)
            List<Track> tracks = mDBAdapter.getAllLocalTracks(true);
            for (Track t : tracks) {
                mTrackList.add(t);
            }

            Collections.sort(mTrackList);

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mTrackList.isEmpty()) {
                        mTextView.setText("No Local Tracks");
                        mTextView.setVisibility(View.VISIBLE);
                    } else {
                        mTextView.setVisibility(View.GONE);
                        mRecyclerViewAdapter.notifyDataSetChanged();
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
    public void setOnTrackUploadedListener(OnTrackUploadedListener listener){
        this.onTrackUploadedListener = listener;
    }
}
