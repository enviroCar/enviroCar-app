package org.envirocar.app.view.dashboard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.bluetooth.obd.events.SpeedUpdateEvent;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.view.preferences.Tempomat;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * @author dewall
 */
public class DashboardTempomatFragment extends BaseInjectorFragment {
    private static final Logger LOGGER = Logger.getLogger(DashboardTempomatFragment.class);

    @InjectView(R.id.fragment_dashboard_tempomat_view)
    protected Tempomat mTempomatView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        LOGGER.info("onCreateView()");

        setHasOptionsMenu(true);

        // First inflate the general dashboard view.
        View contentView = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Inject all dashboard-related views.
        ButterKnife.inject(this, contentView);

        // return the inflated content view.
        return contentView;
    }

    @Override
    public void onResume() {
        LOGGER.info("onResume()");
        super.onResume();
    }

    @Override
    public void onPause(){
        LOGGER.info("onPause()");
        super.onPause();
    }

    @Override
    public void onDestroy() {
        LOGGER.info("onDestroy()");
        super.onDestroy();
        mTempomatView.destroyDrawingCache();
        mTempomatView = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_dashboard_tempomat, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Receiver method for the speed update event.
     *
     * @param event the SpeedUpdateEvent to receive over the bus.
     */
    @Subscribe
    public void onReceiveSpeedUpdateEvent(SpeedUpdateEvent event) {
        LOGGER.debug(String.format("Received event: %s", event.toString()));
        if(mTempomatView != null){
            mTempomatView.setSpeed(event.mSpeed);
        }
    }

}