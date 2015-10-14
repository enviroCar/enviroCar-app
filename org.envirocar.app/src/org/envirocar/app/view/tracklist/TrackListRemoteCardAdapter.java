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

    private static final int TYPE_REMOTE = 0;
    private static final int TYPE_LOCAL = 1;


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
        View remoteView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_tracklist_cardlayout_remote, parent, false);

        return new RemoteTrackCardViewHolder(remoteView);
    }

    @Override
    public void onBindViewHolder(RemoteTrackCardViewHolder holder, int position) {
        LOG.info("onBindViewHolder()");

        final Track remoteTrack = mTrackDataset.get(position);

        holder.mContentView.setVisibility(View.GONE);
        holder.mProgressCircle.setVisibility(View.GONE);
        holder.mDownloadButton.setVisibility(View.GONE);
        holder.mMapView.getOverlays().clear();

        if (remoteTrack.getRemoteID() != null && remoteTrack.isDownloaded()) {
            holder.mContentView.setVisibility(View.VISIBLE);

            bindLocalTrackViewHolder(holder, remoteTrack);
        } else {
            holder.mProgressCircle.setVisibility(View.VISIBLE);
            holder.mDownloadButton.setVisibility(View.VISIBLE);
            if (holder.mState == RemoteTrackCardViewHolder.DownloadState.NOTHING) {
                holder.mDownloadButton.setOnClickListener(v -> {
                    mTrackInteractionCallback.onDownloadTrackClicked(remoteTrack, holder);
                });
            } else if (holder.mState == RemoteTrackCardViewHolder.DownloadState.DOWNLOADING) {
                holder.mDownloadButton.setOnClickListener(null);
                holder.mProgressCircle.show();
            } else if (holder.mState == RemoteTrackCardViewHolder.DownloadState.DOWNLOADED) {
                holder.mProgressCircle.setVisibility(View.GONE);
                holder.mDownloadButton.setVisibility(View.GONE);
            }

            holder.mTitleTextView.setText(remoteTrack.getName());
        }
    }
}
