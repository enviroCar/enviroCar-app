///*
// * enviroCar 2013
// * Copyright (C) 2013
// * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
// *
// * This program is free software; you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation; either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software Foundation,
// * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
// *
// */
//
//package org.envirocar.app.activity;
//
//import org.envirocar.core.logging.Logger;
//import org.mapsforge.android.maps.MapActivity;
//import org.mapsforge.android.maps.MapView;
//import org.mapsforge.android.maps.Projection;
//import org.mapsforge.android.maps.mapgenerator.tiledownloader.MapnikTileDownloader;
//import org.mapsforge.android.maps.overlay.Overlay;
//import org.mapsforge.core.GeoPoint;
//
//import android.graphics.Bitmap;
//import android.graphics.Canvas;
//import android.graphics.Color;
//import android.graphics.Paint;
//import android.graphics.Point;
//import android.graphics.RadialGradient;
//import android.graphics.Shader.TileMode;
//import android.os.Bundle;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.View.OnTouchListener;
//
///**
// * Prototype implementation of a heatmap using the mapsforge API. The
// * zoom/panning event-handling for the heatmap overlay is not working correctly
// * yet. Try to implement double image buffering techniques to improve event
// * handling for panning.
// */
//
//public class HeatMap extends MapActivity {
//
//	private static final Logger logger = Logger.getLogger(HeatMap.class);
//
//	private HeatMapOverlay heatMapOverlay;
//
//	@Override
//	protected void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//
//		MapView mapView = new MapView(this);
//		mapView.setClickable(true);
//		mapView.setBuiltInZoomControls(true);
//		mapView.setMapGenerator(new MapnikTileDownloader());
//
//		heatMapOverlay = new HeatMapOverlay();
//		mapView.getOverlays().add(heatMapOverlay);
//		mapView.setOnTouchListener(heatMapOverlay.touchListener);
//		setContentView(mapView);
//	}
//
//	private class HeatMapOverlay extends Overlay {
//
//		private Canvas myCanvas;
//		private Bitmap backbuffer;
//		private float radius;
//
//		// TODO auto config display width and height
//		private int width = 480;
//		private int height = 800;
//
//		private Projection projection;
//
//		@Override
//		protected void drawOverlayBitmap(Canvas canvas, Point point,
//				Projection projection, byte zoom) {
//
//			this.projection = projection;
//
//			if (backbuffer == null) {
//				init();
//				draw(canvas);
//			}
//
//			Point screenPts = new Point();
//			// projection.toPoint(new GeoPoint(51.219841, 6.7941), screenPts,a);
//			projection.toPixels(new GeoPoint(51.224142, 6.795344), screenPts);
//			// Log.e("Pixel", String.valueOf(screenPts.x) + " " +
//			// String.valueOf(screenPts.y));
//			canvas.drawBitmap(backbuffer, screenPts.x - 180, screenPts.y - 300,
//					new Paint(Paint.ANTI_ALIAS_FLAG));
//			init();
//			// canvas.drawBitmap(backbuffer, 0, 0, new
//			// Paint(Paint.ANTI_ALIAS_FLAG));
//		}
//
//		private void init() {
//			this.radius = 40f;
//			backbuffer = Bitmap.createBitmap(this.width, this.height,
//					Bitmap.Config.ARGB_8888);
//			myCanvas = new Canvas(backbuffer);
//			Paint p = new Paint();
//			p.setStyle(Paint.Style.FILL);
//			p.setColor(Color.TRANSPARENT);
//			myCanvas.drawRect(0, 0, width, height, p);
//
//			GeoPoint geoPoint = new GeoPoint(51.219841, 6.7941);
//			Point pts = new Point();
//			this.projection.toPixels(geoPoint, pts);
//			logger.debug("Pixel0 "+ String.valueOf(pts.x) + " " + String.valueOf(pts.y));
//			for (int i = 0; i < 50; i++) {
//				addPoint(pts.x + 10, pts.y + 10);
//			}
//
//			GeoPoint geoPoint1 = new GeoPoint(51.224142, 6.795344);
//			Point pts1 = new Point();
//			this.projection.toPixels(geoPoint1, pts1);
//			logger.debug("Pixel1 "+
//					String.valueOf(pts1.x) + " " + String.valueOf(pts1.y));
//			for (int i = 0; i < 10; i++) {
//				addPoint(pts1.x + 10, pts1.y + 10);
//			}
//
//			GeoPoint geoPoint2 = new GeoPoint(51.229849, 6.7941);
//			Point pts2 = new Point();
//			this.projection.toPixels(geoPoint2, pts2);
//			logger.debug("Pixel2 "+
//					String.valueOf(pts2.x) + " " + String.valueOf(pts2.y));
//			for (int i = 0; i < 20; i++) {
//				addPoint(pts2.x + 10, pts2.y + 10);
//			}
//
//			GeoPoint geoPoint3 = new GeoPoint(51.223416, 6.794014);
//			Point pts3 = new Point();
//			this.projection.toPixels(geoPoint3, pts3);
//			logger.debug("Pixel3 "+
//					String.valueOf(pts3.x) + " " + String.valueOf(pts3.y));
//			for (int i = 0; i < 30; i++) {
//				addPoint(pts3.x, pts3.y);
//			}
//
//			GeoPoint geoPoint4 = new GeoPoint(51.222986, 6.793242);
//			Point pts4 = new Point();
//			this.projection.toPixels(geoPoint4, pts4);
//			for (int i = 0; i < 40; i++) {
//				addPoint(pts4.x, pts4.y);
//			}
//
//			GeoPoint geoPoint5 = new GeoPoint(51.222529, 6.792469);
//			Point pts5 = new Point();
//			this.projection.toPixels(geoPoint5, pts5);
//			for (int i = 0; i < 50; i++) {
//				addPoint(pts5.x, pts5.y);
//			}
//			GeoPoint geoPoint6 = new GeoPoint(51.221803, 6.791697);
//			Point pts6 = new Point();
//			this.projection.toPixels(geoPoint6, pts6);
//			for (int i = 0; i < 60; i++) {
//				addPoint(pts6.x, pts6.y);
//			}
//			GeoPoint geoPoint7 = new GeoPoint(51.221346, 6.790924);
//			Point pts7 = new Point();
//			this.projection.toPixels(geoPoint7, pts7);
//			for (int i = 0; i < 20; i++) {
//				addPoint(pts7.x, pts7.y);
//			}
//			GeoPoint geoPoint8 = new GeoPoint(51.220889, 6.790152);
//			Point pts8 = new Point();
//			this.projection.toPixels(geoPoint8, pts8);
//			for (int i = 0; i < 20; i++) {
//				addPoint(pts8.x, pts8.y);
//			}
//			GeoPoint geoPoint9 = new GeoPoint(51.220379, 6.789465);
//			Point pts9 = new Point();
//			this.projection.toPixels(geoPoint9, pts9);
//			for (int i = 0; i < 20; i++) {
//				addPoint(pts9.x, pts9.y);
//			}
//			GeoPoint geoPoint10 = new GeoPoint(51.219841, 6.788778);
//			Point pts10 = new Point();
//			this.projection.toPixels(geoPoint10, pts10);
//			for (int i = 0; i < 20; i++) {
//				addPoint(pts10.x, pts10.y);
//			}
//			GeoPoint geoPoint11 = new GeoPoint(51.219303, 6.788092);
//			Point pts11 = new Point();
//			this.projection.toPixels(geoPoint11, pts11);
//			for (int i = 0; i < 20; i++) {
//				addPoint(pts11.x, pts11.y);
//			}
//		}
//
//		public void addPoint(int x, int y) {
//			RadialGradient g = new RadialGradient(x, y, radius, Color.argb(10,
//					0, 0, 0), Color.TRANSPARENT, TileMode.CLAMP);
//			Paint gp = new Paint();
//			gp.setShader(g);
//			myCanvas.drawCircle(x, y, radius, gp);
//			colorize(x - radius, y - radius, radius * 2);
//			// invalidate();
//		}
//
//		private void colorize(float x, float y, float d) {
//			if (x + d > myCanvas.getWidth()) {
//				x = myCanvas.getWidth() - d;
//			}
//			if (x < 0) {
//				x = 0;
//			}
//			if (y < 0) {
//				y = 0;
//			}
//			if (y + d > myCanvas.getHeight()) {
//				y = myCanvas.getHeight() - d;
//			}
//
//			int[] pixels = new int[(int) (d * d)];
//
//			backbuffer.getPixels(pixels, 0, (int) d, (int) x, (int) y, (int) d,
//					(int) d);
//
//			for (int i = 0; i < pixels.length; i++) {
//				int r = 0, g = 0, b = 0, tmp = 0;
//				int alpha = pixels[i] >>> 24;
//				if (alpha <= 255 && alpha >= 240) {
//					tmp = 255 - alpha;
//					r = 255 - tmp;
//					g = tmp * 12;
//				} else if (alpha <= 239 && alpha >= 200) {
//					tmp = 234 - alpha;
//					r = 255 - (tmp * 8);
//					g = 255;
//				} else if (alpha <= 199 && alpha >= 150) {
//					tmp = 199 - alpha;
//					g = 255;
//					b = tmp * 5;
//				} else if (alpha <= 149 && alpha >= 100) {
//					tmp = 149 - alpha;
//					g = 255 - (tmp * 5);
//					b = 255;
//				} else
//					b = 255;
//				pixels[i] = Color.argb(alpha, r, g, b);
//			}
//
//			backbuffer.setPixels(pixels, 0, (int) d, (int) x, (int) y, (int) d,
//					(int) d);
//		}
//
//		public OnTouchListener touchListener = new OnTouchListener() {
//			public boolean onTouch(View v, MotionEvent event) {
////				int i = (int) event.getX();
////				int j = (int) event.getY();
////				Point p = new Point(i, j);
//				// screenPts.x = i;
//				// screenPts.y = j;
//				return false;
//			}
//		};
//
//	}
//}