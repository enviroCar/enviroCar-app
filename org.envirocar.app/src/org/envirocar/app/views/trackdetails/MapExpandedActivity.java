package org.envirocar.app.views.trackdetails;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.transition.ChangeBounds;
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

import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.EnviroCarDB;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import io.reactivex.schedulers.Schedulers;

import static android.view.View.GONE;

public class MapExpandedActivity extends BaseInjectorActivity {
    private static final Logger LOG = Logger.getLogger(MapExpandedActivity.class);

    protected static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("#.##");
    private static final String EXTRA_TRACKID = "org.envirocar.app.extraTrackID";
    private static final String EXTRA_TITLE = "org.envirocar.app.extraTitle";

    @Inject
    protected EnviroCarDB enviroCarDB;

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
    protected TextView legendName;

    protected MapboxMap mapboxMapExpanded;
    protected TrackMapLayer trackMapOverlay;
    private Track track;
    private Style style;
    private List<Measurement.PropertyKey> options = new ArrayList<>();
    private List<String> spinnerStrings = new ArrayList<>();
    private boolean mIsCentredOnTrack;

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

        // Get the track to show.
        int trackID = getIntent().getIntExtra(EXTRA_TRACKID, -1);
        Track.TrackId trackid = new Track.TrackId(trackID);
        Track track = enviroCarDB.getTrack(trackid)
                .subscribeOn(Schedulers.io())
                .blockingFirst();
        this.track = track;

        trackMapOverlay = new TrackMapLayer(track);

        options = track.getSupportedProperties();
        for (Measurement.PropertyKey propertyKey : options) {
            spinnerStrings.add(propertyKey.toString());
        }
        spinnerStrings.add("None");

        initMapView();

        mIsCentredOnTrack = true;
        mCentreFab.show();
        mMapViewExpandedCancel.setOnClickListener(v -> finish());
    }

    @OnTouch(R.id.activity_track_details_expanded_map)
    protected boolean onTouchMapView() {
        if (mIsCentredOnTrack) {
            mIsCentredOnTrack = false;
            TransitionManager.beginDelayedTransition(mMapViewExpandedContainer, new androidx.transition.Slide(Gravity.RIGHT));
            mCentreFab.show();
        }
        return false;
    }

    @OnClick(R.id.activity_map_follow_fab)
    protected void onClickFollowFab() {
        final LatLngBounds viewBbox = trackMapOverlay.getViewBoundingBox();
        if (!mIsCentredOnTrack) {
            mIsCentredOnTrack = true;
            TransitionManager.beginDelayedTransition(mMapViewExpandedContainer, new androidx.transition.Slide(Gravity.RIGHT));
            mCentreFab.hide();
            mapboxMapExpanded.easeCamera(CameraUpdateFactory.newLatLngBounds(viewBbox, 50), 2500);
        }
    }

    @OnClick(R.id.activity_map_visualise_fab)
    protected void onClickVisualiseFab() {
        LOG.info("onClickVisualiseFab");
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Phenomena to Visualise");
        b.setItems(spinnerStrings.toArray(new String[0]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                LOG.info("Choice: " + i);
                makeMapChanges(i);
            }
        });
        b.show();

    }

    private void makeMapChanges(int choice) {
        final LatLngBounds viewBbox = trackMapOverlay.getViewBoundingBox();
        if (mapboxMapExpanded != null) {
            LOG.info("Choice: " + choice);
            if (!spinnerStrings.get(choice).equalsIgnoreCase("None")) {
                if (legendCard.getVisibility() != View.VISIBLE) {
                    TransitionManager.beginDelayedTransition(legendCard, new androidx.transition.Slide(Gravity.LEFT));
                    legendCard.setVisibility(View.VISIBLE);
                } else {
                    TransitionManager.beginDelayedTransition(legendCard, new ChangeBounds());
                }

                mapboxMapExpanded.getStyle(new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        //Remove current gradient layer
                        style.removeLayer(TrackMapLayer.GRADIENT_LAYER);
                        style.removeSource(TrackMapLayer.GRADIENT_SOURCE);

                        //Add new gradient layer based on choice of data
                        style.addSource(trackMapOverlay.getGradientGeoJSONSource());
                        style.addLayerBelow(trackMapOverlay.getGradientLineLayer(options.get(choice)), "marker-layer1");

                        //Set legend values
                        legendStart.setText(DECIMAL_FORMATTER.format(trackMapOverlay.getGradMin()));
                        legendEnd.setText(DECIMAL_FORMATTER.format(trackMapOverlay.getGradMax()));
                        Float mid = (trackMapOverlay.getGradMin() + trackMapOverlay.getGradMax()) / 2;
                        legendMid.setText(DECIMAL_FORMATTER.format(mid));
                        legendName.setText(options.get(choice).getStringResource());
                    }
                });
            } else {
                //None gradient chosen. So remove the gradient layers
                TransitionManager.beginDelayedTransition(legendCard, new androidx.transition.Slide(Gravity.LEFT));
                legendCard.setVisibility(GONE);
                mapboxMapExpanded.getStyle(new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        style.removeLayer(TrackMapLayer.GRADIENT_LAYER);
                        style.removeSource(TrackMapLayer.GRADIENT_SOURCE);
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
        mMapViewExpanded.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap mapboxMap1) {
                mapboxMap1.getUiSettings().setLogoEnabled(false);
                mapboxMap1.getUiSettings().setAttributionEnabled(false);
                mapboxMap1.setStyle(new Style.Builder().fromUrl("https://api.maptiler.com/maps/basic/style.json?key=YJCrA2NeKXX45f8pOV6c "), new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        MapExpandedActivity.this.style = style;
                        //Set normal source and line layer
                        style.addSource(trackMapOverlay.getGeoJsonSource());
                        style.addLayer(trackMapOverlay.getLineLayer());

                        mapboxMap1.moveCamera(CameraUpdateFactory.newLatLngBounds(viewBbox, 50));
                        setUpStartStopIcons(style);

                        if (options.contains(Measurement.PropertyKey.SPEED)) {
                            makeMapChanges(options.indexOf(Measurement.PropertyKey.SPEED));
                        } else {
                            makeMapChanges(options.indexOf(Measurement.PropertyKey.GPS_SPEED));
                        }
                    }
                });
                mapboxMapExpanded = mapboxMap1;
                mapboxMapExpanded.setMaxZoomPreference(18);
                mapboxMapExpanded.setMinZoomPreference(1);
            }
        });
    }

    private void setUpStartStopIcons(@NonNull Style loadedMapStyle) {
        int size = track.getMeasurements().size();
        if (size >= 2) {
            //Set Source with start and stop marker
            Double lng = track.getMeasurements().get(0).getLongitude();
            Double lat = track.getMeasurements().get(0).getLatitude();
            GeoJsonSource geoJsonSource = new GeoJsonSource("marker-source1", Feature.fromGeometry(
                    Point.fromLngLat(lng, lat)));
            loadedMapStyle.addSource(geoJsonSource);

            lng = track.getMeasurements().get(size - 1).getLongitude();
            lat = track.getMeasurements().get(size - 1).getLatitude();
            geoJsonSource = new GeoJsonSource("marker-source2", Feature.fromGeometry(
                    Point.fromLngLat(lng, lat)));
            loadedMapStyle.addSource(geoJsonSource);

            //Set symbol layer to set the icons to be displayed at the start and stop
            loadedMapStyle.addImage("start-marker",
                    BitmapFactory.decodeResource(
                            this.getResources(), R.drawable.start_marker));
            SymbolLayer symbolLayer = new SymbolLayer("marker-layer1", "marker-source1");
            symbolLayer.withProperties(
                    PropertyFactory.iconImage("start-marker"),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true)
            );
            loadedMapStyle.addLayer(symbolLayer);

            loadedMapStyle.addImage("stop-marker",
                    BitmapFactory.decodeResource(
                            this.getResources(), R.drawable.stop_marker));
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
        if (style != null) {
            style.removeLayer(MapLayer.LAYER_NAME);
            style.removeLayer("marker-layer1");
            style.removeLayer("marker-layer2");
        }
        if (mMapViewExpanded != null)
            mMapViewExpanded.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapViewExpanded.onSaveInstanceState(outState);
    }

}
