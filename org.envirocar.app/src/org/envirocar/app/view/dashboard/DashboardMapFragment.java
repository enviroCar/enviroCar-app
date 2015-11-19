package org.envirocar.app.view.dashboard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.views.MapView;

import org.envirocar.app.R;
import org.envirocar.app.view.utils.MapUtils;
import org.envirocar.core.injection.BaseInjectorFragment;
import org.envirocar.core.logging.Logger;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class DashboardMapFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(DashboardMapFragment.class);

    @InjectView(R.id.fragment_dashboard_frag_map_mapview)
    protected MapView mMapView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        LOG.info("onCreateView()");

        // First inflate the general dashboard view.
        View contentView = inflater.inflate(R.layout.fragment_dashboard_frag_map, container, false);

        // Inject all dashboard-related views.
        ButterKnife.inject(this, contentView);

        // Init the map view
        mMapView.setTileSource(MapUtils.getOSMTileLayer());
        mMapView.setDiskCacheEnabled(true);
        mMapView.setZoom(0);

        return contentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.setUserLocationEnabled(true);
        mMapView.setUserLocationTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW);
        mMapView.setUserLocationRequiredZoom(18);
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.setUserLocationEnabled(false);
        mMapView.setUserLocationTrackingMode(UserLocationOverlay.TrackingMode.NONE);
    }
}
