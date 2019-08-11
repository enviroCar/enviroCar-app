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

import android.animation.ArgbEvaluator;
import android.graphics.Color;

import com.mapbox.geojson.BoundingBox;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.lineProgress;
import static com.mapbox.mapboxsdk.style.expressions.Expression.linear;
import static com.mapbox.mapboxsdk.style.expressions.Expression.rgb;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineCap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineGradient;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineJoin;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

/**
 * @author dewall
 */
public class TrackMapLayer extends MapLayer{
    private static final Logger LOG = Logger.getLogger(TrackMapLayer.class);

    public static final String GRADIENT_LAYER = "gradient-layer";
    public static final String GRADIENT_SOURCE = "source-layer";

    private Double gradMax, gradMin;
    private final Track mTrack;
    private List<Measurement> measurementList = new ArrayList<>();
    private Boolean hasNoMeasurements;
    private Boolean hasLatLng;
    protected LatLngBounds mTrackBoundingBox;
    protected LatLngBounds mViewBoundingBox;
    protected LatLngBounds mScrollableLimitBox;

    /**
     * Constructor.
     *
     * @param track the track to create a overlay for.
     */
    public TrackMapLayer(Track track) {
        super();
        mTrack = track;
        if (mTrack.getMeasurements() != null) {
            measurementList = mTrack.getMeasurements();
            hasNoMeasurements = false;
        } else {
            hasNoMeasurements = true;
            hasLatLng = false;
        }

        initPath();
    }

    /**
     * Initializes the track path and the bounding boxes required by the mapviews.
     */
    private void initPath() {
        if(!hasNoMeasurements)
        {
            // For each measurement value add the longitude and latitude coordinates as a new
            // mappoint to the point list. In addition, try to find out the maximum and minimum
            // lon/lat coordinates for the zoom value of the mapview.
            for (Measurement measurement : measurementList) {
                double latitude = measurement.getLatitude();
                double longitude = measurement.getLongitude();

                if(latitude == 0.0 || longitude == 0.0) {
                    LOG.warn("An coordinate was 0.0");
                    continue;
                }
                addPoint(latitude, longitude);
            }

            //If there are no points
            if(mPoints.size() == 0){
                hasNoMeasurements = true;
                hasLatLng = false;
                addPoint(7.635147738274369, 51.96057578167202);
                addPoint(7.635078051137631, 51.96024289279303);
            } else {
                hasLatLng = true;
            }

        } else {
            LOG.info("Track has no measurements");
            addPoint(7.635147738274369, 51.96057578167202);
            addPoint(7.635078051137631, 51.96024289279303);
        }
        setGeoJsonSource();
        setBoundingBoxes();
    }

    protected void setBoundingBoxes(){
        if(mPoints.size() == 1){
            LatLng latLng = latLngs.get(0);
            mViewBoundingBox = LatLngBounds.from(
                    latLng.getLatitude() + 0.01,
                    latLng.getLongitude() + 0.01,
                    latLng.getLatitude() - 0.01,
                    latLng.getLongitude() - 0.01);
        } else {
            mTrackBoundingBox = new LatLngBounds.Builder()
                    .includes(latLngs)
                    .build();

            double latRatio = Math.max(mTrackBoundingBox.getLatitudeSpan() / 10.0, 0.01);
            double lngRatio = Math.max(mTrackBoundingBox.getLongitudeSpan() / 10.0, 0.01);
            // The view bounding box of the pathoverlay
            mViewBoundingBox = LatLngBounds.from(
                    mTrackBoundingBox.getLatNorth() + latRatio,
                    mTrackBoundingBox.getLonEast() + lngRatio,
                    mTrackBoundingBox.getLatSouth() - latRatio,
                    mTrackBoundingBox.getLonWest() - lngRatio);

            // The bounding box that limits the scrolling of the mapview.
            mScrollableLimitBox = LatLngBounds.from(
                    mTrackBoundingBox.getLatNorth() + 0.05,
                    mTrackBoundingBox.getLonEast() + 0.05,
                    mTrackBoundingBox.getLatSouth() - 0.05,
                    mTrackBoundingBox.getLonWest() - 0.05);
        }

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

    public LineLayer getGradientLineLayer(Measurement.PropertyKey propertyKey){
        gradMax = gradMin = (double) 0;
        LOG.info("getGradientLineLayer with " + propertyKey.toString());
        if(!hasNoMeasurements && hasLatLng)
        {
            LOG.info("Track has measurements.");
            float size = (float)measurementList.size(), i= 0f;
            if(size >= 2)
            {
                LOG.info("Track has more than 2 measurements.");
                List<Double> propertyValues = new ArrayList<>();
                for(Measurement measurement : measurementList){
                    if(measurement.hasProperty(propertyKey))
                        propertyValues.add(measurement.getProperty(propertyKey));
                    else {
                        propertyValues.add((double) 0);
                        LOG.info("Measurement doesnt have " + propertyKey.toString());
                    }
                }

                Double min;
                Double max;

                if(propertyKey.equals(Measurement.PropertyKey.SPEED))
                    min = (double) 0;
                else {
                    if(Collections.min(propertyValues) != null)
                        min = Collections.min(propertyValues);
                    else
                        min = (double) 0;
                }

                if(Collections.max(propertyValues) != null)
                    max = Collections.max(propertyValues);
                else
                    max = (double) 0;

                LOG.info("max:" + max + " min:" + min);

                gradMax = max;
                gradMin = min;

                //Set the start and end colors for the map legend
                int startColor = Color.parseColor("#00FF00");
                int endColor = Color.parseColor("#FF0000");
                ArgbEvaluator evaluator = new ArgbEvaluator();
                List<Expression.Stop> stops  = new ArrayList<>();

                for(Double value : propertyValues){
                    //Calculate the color that each point on the line should be and add it to
                    // the list of stops
                    Double fraction = value / max;
                    Float stop = i / size;
                    Integer temp = (Integer) evaluator.evaluate(fraction.floatValue(), startColor, endColor);
                    stops.add(Expression.stop(stop, rgb(Color.red(temp), Color.green(temp), Color.blue(temp))));
                    i++;
                }
                return new LineLayer(GRADIENT_LAYER, GRADIENT_SOURCE).withProperties(
                        lineCap(Property.LINE_CAP_ROUND),
                        lineJoin(Property.LINE_JOIN_ROUND),
                        lineWidth(4f),
                        lineGradient(interpolate(
                                linear(), lineProgress(),
                                stops.toArray(new Expression.Stop[0])
                        )));

            } else {
                LOG.info("Not enough measurements.");
                return new LineLayer(GRADIENT_LAYER, GRADIENT_SOURCE).withProperties(
                        lineCap(Property.LINE_CAP_ROUND),
                        lineJoin(Property.LINE_JOIN_ROUND),
                        lineWidth(4f),
                        lineColor(Color.parseColor("#0065A0")));
            }
        } else {
            LOG.info("Track has no measurements");
            // Line has no points, so return a transparent linestring
            return new LineLayer(GRADIENT_LAYER, GRADIENT_SOURCE).withProperties(
                    lineCap(Property.LINE_CAP_ROUND),
                    lineJoin(Property.LINE_JOIN_ROUND),
                    lineWidth(4f),
                    lineColor(Color.TRANSPARENT));
        }
    }

    public GeoJsonSource getGradientGeoJSONSource(){
        return new GeoJsonSource(GRADIENT_SOURCE, FeatureCollection.fromFeatures(new Feature[] {Feature.fromGeometry(
                LineString.fromLngLats(mPoints)
        )}), new GeoJsonOptions().withLineMetrics(true));
    }

    public Double getGradMax() {
        return gradMax;
    }

    public Double getGradMin() {
        return gradMin;
    }

    @Override
    public void setLineLayer() {
        if(!hasNoMeasurements) {
            lineLayer = new LineLayer(LAYER_NAME, SOURCE_NAME).withSourceLayer(SOURCE_NAME).withProperties(
                    PropertyFactory.lineColor(Color.parseColor("#0065A0")),
                    PropertyFactory.lineWidth(3f),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND));
        } else {
            LOG.info("Track has no measurements");
            lineLayer = new LineLayer(LAYER_NAME, SOURCE_NAME).withSourceLayer(SOURCE_NAME).withProperties(
                    PropertyFactory.lineColor(Color.TRANSPARENT),
                    PropertyFactory.lineWidth(3f),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND));
        }
    }

    @Override
    public LineLayer getLineLayer() {
        this.setLineLayer();
        return lineLayer;
    }

    public Boolean hasNoMeasurements() {
        return hasNoMeasurements;
    }

    public Boolean hasLatLng() {
        return hasLatLng;
    }
}
