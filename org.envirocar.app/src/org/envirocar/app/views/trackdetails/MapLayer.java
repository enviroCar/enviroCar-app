package org.envirocar.app.views.trackdetails;

import android.graphics.Color;

import com.mapbox.geojson.BoundingBox;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.envirocar.core.logging.Logger;

import java.util.ArrayList;

public class MapLayer {
    private static final Logger LOG = Logger.getLogger(MapLayer.class);
    private LineLayer lineLayer;
    private GeoJsonSource geoJsonSource;

    private LatLngBounds mTrackBoundingBox;
    private LatLngBounds mViewBoundingBox;
    private LatLngBounds mScrollableLimitBox;

    private ArrayList<Point> mPoints = new ArrayList<>();
    private ArrayList<LatLng> latLngs = new ArrayList<>();

    public MapLayer(){
        lineLayer = new LineLayer("base-layer", "base-source");
        lineLayer.withProperties(PropertyFactory.lineColor(Color.parseColor("#99DAF2")),
                PropertyFactory.lineWidth(4f));
        geoJsonSource = new GeoJsonSource("base-source", FeatureCollection.fromFeatures(new Feature[] {Feature.fromGeometry(
                LineString.fromLngLats(mPoints)
        )}));
    }


    public void addPoint(double aLatitude, double aLongitude) {
        mPoints.add(Point.fromLngLat(aLatitude, aLongitude));
        latLngs.add(new LatLng(aLatitude, aLongitude));
    }

    public void clearPath(){
        mPoints.clear();
        latLngs.clear();
    }

    private void setBoundingBoxes(){
        // The bounding box of the pathoverlay.
        mTrackBoundingBox = new LatLngBounds.Builder()
                .includes(latLngs)
                .build();

        // The view bounding box of the pathoverlay
        mViewBoundingBox = LatLngBounds.from(
                mTrackBoundingBox.getLatNorth() + 0.01,
                mTrackBoundingBox.getLonEast() + 0.01,
                mTrackBoundingBox.getLatSouth() - 0.01,
                mTrackBoundingBox.getLonWest() - 0.01);

        // The bounding box that limits the scrolling of the mapview.
        mScrollableLimitBox = LatLngBounds.from(
                mTrackBoundingBox.getLatNorth() + 0.05,
                mTrackBoundingBox.getLonEast() + 0.05,
                mTrackBoundingBox.getLatSouth() - 0.05,
                mTrackBoundingBox.getLonWest() - 0.05);
    }

    public LineLayer getLineLayer() {
        return lineLayer;
    }

    public GeoJsonSource getGeoJsonSource() {
        geoJsonSource = new GeoJsonSource("base-source", FeatureCollection.fromFeatures(new Feature[] {Feature.fromGeometry(
                LineString.fromLngLats(mPoints)
        )}));
        return geoJsonSource;
    }
    /**
     * Gets the {@link BoundingBox} of the track.
     *
     * @return the BoundingBox of the track.
     */
    public LatLngBounds getTrackBoundingBox() {
        return mTrackBoundingBox;
    }

    /**
     * Gets the view {@link BoundingBox} of the track, which is a slightly buffered bounding box
     * for zoom purposes of the track.
     *
     * @return the BoundingBox of the track.
     */
    public LatLngBounds getViewBoundingBox() {
        return mViewBoundingBox;
    }

    /**
     * Gets the {@link BoundingBox} that is used as a scrollable limit of the track in the mapview.
     *
     * @return the BoundingBox of the track.
     */
    public LatLngBounds getScrollableLimitBox() {
        return mScrollableLimitBox;
    }
}
