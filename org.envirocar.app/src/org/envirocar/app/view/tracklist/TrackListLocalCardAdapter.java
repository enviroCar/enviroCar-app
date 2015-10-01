package org.envirocar.app.view.tracklist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.envirocar.app.R;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.storage.Track;

import java.util.List;

/**
 * @author dewall
 */
public class TrackListLocalCardAdapter extends TrackListCardAdapter<Track,
        TrackListCardAdapter.LocalTrackCardViewHolder> {
    private static final Logger LOGGER = Logger.getLogger(TrackListLocalCardAdapter.class);

    /**
     * Constructor.
     *
     * @param tracks   the list of tracks to show cards for.
     * @param callback
     */
    public TrackListLocalCardAdapter(List<Track> tracks, OnTrackInteractionCallback callback) {
        super(tracks, callback);
    }

    @Override
    public TrackListLocalCardAdapter.LocalTrackCardViewHolder onCreateViewHolder(
            ViewGroup parent, int viewType) {

        // First inflate the view.
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout
                .fragment_tracklist_cardlayout, parent, false);

        // then return a new view holder for the inflated view.
        return new LocalTrackCardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final LocalTrackCardViewHolder holder, int position) {
        LOGGER.info("onBindViewHolder()");
        bindLocalTrackViewHolder(holder, mTrackDataset.get(position));
    }
}
