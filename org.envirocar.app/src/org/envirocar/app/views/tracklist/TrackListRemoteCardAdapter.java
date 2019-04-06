/**
 * Copyright (C) 2013 - 2015 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.tracklist;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.envirocar.app.R;
import org.envirocar.core.entity.Track;
import org.envirocar.core.logging.Logger;

import java.util.List;

/**
 * The type Track list remote card adapter.
 *
 * @author dewall
 */
public class TrackListRemoteCardAdapter extends AbstractTrackListCardAdapter<
        AbstractTrackListCardAdapter.RemoteTrackCardViewHolder> {
    private static final Logger LOG = Logger.getLogger(TrackListRemoteCardAdapter.class);
    private SparseBooleanArray expandState = new SparseBooleanArray();

    /**
     * Constructor.
     *
     * @param context         the context
     * @param tracks          the list of tracks to show cards for.
     * @param callback        the callback
     * @param isDieselEnabled the is diesel enabled
     */
    public TrackListRemoteCardAdapter(Context context, List<Track> tracks,
                                      OnTrackInteractionCallback callback, Boolean isDieselEnabled) {
        super(tracks, callback, isDieselEnabled);
        //set initial expanded state to false
        for (int i = 0; i < tracks.size(); i++) {
            expandState.append(i, false);
        }
    }

    @Override
    public RemoteTrackCardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate the content view of the card.
        View remoteView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_tracklist_cardlayout, parent, false);

        // and create a new viewholder.
        return new RemoteTrackCardViewHolder(remoteView);
    }

    @Override
    public void onBindViewHolder(RemoteTrackCardViewHolder holder, int position) {
        LOG.info("onBindViewHolder()");

        final Track remoteTrack = mTrackDataset.get(position);

        // Reset the most important settings of the views.
        String[] titleArray = getDateAndTime(remoteTrack.getName());

        holder.mDateTitleTextView.setText(titleArray[0]);
        holder.mTimeTitleTextView.setText(titleArray[1]);

        //check if view is expanded
        final boolean isExpanded = expandState.get(position);
        holder.expandableLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.buttonLayout.setRotation(expandState.get(position) ? 180f : 0f);

        holder.buttonLayout.setOnClickListener(v -> onClickButton(holder.expandableLayout, holder.buttonLayout, holder.completeCard, position, expandState));
        holder.completeCard.setOnClickListener(v -> holder.buttonLayout.performClick());

        // Depending on the tracks state
        switch (remoteTrack.getDownloadState()) {
            case REMOTE:
                holder.buttonLayout.setOnClickListener(view -> {
                    buttonsToggle(R.id.download_progress, holder);
                    mTrackInteractionCallback.onDownloadTrackClicked(remoteTrack, holder);
                });

                holder.completeCard.setOnClickListener(v -> {
                    mTrackInteractionCallback.showToast("Please click on the button to download the track");
                });
                buttonsToggle(R.id.button_download, holder);
                break;
            case DOWNLOADING:
                buttonsToggle(R.id.download_progress, holder);
                break;
            case DOWNLOADED:
                holder.buttonLayout.setOnClickListener(v -> onClickButton(holder.expandableLayout, holder.buttonLayout, holder.completeCard, position, expandState));
                holder.completeCard.setOnClickListener(v -> holder.buttonLayout.performClick());
                buttonsToggle(R.id.button_arrow, holder);
                bindLocalTrackViewHolder(holder, remoteTrack);
                break;
        }
    }
/**
 * Send the ID of the View that should be visible
 * */
    public static void buttonsToggle(int toggle, RemoteTrackCardViewHolder holder) {
        switch (toggle){
            case R.id.button_arrow:
                holder.buttonArrow.setVisibility(View.VISIBLE);
                holder.buttonDownload.setVisibility(View.GONE);
                holder.downloadProgress.setVisibility(View.GONE);
                break;
            case R.id.button_download:
                holder.buttonArrow.setVisibility(View.GONE);
                holder.buttonDownload.setVisibility(View.VISIBLE);
                holder.downloadProgress.setVisibility(View.GONE);
                break;
            case R.id.download_progress:
                holder.buttonArrow.setVisibility(View.GONE);
                holder.buttonDownload.setVisibility(View.GONE);
                holder.downloadProgress.setVisibility(View.VISIBLE);
                break;
        }
    }
}
