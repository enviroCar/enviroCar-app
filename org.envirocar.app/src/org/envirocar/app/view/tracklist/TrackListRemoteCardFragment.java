package org.envirocar.app.view.tracklist;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.view.trackdetails.TrackDetailsActivity;
import org.envirocar.app.view.utils.ECAnimationUtils;
import org.envirocar.core.entity.Track;
import org.envirocar.core.events.NewUserSettingsEvent;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.injection.Injector;
import org.envirocar.core.logging.Logger;

import java.util.Collections;
import java.util.List;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @author dewall
 */
public class TrackListRemoteCardFragment extends AbstractTrackListCardFragment<
        TrackListRemoteCardAdapter> implements TrackListLocalCardFragment.OnTrackUploadedListener {
    private static final Logger LOG = Logger.getLogger(TrackListRemoteCardFragment.class);


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((Injector) activity).injectObjects(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mUserManager.isLoggedIn()) {
            mRecyclerView.setVisibility(View.VISIBLE);
            mTextView.setVisibility(View.GONE);
        } else {
            mTextView.setText("Not Logged In!");
            mRecyclerView.setVisibility(View.GONE);
            mRecyclerViewAdapter.mTrackDataset.clear();
            mRecyclerViewAdapter.notifyDataSetChanged();
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
                        LOG.warn("onUploadTrackClicked() on remote tracks has no effect.");
                    }

                    @Override
                    public void onExportTrackClicked(Track track) {
                        LOG.info(String.format("onExportTrackClicked(%s)", track.getTrackID()));
                        exportTrack(track);
                    }

                    @Override
                    public void onDownloadTrackClicked(Track track, AbstractTrackListCardAdapter
                            .TrackCardViewHolder viewHolder) {
                        onDownloadTrackClickedInner(track, viewHolder);
                    }
                });
    }

    @Override
    protected void loadDataset() {
        // Do not load the dataset twice.
        if (!tracksLoaded) {
            tracksLoaded = true;
            new LoadRemoteTracksTask().execute();
        }
    }

    @Subscribe
    public void onReceiveNewUserSettingsEvent(NewUserSettingsEvent event) {
        if (!event.mIsLoggedIn) {
            mMainThreadWorker.schedule(() -> {
                mRecyclerViewAdapter.mTrackDataset.clear();
                mRecyclerViewAdapter.notifyDataSetChanged();
                tracksLoaded = false;
            });
        }
    }

    private void onDownloadTrackClickedInner(final Track track, AbstractTrackListCardAdapter
            .TrackCardViewHolder viewHolder) {
        AbstractTrackListCardAdapter.RemoteTrackCardViewHolder holder =
                (AbstractTrackListCardAdapter.RemoteTrackCardViewHolder) viewHolder;

        // Show the downloading text notification.
        ECAnimationUtils.animateShowView(getContext(), holder.mDownloadNotification,
                R.anim.fade_in);
        holder.mProgressCircle.show();
        track.setDownloadState(Track.DownloadState.DOWNLOADING);

        mTrackHandler.fetchRemoteTrackObservable(track)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Track>() {
                    @Override
                    public void onCompleted() {
                        holder.mProgressCircle.beginFinalAnimation();
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
                        track.setDownloadState(Track.DownloadState.DOWNLOADING);
                        holder.mDownloadNotification.setText("Error while downloading..");
                    }

                    @Override
                    public void onNext(Track remoteTrack) {
                        LOG.info("Successfully fetched remote track:");
                    }
                });
    }

    private final class LoadRemoteTracksTask extends AsyncTask<Void, Void, Void> {
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

            //fetch db tracks (local+remote)
            List<Track> tracks = mDBAdapter.getAllRemoteTracks(false);
            for (Track t : tracks) {
                mTrackList.add(t);
            }

            try {
                List<Track> remoteTracks = mDAOProvider.getTrackDAO().getTrackIds(2000, 1);

                for (Track remoteTrack : remoteTracks) {
                    if (!mTrackList.contains(remoteTrack))
                        mTrackList.add(remoteTrack);
                }

                Collections.sort(remoteTracks);
            } catch (NotConnectedException e) {
                LOG.error("Unable to load remote tracks", e);
                Snackbar.make(getView(), "Unable to load remote tracks. Maybe you have no " +
                        "connection to the internet?", Snackbar.LENGTH_LONG).show();
                if (mTrackList.isEmpty()) {
                    showText("No connection to the internet");
                }
            } catch (UnauthorizedException e) {
                LOG.error("Unauthorized to load the tracks.", e);
                Snackbar.make(getView(), "Unauthorized to load the tracks", Snackbar.LENGTH_LONG)
                        .show();
                if (mTrackList.isEmpty()) {
                    showText("Unauthorized to load tracks");
                }
            }

            Collections.sort(mTrackList);

            if (getActivity() != null)
                getActivity().runOnUiThread(() -> mRecyclerViewAdapter.notifyDataSetChanged());

            return null;
        }
    }


    @Override
    public void onTrackUploaded(Track track) {
        Track currentRemoteTrackRef = mDBAdapter.getTrack(track.getTrackID());
        mMainThreadWorker.schedule(() -> {
            mTrackList.add(currentRemoteTrackRef);
            mRecyclerViewAdapter.notifyDataSetChanged();
        });
    }
}