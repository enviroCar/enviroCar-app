package org.envirocar.app.view.dashboard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.events.LocationChangedEvent;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.view.utils.MapUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * @author dewall
 */
public class DashboardTrackMapFragment extends BaseInjectorFragment {
    private static final Logger LOGGER = Logger.getLogger(DashboardTrackMapFragment.class);

    @InjectView(R.id.fragment_dashboard_frag_map_mapview)
    protected MapView mMapView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        LOGGER.info("onCreateView()");

        setHasOptionsMenu(true);

        // First inflate the general dashboard view.
        View contentView = inflater.inflate(R.layout.fragment_dashboard_frag_map, container, false);

        // Inject all dashboard-related views.
        ButterKnife.inject(this, contentView);

        // Init the map view
        mMapView.setTileSource(MapUtils.getOSMTileLayer());
        mMapView.setUserLocationEnabled(true);
        mMapView.setUserLocationTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW);
        mMapView.setUserLocationRequiredZoom(18);

        return contentView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // inflate the map menu for the dashboard when this fragment is visible.
        inflater.inflate(R.menu.menu_dashboard_map, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Subscribe
    public void onReceiveLocationChangedEvent(LocationChangedEvent event){
        LOGGER.info(String.format("Received event: %s", event.toString()));

    }

}