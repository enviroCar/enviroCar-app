package org.envirocar.app.view.tracklist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.envirocar.app.R;
import org.envirocar.core.entity.Track;
import org.envirocar.core.logging.Logger;

import java.util.List;

/**
 * @author dewall
 */
public class TrackListRemoteCardAdapter extends AbstractTrackListCardAdapter<
        AbstractTrackListCardAdapter.RemoteTrackCardViewHolder> {
    private static final Logger LOG = Logger.getLogger(TrackListRemoteCardAdapter.class);

    /**
     * Constructor.
     *
     * @param tracks   the list of tracks to show cards for.
     * @param callback
     */
    public TrackListRemoteCardAdapter(Context context, List<Track> tracks,
                                      OnTrackInteractionCallback callback) {
        super(tracks, callback);
    }

    @Override
    public RemoteTrackCardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate the content view of the card.
        View remoteView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_tracklist_cardlayout_remote, parent, false);

        // and create a new viewholder.
        return new RemoteTrackCardViewHolder(remoteView);
    }

    @Override
    public void onBindViewHolder(RemoteTrackCardViewHolder holder, int position) {
        LOG.info("onBindViewHolder()");

        final Track remoteTrack = mTrackDataset.get(position);

        // Reset the most important settings of the views.
        holder.mTitleTextView.setText(remoteTrack.getName());
        holder.mMapView.getOverlays().clear();
        holder.mDownloadButton.setOnClickListener(null);
        holder.mToolbar.getMenu().clear();

        // Depending on the tracks state
        switch (remoteTrack.getDownloadState()) {
            case REMOTE:
                holder.mContentView.setVisibility(View.GONE);
                holder.mProgressCircle.setVisibility(View.VISIBLE);

                // Workaround: Sometimes the inner arcview can be null when set visible
                holder.mProgressCircle.post(() -> {
                    holder.mProgressCircle.hide();
                });
                holder.mDownloadButton.setVisibility(View.VISIBLE);
                holder.mDownloadButton.setOnClickListener(v -> {
                    holder.mDownloadButton.setOnClickListener(null);
                    mTrackInteractionCallback.onDownloadTrackClicked(remoteTrack, holder);
                });
                holder.mDownloadNotification.setVisibility(View.GONE);
                break;
            case DOWNLOADING:
                holder.mContentView.setVisibility(View.GONE);
                holder.mProgressCircle.setVisibility(View.VISIBLE);
                holder.mProgressCircle.show();
                holder.mDownloadButton.setVisibility(View.VISIBLE);
                holder.mDownloadNotification.setVisibility(View.VISIBLE);
                break;
            case DOWNLOADED:
                holder.mContentView.setVisibility(View.VISIBLE);
                holder.mProgressCircle.setVisibility(View.GONE);
                holder.mDownloadNotification.setVisibility(View.GONE);
                bindLocalTrackViewHolder(holder, remoteTrack);
                break;
        }
    }
}
