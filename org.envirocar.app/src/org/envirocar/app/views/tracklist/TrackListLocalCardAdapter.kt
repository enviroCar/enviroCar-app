/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.mapbox.mapboxsdk.maps.MapView;

import org.envirocar.app.databinding.FragmentTracklistCardlayoutBinding;
import org.envirocar.core.entity.Track;
import org.envirocar.core.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dewall
 */
public class TrackListLocalCardAdapter extends AbstractTrackListCardAdapter<
        AbstractTrackListCardAdapter.LocalTrackCardViewHolder> {
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

    protected List<MapView> mapViews = new ArrayList<>();

    @Override
    public TrackListLocalCardAdapter.LocalTrackCardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final FragmentTracklistCardlayoutBinding binding = FragmentTracklistCardlayoutBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        LocalTrackCardViewHolder holder = new LocalTrackCardViewHolder(binding);
        mapViews.add(holder.getMapView());
        return holder;
    }

    @Override
    public void onBindViewHolder(final LocalTrackCardViewHolder holder, int position) {
        bindLocalTrackViewHolder(holder, mTrackDataset.get(position));
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
