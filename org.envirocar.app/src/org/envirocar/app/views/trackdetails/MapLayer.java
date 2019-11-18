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
package org.envirocar.app.views.trackdetails;

import android.graphics.Color;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.PropertyValue;
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
    protected Float maxZoom, minZoom;

    public MapLayer(){
        maxZoom = 18f;
        minZoom = 1f;
    }

    public void addPoint(double aLatitude, double aLongitude) {
        mPoints.add(Point.fromLngLat(aLongitude,aLatitude));
        latLngs.add(new LatLng(aLatitude, aLongitude));
    }

    public void clearPath(){
        mPoints.clear();
        latLngs.clear();
    }

    public LineLayer getLineLayer() {
        setLineLayer();
        return lineLayer;
    }

    public void setGeoJsonSource() {
        this.geoJsonSource = new GeoJsonSource(SOURCE_NAME, FeatureCollection.fromFeatures(new Feature[] {Feature.fromGeometry(
                LineString.fromLngLats(mPoints)
        )}));
    }

    public void setLineLayer() {
        lineLayer = new LineLayer(LAYER_NAME, SOURCE_NAME).withSourceLayer(SOURCE_NAME).withProperties(
                PropertyFactory.lineColor(Color.parseColor("#0065A0")),
                PropertyFactory.lineWidth(4f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND));
    }

    public void changeLineProperties(PropertyValue<?> properties){
        lineLayer.setProperties(properties);
    }

    public GeoJsonSource getGeoJsonSource() {
        setGeoJsonSource();
        return geoJsonSource;
    }

    public ArrayList<Point> getPoints() {
        return mPoints;
    }

    public Float getMaxZoom() {
        return maxZoom;
    }

    public Float getMinZoom() {
        return minZoom;
    }
}
