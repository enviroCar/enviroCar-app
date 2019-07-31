package org.envirocar.app.views.trackdetails;


import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.transition.TransitionManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.sources.TileSet;

import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.views.utils.MapUtils;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.logging.Logger;
import org.envirocar.storage.EnviroCarDB;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import rx.schedulers.Schedulers;

public class MapExpandedActivity extends BaseInjectorActivity {

    private static final Logger LOG = Logger.getLogger(MapExpandedActivity.class);
    protected static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("#.##");

    private static final String EXTRA_TRACKID = "org.envirocar.app.extraTrackID";
    private static final String EXTRA_TITLE = "org.envirocar.app.extraTitle";


    @BindView(R.id.activity_map_follow_fab)
    protected FloatingActionButton mCentreFab;

    @BindView(R.id.activity_map_visualise_fab)
    protected FloatingActionButton mVisualiseFab;

    @BindView(R.id.activity_track_details_expanded_map_cancel)
    protected ImageView mMapViewExpandedCancel;

    @BindView(R.id.activity_track_details_expanded_map)
    protected MapView mMapViewExpanded;

    @BindView(R.id.activity_track_details_expanded_map_container)
    protected ConstraintLayout mMapViewExpandedContainer;

    @BindView(R.id.legendCard)
    protected CardView legendCard;

    @BindView(R.id.legend)
    protected ImageView legend;

    @BindView(R.id.legend_start)
    protected TextView legendStart;

    @BindView(R.id.legend_mid)
    protected TextView legendMid;

    @BindView(R.id.legend_end)
    protected TextView legendEnd;

    @BindView(R.id.legend_unit)
    protected TextView legendUnit;

    @BindView(R.id.spinner)
    protected Spinner spinner;

    @Inject
    protected EnviroCarDB enviroCarDB;
    protected MapboxMap mapboxMapExpanded;
    protected TrackSpeedMapOverlay trackMapOverlay;
    private Track track;
    private Style style;
    private String[] spinnerOptions = {"CO2", "Consumption", "Engine Load", "Rpm", "Speed"};

    private boolean mIsCentredOnTrack;
    private boolean mIsGradientActive;

    public static void createInstance(Activity activity, int trackID) {
        Intent intent = new Intent(activity, MapExpandedActivity.class);
        intent.putExtra(EXTRA_TRACKID, trackID);
        activity.startActivity(intent);
    }
    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_expanded);
        // Inject all annotated views.
        ButterKnife.bind(this);
        mMapViewExpanded.onCreate(savedInstanceState);

        int trackID = getIntent().getIntExtra(EXTRA_TRACKID, -1);
        Track.TrackId trackid = new Track.TrackId(trackID);

        Track track = enviroCarDB.getTrack(trackid)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .first();
        this.track = track;
        trackMapOverlay = new TrackSpeedMapOverlay(track);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MapExpandedActivity.this,
                android.R.layout.simple_spinner_item, spinnerOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        initMapView();
        mIsCentredOnTrack = true;
        mIsGradientActive = false;
        mCentreFab.setVisibility(View.VISIBLE);
        mMapViewExpandedCancel.setOnClickListener(v-> finish());

    }

    @OnTouch(R.id.activity_track_details_expanded_map)
    protected boolean onTouchMapView() {
        if (mIsCentredOnTrack) {
            mIsCentredOnTrack = false;
            TransitionManager.beginDelayedTransition(mMapViewExpandedContainer,new androidx.transition.Slide(Gravity.RIGHT));
            mCentreFab.setVisibility(View.VISIBLE);
        }
        return false;
    }

    @OnClick(R.id.activity_map_follow_fab)
    protected void onClickFollowFab() {
        final LatLngBounds viewBbox = trackMapOverlay.getViewBoundingBox();
        if (!mIsCentredOnTrack) {
            mIsCentredOnTrack = true;
            TransitionManager.beginDelayedTransition(mMapViewExpandedContainer,new androidx.transition.Slide(Gravity.LEFT));
            mCentreFab.setVisibility(View.GONE);
            mapboxMapExpanded.easeCamera(CameraUpdateFactory.newLatLngBounds(viewBbox, 50),2500);
        }
    }

    @OnClick(R.id.activity_map_visualise_fab)
    protected void onClickVisualiseFab() {
        LOG.info("onClickVisualiseFab");
        makeMapChanges();

    }

    private void makeMapChanges(){
        final LatLngBounds viewBbox = trackMapOverlay.getViewBoundingBox();
        if(mapboxMapExpanded != null)
        {
            if(!mIsGradientActive)
            {
                legendCard.setVisibility(View.VISIBLE);
                mIsGradientActive = true;
                mapboxMapExpanded.getStyle(new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        style.addSource(trackMapOverlay.getGradientGeoJSONSource());
                        style.addLayerBelow(trackMapOverlay.getGradientLineLayer(Measurement.PropertyKey.SPEED), "marker-layer1");
                        legendStart.setText(DECIMAL_FORMATTER.format(trackMapOverlay.getGradMin()));
                        legendEnd.setText(DECIMAL_FORMATTER.format(trackMapOverlay.getGradMax()));
                        Float mid = (trackMapOverlay.getGradMin() + trackMapOverlay.getGradMax())/2;
                        legendMid.setText(DECIMAL_FORMATTER.format(mid));
                        legendUnit.setText("km/h");
                    }
                });
            }
            else{
                legendCard.setVisibility(View.GONE);
                mIsGradientActive = false;
                mapboxMapExpanded.getStyle(new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        style.removeLayer(TrackSpeedMapOverlay.GRADIENT_LAYER);
                        style.removeSource(TrackSpeedMapOverlay.GRADIENT_SOURCE);
                    }
                });
            }
            mapboxMapExpanded.easeCamera(CameraUpdateFactory.newLatLngBounds(viewBbox, 50));
        }
    }

    @Override
    public void onBackPressed() {
            super.onBackPressed();
    }

    private void initMapView() {
        final LatLngBounds viewBbox = trackMapOverlay.getViewBoundingBox();
        TileSet layer = MapUtils.getOSMTileLayer();
        mMapViewExpanded.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap mapboxMap1) {
                mapboxMap1.getUiSettings().setLogoEnabled(false);
                mapboxMap1.getUiSettings().setAttributionEnabled(false);
                mapboxMap1.setStyle(new Style.Builder().fromUrl("https://api.maptiler.com/maps/basic/style.json?key=YJCrA2NeKXX45f8pOV6c "), new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        MapExpandedActivity.this.style = style;
                        style.addSource(trackMapOverlay.getGeoJsonSource());
                        style.addLayer(trackMapOverlay.getLineLayer());
                        mapboxMap1.moveCamera(CameraUpdateFactory.newLatLngBounds(viewBbox, 50));
                        setUpStartStopIcons(style, mapboxMap1);
                    }
                });
                mapboxMapExpanded = mapboxMap1;
                mapboxMapExpanded.setMaxZoomPreference(18);
                mapboxMapExpanded.setMinZoomPreference(1);
            }
        });
    }

    private void setUpStartStopIcons(@NonNull Style loadedMapStyle, @NonNull MapboxMap loadedMapBox) {
        loadedMapStyle.addImage("stop-marker",
                BitmapFactory.decodeResource(
                        this.getResources(), R.drawable.stop_marker));

        loadedMapStyle.addImage("start-marker",
                BitmapFactory.decodeResource(
                        this.getResources(), R.drawable.start_marker));

        int size = track.getMeasurements().size();
        List<Point> markerCoordinates = new ArrayList<>();
        if(size>=2)
        {
            markerCoordinates.add(Point.fromLngLat(track.getMeasurements().get(0).getLongitude(),track.getMeasurements().get(0).getLatitude()));
            markerCoordinates.add(Point.fromLngLat(track.getMeasurements().get(size-1).getLongitude(),track.getMeasurements().get(size-1).getLatitude()));

            GeoJsonSource geoJsonSource = new GeoJsonSource("marker-source1", Feature.fromGeometry(
                    Point.fromLngLat(markerCoordinates.get(0).longitude(), markerCoordinates.get(0).latitude())));
            loadedMapStyle.addSource(geoJsonSource);
            SymbolLayer symbolLayer = new SymbolLayer("marker-layer1", "marker-source1");
            symbolLayer.withProperties(
                    PropertyFactory.iconImage("start-marker"),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true)
            );
            loadedMapStyle.addLayer(symbolLayer);

            geoJsonSource = new GeoJsonSource("marker-source2", Feature.fromGeometry(
                    Point.fromLngLat(markerCoordinates.get(1).longitude(), markerCoordinates.get(1).latitude())));
            loadedMapStyle.addSource(geoJsonSource);

            symbolLayer = new SymbolLayer("marker-layer2", "marker-source2");
            symbolLayer.withProperties(
                    PropertyFactory.iconImage("stop-marker"),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true)
            );
            loadedMapStyle.addLayerAbove(symbolLayer, "marker-layer1");

        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mMapViewExpanded.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        supportStartPostponedEnterTransition();
        mMapViewExpanded.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapViewExpanded.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMapViewExpanded.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapViewExpanded.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        style.removeLayer(MapLayer.LAYER_NAME);
        style.removeLayer("marker-layer1");
        style.removeLayer("marker-layer2");
        mMapViewExpanded.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapViewExpanded.onSaveInstanceState(outState);
    }

}
