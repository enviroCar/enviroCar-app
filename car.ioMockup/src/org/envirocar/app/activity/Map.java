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
import android.view.Menu;

public class Map extends MapActivity {

	private final static String TAG = "Map";

	private MapView mapView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mapView = new MapView(this, new MapnikTileDownloader());
		mapView.setClickable(true);
		mapView.setBuiltInZoomControls(true);

		Bundle bundle = getIntent().getExtras();
		String[] coordinates = bundle.getStringArray("coordinates");
		GeoPoint[][] overlayPoints = getGeoPoints(coordinates);

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

		GeoPoint max = new GeoPoint(getMaxLat(coordinates),
				getMaxLon(coordinates));
		GeoPoint min = new GeoPoint(getMinLat(coordinates),
				getMinLon(coordinates));

		zoomToTrackExtent(mapView, min.latitudeE6, max.latitudeE6,
				min.longitudeE6, max.longitudeE6);

		setContentView(mapView);
	}

	private GeoPoint[][] getGeoPoints(String[] coordinates) {
		List<GeoPoint> geoPoints = new ArrayList<GeoPoint>();

		for (int i = 0; i < coordinates.length; i = i + 2) {
			String lat = coordinates[i];
			String lon = coordinates[i + 1];
			GeoPoint geoPoint = new GeoPoint(Double.parseDouble(lat),
					Double.parseDouble(lon));
			geoPoints.add(geoPoint);
		}

		GeoPoint[] geoPointArr = geoPoints.toArray(new GeoPoint[geoPoints
				.size()]);

		GeoPoint[][] result = new GeoPoint[1][geoPointArr.length];

		for (int i = 0; i < geoPointArr.length; i++) {
			result[0][i] = geoPointArr[i];
		}

		Log.d(TAG, debugTwoDimArr(result));

		return result;
	}

	private double getMaxLat(String[] coordinates) {
		String[] lats = new String[coordinates.length / 2];
		int counter = 0;

		for (int i = 0; i < coordinates.length; i = i + 2) {
			lats[counter] = coordinates[i];
			counter++;
		}

		Double maxLat = Double.parseDouble(String.valueOf(Collections
				.max(Arrays.asList(lats))));
		Log.e(TAG, String.valueOf(maxLat));

		return maxLat;
	}

	private double getMinLat(String[] coordinates) {
		String[] lats = new String[coordinates.length / 2];
		int counter = 0;

		for (int i = 0; i < coordinates.length; i = i + 2) {
			lats[counter] = coordinates[i];
			counter++;
		}

		Double minLat = Double.parseDouble(String.valueOf(Collections
				.min(Arrays.asList(lats))));
		Log.e(TAG, String.valueOf(minLat));

		return minLat;
	}

	private double getMinLon(String[] coordinates) {
		String[] lons = new String[coordinates.length / 2];
		int counter = 0;

		for (int i = 1; i < coordinates.length; i = i + 2) {
			lons[counter] = coordinates[i];
			counter++;
		}

		Double minLon = Double.parseDouble(String.valueOf(Collections
				.min(Arrays.asList(lons))));
		Log.e(TAG, String.valueOf(minLon));

		return minLon;
	}

	private double getMaxLon(String[] coordinates) {
		String[] lons = new String[coordinates.length / 2];
		int counter = 0;

		for (int i = 1; i < coordinates.length; i = i + 2) {
			lons[counter] = coordinates[i];
			counter++;
		}

		Double maxLon = Double.parseDouble(String.valueOf(Collections
				.max(Arrays.asList(lons))));
		Log.e(TAG, String.valueOf(maxLon));

		return maxLon;
	}

	private boolean zoomToTrackExtent(MapView mapView, int minLatE6,
			int maxLatE6, int minLngE6, int maxLngE6) {

		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
//		display.getSize(size);
		int width = size.x;
		int heigth = size.y;

		if (width <= 0 || heigth <= 0) {
			Log.e(TAG, "Display size values not valid numbers.");
			return false;
		}

		int centerLat = (maxLatE6 + minLatE6) / 2;
		int centerLon = (maxLngE6 + minLngE6) / 2;

		mapView.getController().setCenter(new GeoPoint(centerLat, centerLon));

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
			if (pointSW.y - pointNE.y > heigth) {
				break;
			}
			zoomLevel = tmpZoomLevel;
		}

		Log.d(TAG + "Zoomlevel", String.valueOf(zoomLevel));
		mapView.getController().setZoom(zoomLevel);

		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.map, menu);
		return true;
	}

	private String debugTwoDimArr(GeoPoint[][] result) {
		StringBuffer results = new StringBuffer();
		String separator = ",";

		for (int i = 0; i < result.length; ++i) {
			results.append('[');
			for (int j = 0; j < result[i].length; ++j)
				if (j > 0)
					results.append(result[i][j]);
				else
					results.append(result[i][j]).append(separator);
			results.append(']');
		}
		return results.toString();
	}
}