package org.envirocar.app.view.trackdetails;

import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;

import org.envirocar.core.entity.Track;

public abstract interface TrackInterface {
    public Track getTrackLocal();
    public WebSourceTileLayer getSourceLocal();

}
   