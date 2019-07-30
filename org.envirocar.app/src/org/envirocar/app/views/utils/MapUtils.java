/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
package org.envirocar.app.views.utils;

import com.mapbox.mapboxsdk.style.sources.TileSet;

import org.envirocar.app.views.trackdetails.TrackSpeedMapOverlay;
import org.envirocar.core.entity.Track;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author dewall
 */
public class MapUtils {

    private static Map<Long, TrackSpeedMapOverlay> TRACKID_TO_OVERLAY_CACHE = new ConcurrentHashMap<>();
    private static TileSet OSM_TILE_LAYER;

    public static TileSet getOSMTileLayer() {
        if (OSM_TILE_LAYER == null) {
            OSM_TILE_LAYER = new TileSet("openstreetmap", "http://tile" +
                    ".openstreetmap.org/{z}/{x}/{y}.png");
            OSM_TILE_LAYER.setName("OpenStreetMap");
            OSM_TILE_LAYER.setAttribution("OpenStreetMap Contributors");
            OSM_TILE_LAYER.setMaxZoom(18);
            OSM_TILE_LAYER.setMinZoom(1);
        }
        return OSM_TILE_LAYER;
    }

    public static TrackSpeedMapOverlay createTrackPathOverlay(Track track){
        if(TRACKID_TO_OVERLAY_CACHE.containsKey(track.getTrackID().getId())){
            return TRACKID_TO_OVERLAY_CACHE.get(track.getTrackID().getId());
        }

        TrackSpeedMapOverlay overlay = new TrackSpeedMapOverlay(track);
        TRACKID_TO_OVERLAY_CACHE.put(track.getTrackID().getId(), overlay);
        return overlay;
    }
}
