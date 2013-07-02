/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */

package org.envirocar.app.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.envirocar.app.R;
import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.MapnikTileDownloader;
import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.android.maps.overlay.ArrayWayOverlay;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.android.maps.overlay.OverlayWay;
import org.mapsforge.core.GeoPoint;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
/**
 * Map visualization that displays a track on a map
 * @author christopher
 *
 */
public class Map extends MapActivity {

	private final static String TAG = "Map";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		MapView mapView = new MapView(this, new MapnikTileDownloader());
		mapView.setClickable(true);
		mapView.setBuiltInZoomControls(true);

		Bundle bundle = getIntent().getExtras();
		String[] coordinates = bundle.getStringArray("coordinates");
		GeoPoint[][] overlayPoints = getOverlayPoints(coordinates);

		Log.d(TAG, Arrays.deepToString(coordinates));

		Paint wayDefaultPaintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
		wayDefaultPaintFill.setStyle(Paint.Style.STROKE);
		wayDefaultPaintFill.setColor(Color.BLUE);
		wayDefaultPaintFill.setAlpha(160);
		wayDefaultPaintFill.setStrokeWidth(7);
		wayDefaultPaintFill.setStrokeJoin(Paint.Join.ROUND);
		wayDefaultPaintFill.setPathEffect(new DashPathEffect(new float[] { 20,
				20 }, 0));

		Paint wayDefaultPaintOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
		wayDefaultPaintOutline.setStyle(Paint.Style.STROKE);
		wayDefaultPaintOutline.setColor(Color.BLUE);
		wayDefaultPaintOutline.setAlpha(128);
		wayDefaultPaintOutline.setStrokeWidth(7);
		wayDefaultPaintOutline.setStrokeJoin(Paint.Join.ROUND);
		ArrayWayOverlay wayOverlay = new ArrayWayOverlay(wayDefaultPaintFill,
				wayDefaultPaintOutline);

		Drawable defaultMarker = getResources().getDrawable(
				R.drawable.marker_red);
		ArrayItemizedOverlay itemizedOverlay = new ArrayItemizedOverlay(
				defaultMarker);

		GeoPoint startPoint = overlayPoints[0][0];
		OverlayItem startItem = new OverlayItem(startPoint, "Start",
				"Starting Point of Measurements.");
		itemizedOverlay.addItem(startItem);

		GeoPoint endPoint = overlayPoints[0][overlayPoints[0].length - 1];
		OverlayItem endItem = new OverlayItem(endPoint, "End",
				"End Point of Measurements.");
		itemizedOverlay.addItem(endItem);

		OverlayWay way = new OverlayWay(overlayPoints);
		wayOverlay.addWay(way);
		mapView.getOverlays().add(wayOverlay);
		mapView.getOverlays().add(itemizedOverlay);

		GeoPoint pointNorthEast = new GeoPoint(getMaxLat(coordinates),
				getMaxLng(coordinates));
		GeoPoint pointSouthWest = new GeoPoint(getMinLat(coordinates),
				getMinLng(coordinates));

		zoomToTrackExtent(mapView, pointSouthWest.latitudeE6,
				pointNorthEast.latitudeE6, pointSouthWest.longitudeE6,
				pointNorthEast.longitudeE6);
		
		setContentView(mapView);
	}

	/**
	 * Converts a string array with coordinates to geoOverlay array.
	 * 
	 * @param coordinates the string array containing
	 * @return the overlay GeoPoints
	 */
	private GeoPoint[][] getOverlayPoints(String[] coordinates) {
		List<GeoPoint> geoPointList = new ArrayList<GeoPoint>();

		for (int i = 0; i < coordinates.length; i = i + 2) {
			String lat = coordinates[i];
			String lng = coordinates[i + 1];
			GeoPoint geoPoint = new GeoPoint(Double.parseDouble(lat),
					Double.parseDouble(lng));
			geoPointList.add(geoPoint);
		}

		GeoPoint[] geoPointArray = geoPointList
				.toArray(new GeoPoint[geoPointList.size()]);

		GeoPoint[][] overlayPoints = new GeoPoint[1][geoPointArray.length];

		for (int i = 0; i < geoPointArray.length; i++) {
			overlayPoints[0][i] = geoPointArray[i];
		}

		return overlayPoints;
	}

	/**
	 * Returns the maximum latitude value of a string array containing coordinates 
	 * in order "[lat, lng, lat, ...]".
	 * 
	 * @param coordinates the coordinates array to be searched
	 * @return the maximum latitude value of the passed coordinates array
	 */
	private double getMaxLat(String[] coordinates) {
		String[] lats = new String[coordinates.length / 2];
		int counter = 0;

		for (int i = 0; i < coordinates.length; i = i + 2) {
			lats[counter] = coordinates[i];
			counter++;
		}

		Double maxLat = Double.parseDouble(String.valueOf(Collections
				.max(Arrays.asList(lats))));
		Log.d(TAG, String.valueOf(maxLat));

		return maxLat;
	}

	/**
	 * Returns the minimum latitude value of a string array containing coordinates 
	 * in order "[lat, lng, lat, ...]".
	 * 
	 * @param coordinates the coordinates array to be searched
	 * @return the minimum latitude value of the passed coordinates array
	 */
	private double getMinLat(String[] coordinates) {
		String[] lats = new String[coordinates.length / 2];
		int counter = 0;

		for (int i = 0; i < coordinates.length; i = i + 2) {
			lats[counter] = coordinates[i];
			counter++;
		}

		Double minLat = Double.parseDouble(String.valueOf(Collections
				.min(Arrays.asList(lats))));
		Log.d(TAG, String.valueOf(minLat));

		return minLat;
	}

	/**
	 * Returns the minimum longitude value of a string array containing coordinates 
	 * in order "[lat, lng, lat, ...]".
	 * 
	 * @param coordinates the coordinates array to be searched
	 * @return the minimum longitude value of the passed coordinates array
	 */
	private double getMinLng(String[] coordinates) {
		String[] lngs = new String[coordinates.length / 2];
		int counter = 0;

		for (int i = 1; i < coordinates.length; i = i + 2) {
			lngs[counter] = coordinates[i];
			counter++;
		}

		Double minLng = Double.parseDouble(String.valueOf(Collections
				.min(Arrays.asList(lngs))));
		Log.d(TAG, String.valueOf(minLng));

		return minLng;
	}

	/**
	 * Returns the maximum longitude value of a string array containing coordinates 
	 * in order "[lat, lng, lat, ...]".
	 * 
	 * @param coordinates the coordinates array to be searched
	 * @return the maximum longitude value of the passed coordinates array
	 */
	private double getMaxLng(String[] coordinates) {
		String[] lngs = new String[coordinates.length / 2];
		int counter = 0;

		for (int i = 1; i < coordinates.length; i = i + 2) {
			lngs[counter] = coordinates[i];
			counter++;
		}

		Double maxLng = Double.parseDouble(String.valueOf(Collections
				.max(Arrays.asList(lngs))));
		Log.d(TAG, String.valueOf(maxLng));

		return maxLng;
	}

	/**
	 * Sets the map's bounding box to the extent of the track overlay.
	 * 
	 * @param mapView the MapView instance to be adapted
	 * @param minLatE6 the minimum latitude value of the track overlay in microdegrees
	 * @param maxLatE6 the maximum latitude value of the track overlay in microdegrees
	 * @param minLngE6 the minimum longitude value of the track overlay in microdegrees
	 * @param maxLngE6 the maximum longitude value of the track overlay in microdegrees
	 * @return true if zoomToTrackExtent could be executed, otherwise false
	 */
	private boolean zoomToTrackExtent(MapView mapView, int minLatE6,
			int maxLatE6, int minLngE6, int maxLngE6) {

		Display display = getWindowManager().getDefaultDisplay(); 
		int width = display.getWidth() - 45;  
		int height = display.getHeight() - 45;  
		
		if (width <= 0 || height <= 0) {
			Log.e(TAG, "Display size values not valid numbers.");
			return false;
		}

		int centerLat = (maxLatE6 + minLatE6) / 2;
		int centerLng = (maxLngE6 + minLngE6) / 2;

		mapView.getController().setCenter(new GeoPoint(centerLat, centerLng));

		GeoPoint pointSouthWest = new GeoPoint(minLatE6, minLngE6);
		GeoPoint pointNorthEast = new GeoPoint(maxLatE6, maxLngE6);

		Projection projection = mapView.getProjection();
		Point pointSW = new Point();
		Point pointNE = new Point();
		byte zoomLevelMax = (byte) mapView.getMapGenerator().getZoomLevelMax();
		byte zoomLevel = 0;

		while (zoomLevel < zoomLevelMax) {
			byte tmpZoomLevel = (byte) (zoomLevel + 1);
			projection.toPoint(pointSouthWest, pointSW, tmpZoomLevel);
			projection.toPoint(pointNorthEast, pointNE, tmpZoomLevel);
			if (pointNE.x - pointSW.x > width) {
				break;
			}
			if (pointSW.y - pointNE.y > height) {
				break;
			}
			zoomLevel = tmpZoomLevel;
		}

		Log.d(TAG + "Zoomlevel", String.valueOf(zoomLevel));
		mapView.getController().setZoom(zoomLevel);

		return true;
	}
	
}