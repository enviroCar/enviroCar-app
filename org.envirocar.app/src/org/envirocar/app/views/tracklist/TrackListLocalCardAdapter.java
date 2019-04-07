/**
 * Copyright (C) 2013 - 2015 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.tracklist;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import org.envirocar.app.R;
import org.envirocar.core.entity.Track;
import org.envirocar.core.logging.Logger;

import java.util.List;

/**
 * @author dewall
 */
public class TrackListLocalCardAdapter extends AbstractTrackListCardAdapter<
        AbstractTrackListCardAdapter.LocalTrackCardViewHolder> {
    private static final Logger LOGGER = Logger.getLogger(TrackListLocalCardAdapter.class);
    private final Context context;
    private SparseBooleanArray expandState = new SparseBooleanArray();

    /**
     * Constructor.
     *
     * @param tracks   the list of tracks to show cards for.
     * @param callback
     */
    public TrackListLocalCardAdapter(List<Track> tracks, OnTrackInteractionCallback callback, Boolean isDieselEnabled, Context context) {
        super(tracks, callback, isDieselEnabled);
        this.context = context;
        //set initial expanded state to false
        for (int i = 0; i < tracks.size(); i++) {
            expandState.append(i, false);
        }
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
        //check if view is expanded
        final boolean isExpanded = expandState.get(position);
        holder.expandableLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.buttonLayout.setRotation(expandState.get(position) ? 180f : 0f);

        //Pop-up menu
        holder.popupMenuButton.setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(context, view);
            MenuInflater inflater = popup.getMenuInflater();
            popup.setOnMenuItemClickListener(menuItem -> {
                switch (menuItem.getItemId()) {
                    case R.id.export_item:
                        mTrackInteractionCallback.onExportTrackClicked(mTrackDataset.get(position));
                        return true;
                    case R.id.upload_item:
                        mTrackInteractionCallback.onUploadTrackClicked(mTrackDataset.get(position));
                        return true;
                    case R.id.delete_item:
                        mTrackInteractionCallback.onDeleteTrackClicked(mTrackDataset.get(position));
                        return true;
                    default:
                        return false;
                }
            });
            inflater.inflate(R.menu.menu_card_popup_local_track, popup.getMenu());
            popup.show();
        });

        holder.buttonLayout.setOnClickListener(v -> onClickArrowButton(holder.expandableLayout, holder.buttonLayout, holder.completeCard, position, expandState));
        holder.completeCard.setOnClickListener(v -> holder.buttonLayout.performClick());
        bindLocalTrackViewHolder(holder, mTrackDataset.get(position));
    }
}
