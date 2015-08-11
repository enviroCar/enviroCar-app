package org.envirocar.app.view.utils;

import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;

/**
 * @author dewall
 */
public class MapUtils {

    private static WebSourceTileLayer OSM_TILE_LAYER;

    public static WebSourceTileLayer getOSMTileLayer(){
        if(OSM_TILE_LAYER == null){
            OSM_TILE_LAYER = new WebSourceTileLayer("openstreetmap", "http://tile" +
                    ".openstreetmap.org/{z}/{x}/{y}.png");
            OSM_TILE_LAYER.setName("OpenStreetMap")
                    .setAttribution("OpenStreetMap Contributors")
                    .setMinimumZoomLevel(1)
                    .setMaximumZoomLevel(18);
        }
        return OSM_TILE_LAYER;
    }
}
