package org.envirocar.app.view.dashboard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.mapbox.mapboxsdk.overlay.PathOverlay;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.services.trackdetails.TrackPathOverlayEvent;
import org.envirocar.app.view.utils.MapUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnTouch;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

/**
 * @author dewall
 */
public class DashboardTrackMapFragment extends BaseInjectorFragment {
    private static final Logger LOGGER = Logger.getLogger(DashboardTrackMapFragment.class);

    @InjectView(R.id.fragment_dashboard_frag_map_mapview)
    protected MapView mMapView;
    @InjectView(R.id.fragment_dashboard_frag_map_follow_fab)
    protected FloatingActionButton mFollowFab;

    private PathOverlay mPathOverlay;

    private final Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread()
            .createWorker();

    private boolean mIsFollowingLocation;

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
        mMapView.setUserLocationTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW_BEARING);
        mMapView.setUserLocationRequiredZoom(18);
        mIsFollowingLocation = true;
        mFollowFab.setVisibility(View.INVISIBLE);

        //        mMapView.setOnTouchListener(new View.OnTouchListener() {
        //            @Override
        //            public boolean onTouch(View v, MotionEvent event) {
        //                mMapView.getUserLocationOverlay().disableFollowLocation();
        //                mFollowFab.setVisibility(View.VISIBLE);
        //                return false;
        //            }
        //        });

        // If the mPathOverlay has already been set, then add the overlay to the mapview.
        if (mPathOverlay != null)
            mMapView.getOverlays().add(mPathOverlay);

        return contentView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // inflate the map menu for the dashboard when this fragment is visible.
        inflater.inflate(R.menu.menu_dashboard_map, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @OnTouch(R.id.fragment_dashboard_frag_map_mapview)
    protected boolean onTouchMapView() {
        if (mIsFollowingLocation) {
            // Disable the follow location mode.
            UserLocationOverlay userLocationOverlay = mMapView.getUserLocationOverlay();
            userLocationOverlay.disableFollowLocation();
            mIsFollowingLocation = false;

            // show the floating action button that can enable the follow location mode.
            showFollowFAB();
        }
        return false;
    }

    @OnClick(R.id.fragment_dashboard_frag_map_follow_fab)
    protected void onClickFollowFab() {
        if (!mIsFollowingLocation) {
            UserLocationOverlay userLocationOverlay = mMapView.getUserLocationOverlay();
            userLocationOverlay.enableFollowLocation();
            userLocationOverlay.goToMyPosition(true); // animated is not working... don't know why
            mIsFollowingLocation = true;

            hideFollowFAB();
        }
    }

    /**
     * Shows the floating action button for toggling the follow location ability.
     */
    private void showFollowFAB() {
        // load the translate animation.
        Animation slideLeft = AnimationUtils.loadAnimation(getActivity(),
                R.anim.translate_slide_in_right);

        // and start it on the fab.
        mFollowFab.startAnimation(slideLeft);
        mFollowFab.setVisibility(View.VISIBLE);
    }

    /**
     *
     */
    private void hideFollowFAB() {
        // load the translate animation.
        Animation slideRight = AnimationUtils.loadAnimation(getActivity(),
                R.anim.translate_slide_out_right);

        // set a listener that makes the button invisible when the animation has finished.
        slideRight.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // nothing to do..
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mFollowFab.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // nothing to do..
            }
        });

        // and start it on the fab.
        mFollowFab.startAnimation(slideRight);
    }

    @Subscribe
    public void onReceivePathOverlayEvent(TrackPathOverlayEvent event) {
        mMainThreadWorker.schedule(() -> {
            mPathOverlay = event.mTrackOverlay;
            if (mMapView != null)
                mMapView.addOverlay(mPathOverlay);
        });
    }

}