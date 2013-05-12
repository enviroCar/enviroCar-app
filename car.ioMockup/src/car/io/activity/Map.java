package car.io.activity;

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
import android.view.Menu;

import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.android.maps.overlay.OverlayItem;

public class Map extends MapActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.activity_map);
		
		this.setTheme(android.R.style.Theme);

		MapView mapView = new MapView(this, new MapnikTileDownloader());
		mapView.setClickable(true);
		mapView.setBuiltInZoomControls(true);
		// mapView.setMapGenerator(new MapnikTileDownloader());
//		String s = Environment.getExternalStorageDirectory().getPath();
//		mapView.setMapFile(new File(s + "/nordrhein-westfalen.map"));
		
		// create the default paint objects for overlay ways
		Paint wayDefaultPaintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
		wayDefaultPaintFill.setStyle(Paint.Style.STROKE);
		wayDefaultPaintFill.setColor(Color.BLUE);
		wayDefaultPaintFill.setAlpha(160);
		wayDefaultPaintFill.setStrokeWidth(7);
		wayDefaultPaintFill.setStrokeJoin(Paint.Join.ROUND);
		wayDefaultPaintFill.setPathEffect(new DashPathEffect(new float[] { 20, 20 }, 0));

		Paint wayDefaultPaintOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
		wayDefaultPaintOutline.setStyle(Paint.Style.STROKE);
		wayDefaultPaintOutline.setColor(Color.BLUE);
		wayDefaultPaintOutline.setAlpha(128);
		wayDefaultPaintOutline.setStrokeWidth(7);
		wayDefaultPaintOutline.setStrokeJoin(Paint.Join.ROUND);
		ArrayWayOverlay wayOverlay = new ArrayWayOverlay(wayDefaultPaintFill, wayDefaultPaintOutline);
		
		GeoPoint geoPoint = new GeoPoint(51.219841, 6.7941); // DDorf Hbf
		GeoPoint geoPoint1 = new GeoPoint(51.224142, 6.795344);
//		GeoPoint geoPoint2 = new GeoPoint(51.229849, 6.7941); // Übeltäter erwischt
		GeoPoint geoPoint3 = new GeoPoint(51.223416, 6.794014); 
		GeoPoint geoPoint4 = new GeoPoint(51.222986, 6.793242);
		GeoPoint geoPoint5 = new GeoPoint(51.222529, 6.792469);
		GeoPoint geoPoint6 = new GeoPoint(51.221803, 6.791697);
		GeoPoint geoPoint7 = new GeoPoint(51.221346, 6.790924);
		GeoPoint geoPoint8 = new GeoPoint(51.220889, 6.790152);
		GeoPoint geoPoint9 = new GeoPoint(51.220379, 6.789465);
		GeoPoint geoPoint10 = new GeoPoint(51.219841, 6.788778);
		GeoPoint geoPoint11 = new GeoPoint(51.219303, 6.788092);
		
		Drawable defaultMarker = getResources().getDrawable(R.drawable.marker_red);
        ArrayItemizedOverlay itemizedOverlay = new ArrayItemizedOverlay(defaultMarker);
        OverlayItem item = new OverlayItem(geoPoint1, "Start", "Starting Point of Measurements.");
        OverlayItem item2 = new OverlayItem(geoPoint11, "End", "Ending Point of Measurements.");
        itemizedOverlay.addItem(item);
        itemizedOverlay.addItem(item2);
        mapView.getOverlays().add(itemizedOverlay);
		
		OverlayWay way1 = new OverlayWay(new GeoPoint[][] { { geoPoint1, geoPoint3, geoPoint4, geoPoint5, geoPoint6, geoPoint7, geoPoint8, geoPoint9, geoPoint10, geoPoint11} });
		wayOverlay.addWay(way1);
		mapView.getOverlays().add(wayOverlay);
		setContentView(mapView);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.map, menu);
		return true;
	}

}