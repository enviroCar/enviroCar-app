package car.io.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.MapnikTileDownloader;
import org.mapsforge.android.maps.overlay.ArrayWayOverlay;
import org.mapsforge.android.maps.overlay.OverlayWay;
import org.mapsforge.core.GeoPoint;

import car.io.R;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.android.maps.overlay.OverlayItem;

public class Map extends MapActivity {

	private final static String TAG = "Map";

	private MapView mapView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(null);

		this.setTheme(android.R.style.Theme);
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

		mapView.getOverlays().add(itemizedOverlay);
		OverlayWay way = new OverlayWay(overlayPoints);
		wayOverlay.addWay(way);
		mapView.getOverlays().add(wayOverlay);
		setContentView(mapView);
		bundle.clear();
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