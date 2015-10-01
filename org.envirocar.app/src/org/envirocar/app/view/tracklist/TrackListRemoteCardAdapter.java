package org.envirocar.app.view.tracklist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.envirocar.app.R;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.storage.RemoteTrack;
import org.envirocar.app.view.utils.ECAnimationUtils;

import java.util.List;

/**
 * @author dewall
 */
public class TrackListRemoteCardAdapter extends TrackListCardAdapter<RemoteTrack,
        TrackListCardAdapter.RemoteTrackCardViewHolder> {
    private static final Logger LOG = Logger.getLogger(TrackListRemoteCardAdapter.class);

    private static final int TYPE_REMOTE = 0;
    private static final int TYPE_LOCAL = 1;

    private Context mContext;

    /**
     * Constructor.
     *
     * @param tracks   the list of tracks to show cards for.
     * @param callback
     */
    public TrackListRemoteCardAdapter(Context context, List<RemoteTrack> tracks,
                                      OnTrackInteractionCallback callback) {
        super(tracks, callback);
        this.mContext = context;
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

        final RemoteTrack remoteTrack = mTrackDataset.get(position);

        if (remoteTrack.isDownloaded()) {
            holder.mContentView.setVisibility(View.VISIBLE);
            holder.mProgressCircle.setVisibility(View.GONE);
            holder.mDownloadButton.setVisibility(View.GONE);

            bindLocalTrackViewHolder(holder, remoteTrack);
        } else {
            if (holder.mState == RemoteTrackCardViewHolder.DownloadState.NOTHING) {
                holder.mDownloadButton.setOnClickListener(v -> {
                    mTrackInteractionCallback.onDownloadTrackClicked(remoteTrack, holder);
                });
            } else if(holder.mState == RemoteTrackCardViewHolder.DownloadState.DOWNLOADING){
                holder.mDownloadButton.setOnClickListener(null);
                holder.mProgressCircle.show();
            }
        }
    }
}
