package org.envirocar.app.fragments;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Stopwatch;

import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.logging.Logger;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * @author dewall
 */
public class NewDashboardFragment extends BaseInjectorFragment {
    private static final Logger LOGGER = Logger.getLogger(NewDashboardFragment.class);

    @InjectView(R.id.fragment_dashboard_header_gps_image)
    protected ImageView mGpsImage;
    @InjectView(R.id.fragment_dashboard_header_gps_text)
    protected TextView mGpsText;

    @InjectView(R.id.fragment_dashboard_header_bt_image)
    protected ImageView mBluetoothImage;
    @InjectView(R.id.fragment_dashboard_header_bt_text)
    protected TextView mBluetoothText;

    @InjectView(R.id.fragment_dashboard_header_time_timer)
    protected Chronometer mTimerText;
    @InjectView(R.id.fragment_dashboard_header_speed_text)
    protected TextView mSpeedText;
    @InjectView(R.id.fragment_dashboard_header_distance_text)
    protected TextView mDistanceText;


    private Stopwatch mStopwatch;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        LOGGER.info("onCreateView()");

        // First inflate the general dashboard view.
        View contentView = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Inject all dashboard-related views.
        ButterKnife.inject(this, contentView);

        // return the inflated content view.
        return contentView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        LOGGER.info("onViewCreated()");

//        mStopwatch.st
        mTimerText.setBase(SystemClock.elapsedRealtime()-10000);
        mTimerText.start();

        super.onViewCreated(view, savedInstanceState);
    }
}
