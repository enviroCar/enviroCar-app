/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.mapboxsdk.maps.MapView;

import org.envirocar.app.databinding.FragmentTracklistCardlayoutRemoteBinding;
import org.envirocar.core.entity.Track;
import org.envirocar.core.logging.Logger;

import java.util.ArrayList;
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
    public TrackListRemoteCardAdapter(List<Track> tracks, OnTrackInteractionCallback callback) {
        super(tracks, callback);
    }

    protected List<MapView> mapViews = new ArrayList<>();

    @Override
    public RemoteTrackCardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final FragmentTracklistCardlayoutRemoteBinding binding = FragmentTracklistCardlayoutRemoteBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        RemoteTrackCardViewHolder holder = new RemoteTrackCardViewHolder(binding);
        mapViews.add(holder.getMapView());
        return holder;
    }

    @Override
    public void onBindViewHolder(RemoteTrackCardViewHolder holder, int position) {
        LOG.info("onBindViewHolder()");

        final Track remoteTrack = mTrackDataset.get(position);

        // Reset the most important settings of the views.
        holder.getTitleTextView().setText(remoteTrack.getName());
        holder.getDownloadButton().setOnClickListener(null);
        holder.getMapView().onCreate(null);
        holder.getMapView().removeOnDidFailLoadingMapListener(holder.failLoadingMapListener);
        holder.getToolbar().getMenu().clear();
        // Depending on the tracks state
        switch (remoteTrack.getDownloadState()) {
            case REMOTE:
                holder.getContentView().setVisibility(View.GONE);
                holder.getProgressCircle().setVisibility(View.VISIBLE);

                // Workaround: Sometimes the inner arcview can be null when set visible
                holder.getProgressCircle().post(() -> {
                    //holder.mProgressCircle.hide();
                });
                holder.getDownloadButton().show();
                holder.getDownloadButton().setOnClickListener(v -> {
                    holder.getDownloadButton().setOnClickListener(null);
                    mTrackInteractionCallback.onDownloadTrackClicked(remoteTrack, holder);
                });
                holder.getDownloadNotification().setVisibility(View.GONE);
                break;
            case DOWNLOADING:
                holder.getContentView().setVisibility(View.GONE);
                holder.getProgressCircle().setVisibility(View.VISIBLE);
                holder.getProgressCircle().post(() -> holder.getProgressCircle().show());
                holder.getDownloadButton().show();
                holder.getDownloadNotification().setVisibility(View.VISIBLE);
                break;
            case DOWNLOADED:
                holder.getContentView().setVisibility(View.VISIBLE);
                holder.getProgressCircle().setVisibility(View.GONE);
                holder.getDownloadNotification().setVisibility(View.GONE);
                bindLocalTrackViewHolder(holder, remoteTrack);
                break;
        }

        //holder.mMapView.postInvalidate();
    }

    public void onLowMemory() {
        for (MapView mapView : mapViews) {
            mapView.onLowMemory();
        }
    }

    public void onDestroy() {
        for (MapView mapView : mapViews) {
            mapView.onPause();
            mapView.onStop();
            mapView.onDestroy();
        }
    }

}
