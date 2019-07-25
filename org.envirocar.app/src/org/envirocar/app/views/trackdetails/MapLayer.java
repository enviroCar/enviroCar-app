package org.envirocar.app.views.trackdetails;

import android.graphics.Color;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.envirocar.core.logging.Logger;

import java.util.ArrayList;

public class MapLayer {
    private static final Logger LOG = Logger.getLogger(MapLayer.class);


    public static final String SOURCE_NAME = "base-source";
    public static final String LAYER_NAME = "base-layer";

    protected LineLayer lineLayer;
    protected GeoJsonSource geoJsonSource;

    protected ArrayList<Point> mPoints = new ArrayList<>();
    protected ArrayList<LatLng> latLngs = new ArrayList<>();

    public MapLayer(){

        geoJsonSource = new GeoJsonSource(SOURCE_NAME, FeatureCollection.fromFeatures(new Feature[] {Feature.fromGeometry(
                LineString.fromLngLats(mPoints)
        )}));
        lineLayer = new LineLayer(LAYER_NAME, SOURCE_NAME).withProperties(PropertyFactory.lineColor(Color.parseColor("#99DAF2")),
                PropertyFactory.lineWidth(4f));
    }


    public void addPoint(double aLatitude, double aLongitude) {
        mPoints.add(Point.fromLngLat(aLatitude, aLongitude));
        latLngs.add(new LatLng(aLatitude, aLongitude));
    }

    public void clearPath(){
        mPoints.clear();
        latLngs.clear();
    }

    public LineLayer getLineLayer() {
        return lineLayer;
    }

    public void setGeoJsonSource() {
        this.geoJsonSource = new GeoJsonSource(SOURCE_NAME, FeatureCollection.fromFeatures(new Feature[] {Feature.fromGeometry(
                LineString.fromLngLats(mPoints)
        )}));
    }

    public GeoJsonSource getGeoJsonSource() {
        setGeoJsonSource();
        return geoJsonSource;
    }
}
