package org.envirocar.app.view.utils;

import com.google.common.collect.Maps;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;

import org.envirocar.app.view.trackdetails.TrackSpeedMapOverlay;
import org.envirocar.core.entity.Track;

import java.util.Map;

/**
 * @author dewall
 */
public class MapUtils {

    private static Map<Long, TrackSpeedMapOverlay> TRACKID_TO_OVERLAY_CACHE = Maps.newConcurrentMap();
    private static WebSourceTileLayer OSM_TILE_LAYER;

    public static WebSourceTileLayer getOSMTileLayer() {
        if (OSM_TILE_LAYER == null) {
            OSM_TILE_LAYER = new WebSourceTileLayer("openstreetmap", "http://tile" +
                    ".openstreetmap.org/{z}/{x}/{y}.png");
            OSM_TILE_LAYER.setName("OpenStreetMap")
                    .setAttribution("OpenStreetMap Contributors")
                    .setMinimumZoomLevel(1)
                    .setMaximumZoomLevel(18);
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
