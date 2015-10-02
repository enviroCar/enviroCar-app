package org.envirocar.app.view.tracklist;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.envirocar.app.R;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.dao.exception.NotConnectedException;
import org.envirocar.app.model.dao.exception.UnauthorizedException;
import org.envirocar.app.storage.RemoteTrack;
import org.envirocar.app.storage.Track;
import org.envirocar.app.view.trackdetails.TrackDetailsActivity;
import org.envirocar.app.view.utils.ECAnimationUtils;

import java.util.Collections;
import java.util.List;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @author dewall
 */
public class TrackListRemoteCardFragment extends AbstractTrackListCardFragment<RemoteTrack,
        TrackListRemoteCardAdapter> implements TrackListLocalCardFragment.OnTrackUploadedListener {
    private static final Logger LOG = Logger.getLogger(TrackListRemoteCardFragment.class);

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mUserManager.isLoggedIn()) {
            mRecyclerViewAdapter.mTrackDataset.clear();
            mRecyclerView.setVisibility(View.VISIBLE);
            mTextView.setVisibility(View.GONE);
            new LoadRemoteTracksTask().execute();
        } else {
            mTextView.setText("Not Logged In!");
            mRecyclerView.setVisibility(View.GONE);
            mRecyclerViewAdapter.mTrackDataset.clear();
            ECAnimationUtils.animateShowView(getContext(), mTextView, R.anim.fade_in);
        }

    }

    @Override
    public TrackListRemoteCardAdapter getRecyclerViewAdapter() {
        return new TrackListRemoteCardAdapter(getContext(), mTrackList,
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
                        LOG.info(String.format("onTrackDetailsClicked(%s)", track.getTrackId()
                                .toString()));
                        int trackID = (int) track.getTrackId().getId();
                        TrackDetailsActivity.navigate(getActivity(), transitionView, trackID);
                    }

                    @Override
                    public void onDeleteTrackClicked(Track track) {
                        LOG.info(String.format("onDeleteTrackClicked(%s)", track.getTrackId()));
                        // create a dialog
                        createDeleteTrackDialog(track);
                    }

                    @Override
                    public void onUploadTrackClicked(Track track) {
                        LOG.info(String.format("onUploadTrackClicked(%s)", track.getTrackId()));
                        // Upload the track
                        LOG.warn("onUploadTrackClicked() on remote tracks has no effect.");
                    }

                    @Override
                    public void onExportTrackClicked(Track track) {
                        LOG.info(String.format("onExportTrackClicked(%s)", track.getTrackId()));
                        exportTrack(track);
                    }

                    @Override
                    public void onDownloadTrackClicked(
                            RemoteTrack track, AbstractTrackListCardAdapter
                            .TrackCardViewHolder viewHolder) {
                        onDownloadTrackClickedInner(track, viewHolder);
                    }
                });
    }

    private void onDownloadTrackClickedInner(RemoteTrack track, AbstractTrackListCardAdapter
            .TrackCardViewHolder viewHolder) {
        AbstractTrackListCardAdapter.RemoteTrackCardViewHolder holder =
                (AbstractTrackListCardAdapter.RemoteTrackCardViewHolder) viewHolder;

        // Show the downloading text notification.
        ECAnimationUtils.animateShowView(getContext(), holder.mDownloadNotification,
                R.anim.fade_in);
        holder.mProgressCircle.show();

        mTrackHandler.fetchRemoteTrackObservable(track)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<RemoteTrack>() {
                    @Override
                    public void onCompleted() {
                        holder.mProgressCircle.beginFinalAnimation();
                        holder.mState = AbstractTrackListCardAdapter.RemoteTrackCardViewHolder
                                .DownloadState.DOWNLOADED;
                        holder.mProgressCircle.attachListener(() -> {
                            // When the visualization is finished, then Init the
                            // content view including its mapview and track details.
                            mRecyclerViewAdapter.bindLocalTrackViewHolder(holder, track);

                            // and hide the download button
                            ECAnimationUtils.animateHideView(getContext(), R.anim.fade_out,
                                    holder.mProgressCircle, holder.mDownloadButton, holder
                                            .mDownloadNotification);
                            ECAnimationUtils.animateShowView(getContext(), holder.mContentView, R
                                    .anim.fade_in);
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error("Not connected exception", e);
                        Snackbar.make(getView(), "Not Connected Exception", Snackbar
                                .LENGTH_LONG).show();
                        holder.mProgressCircle.hide();
                        holder.mDownloadNotification.setText("Error while downloading..");
                    }

                    @Override
                    public void onNext(RemoteTrack remoteTrack) {
                        LOG.info("Successfully fetched remote track:");
                    }
                });
    }

    private final class LoadRemoteTracksTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {

            //fetch db tracks (local+remote)
            List<Track> tracks = mDBAdapter.getAllRemoteTracks(true);
            for (Track t : tracks) {
                mTrackList.add((RemoteTrack) t);
            }

            try {
                List<RemoteTrack> remoteTracks = mDAOProvider.getTrackDAO().getTrackIds(2000, 1);

                for (RemoteTrack remoteTrack : remoteTracks) {
                    if (!mTrackList.contains(remoteTrack))
                        mTrackList.add(remoteTrack);
                }

                Collections.sort(remoteTracks);
            } catch (NotConnectedException e) {
                LOG.error("Unable to load remote tracks", e);
                Snackbar.make(getView(), "Unable to load remote tracks. Maybe you have no " +
                        "connection to the internet?", Snackbar.LENGTH_LONG).show();
            } catch (UnauthorizedException e) {
                LOG.error("Unauthorized to load the tracks.", e);
                Snackbar.make(getView(), "Unauthorized to load the tracks", Snackbar.LENGTH_LONG)
                        .show();
            }

            Collections.sort(mTrackList);

            getActivity().runOnUiThread(() -> mRecyclerViewAdapter.notifyDataSetChanged());

            return null;
        }
    }

    @Override
    public void onTrackUploaded(Track track) {
        RemoteTrack currentRemoteTrackRef = (RemoteTrack) mDBAdapter.getTrack(track.getTrackId());
        mMainThreadWorker.schedule(() -> {
            mTrackList.add(currentRemoteTrackRef);
            mRecyclerViewAdapter.notifyDataSetChanged();
        });
    }
}