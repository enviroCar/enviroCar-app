/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.recordingscreen;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.fragment.app.FragmentTransaction;

import android.preference.PreferenceManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.events.AvrgSpeedUpdateEvent;
import org.envirocar.app.events.DistanceValueUpdateEvent;
import org.envirocar.app.events.DrivingDetectedEvent;
import org.envirocar.app.events.StartingTimeEvent;
import org.envirocar.app.handler.PreferenceConstants;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.handler.TrackRecordingHandler;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.main.BaseMainActivityBottomBar;
import org.envirocar.app.main.MainActivityComponent;
import org.envirocar.app.main.MainActivityModule;
import org.envirocar.app.services.recording.GPSOnlyRecordingService;
import org.envirocar.core.events.gps.GpsSatelliteFixEvent;
import org.envirocar.core.logging.Logger;
import org.envirocar.obd.events.TrackRecordingServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;

import java.text.DecimalFormat;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class GPSOnlyTrackRecordingScreen extends BaseInjectorActivity {

    private static final Logger LOGGER = Logger.getLogger(GPSOnlyTrackRecordingScreen.class);

    private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("###.#");

    @BindView(R.id.mGpsImage)
    protected ImageView mGpsImage;
    @BindView(R.id.mBluetoothImage)
    protected ImageView mBluetoothImage;
    @BindView(R.id.displayBluetoothCarDriving)
    protected TextView displayBluetoothCarDriving;
    @BindView(R.id.mTimerText)
    protected Chronometer mTimerText;
    @BindView(R.id.mDistanceText)
    protected TextView mDistanceText;
    @BindView(R.id.mSpeedText)
    protected TextView mSpeedText;
    @BindView(R.id.switchMetersButton)
    protected LinearLayout switchMetersButton;
    @BindView(R.id.trackDetailsContainer)
    protected LinearLayout trackDetailsContainer;
    @BindView(R.id.trackMapContainer)
    protected LinearLayout trackMapContainer;
    @BindView(R.id.trackSingleMeterContainer)
    protected LinearLayout trackSingleMeterContainer;
    @BindView(R.id.stopTrackRecordingButton)
    protected LinearLayout stopTrackRecordingButton;


    @Inject
    protected TrackRecordingHandler mTrackRecordingHandler;


    //viewTypeInGeneral = 1 means meter view
    //viewTypeInGeneral = 2 means map view
    private static int viewTypeInGeneral = 2;

    private Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        MainActivityComponent mainActivityComponent =  baseApplicationComponent.plus(new MainActivityModule(this));
        mainActivityComponent.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obdplus_gpstrack_recording_screen);
        //if the track recording service is stopped then finish this activity and goback to bottombar main activity
        if(GPSOnlyRecordingService.CURRENT_SERVICE_STATE == BluetoothServiceState.SERVICE_STOPPED){
            startActivity(new Intent(GPSOnlyTrackRecordingScreen.this, BaseMainActivityBottomBar.class));
            finish();
        }

        // Inject all dashboard-related views.
        ButterKnife.bind(this);

        // set keep screen on setting
        this.trackDetailsContainer.setKeepScreenOn(PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean(PreferenceConstants.DISPLAY_STAYS_ACTIV, false));

        switchMetersButton.setVisibility(View.GONE);
        displayBluetoothCarDriving.setText(R.string.driving);
        mBluetoothImage.setImageResource(R.drawable.not_driving);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.trackMapContainer, new TrackMapFragment());
        fragmentTransaction.add(R.id.trackSingleMeterContainer, new TempomatFragment());
        fragmentTransaction.commit();

        initAnimations();
        updateDrivingViews(GPSOnlyRecordingService.drivingDetected);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //if the track recording service is stopped then finish this activity and goback to bottombar main activity
        if(GPSOnlyRecordingService.CURRENT_SERVICE_STATE == BluetoothServiceState.SERVICE_STOPPED){
            startActivity(new Intent(GPSOnlyTrackRecordingScreen.this, BaseMainActivityBottomBar.class));
            finish();
        }
    }


    @Subscribe
    public void onReceiveTrackRecordingServiceStateChangedEvent(
            TrackRecordingServiceStateChangedEvent event) {
        LOGGER.info(String.format("Received event: %s", event.toString()));
        mMainThreadWorker.schedule(() -> {
            if (event.mState == BluetoothServiceState.SERVICE_STOPPED) {
                mTimerText.setBase(SystemClock.elapsedRealtime());
                mTimerText.stop();
                mDistanceText.setText("0.0 km");
                mSpeedText.setText("0 km/h");
                finish();
            }
        });
    }

    @Subscribe
    public void onReceiveAvrgSpeedUpdateEvent(AvrgSpeedUpdateEvent event) {
        mMainThreadWorker.schedule(() -> mSpeedText.setText(String.format("%s km/h",
                Integer.toString(event.mAvrgSpeed))));
    }

    @Subscribe
    public void onReceiveDrivingDetectedEvent(DrivingDetectedEvent event) {
        mMainThreadWorker.schedule(() -> {
            updateDrivingViews(event.mDrivingDetected);
        });
    }

    @Subscribe
    public void onReceiveDistanceUpdateEvent(DistanceValueUpdateEvent event) {
        mMainThreadWorker.schedule(() ->
                mDistanceText.setText(String.format("%s km",
                        DECIMAL_FORMATTER.format(event.mDistanceValue))));
    }

    @Subscribe
    public void onReceiveGpsSatelliteFixEvent(GpsSatelliteFixEvent event) {
        LOGGER.info(String.format("Received event: %s", event.toString()));
        updateLocationViews(event.mGpsSatelliteFix.isFix());
    }

    @Subscribe
    public void onReceiveStartingTimeEvent(StartingTimeEvent event) {
        mMainThreadWorker.schedule(() -> {
            mTimerText.setBase(event.mStartingTime);
            if(event.mIsStarted)
                mTimerText.start();
            else
                mTimerText.stop();
        });
    }

    @OnClick(R.id.switchViewsButton)
    protected void onSwitchViewsButtonClicked(){
        if(viewTypeInGeneral == 1) viewTypeInGeneral = 2;
        else viewTypeInGeneral = 1;
        PreferencesHandler.setPreviousViewTypeGeneralRecordingScreen(this,viewTypeInGeneral);
        updateTheDisplayViewsGeneral();
    }

    @OnClick(R.id.stopTrackRecordingButton)
    protected void onStopTrackRecordingButtonClicked(){
        new MaterialDialog.Builder(this)
                .title(R.string.dashboard_dialog_stop_track)
                .content(R.string.dashboard_dialog_stop_track_content)
                .negativeText(R.string.cancel)
                .positiveText(R.string.ok)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        mTrackRecordingHandler.finishCurrentTrack();
                    }
                })
                .show();
    }

    private void updateLocationViews(boolean isFix) {
        if (isFix) {
            mGpsImage.setImageResource(R.drawable.ic_location_on_white_24dp);
        } else {
            mGpsImage.setImageResource(R.drawable.ic_location_off_white_24dp);
        }
    }

    private void updateDrivingViews(boolean isDriving) {
        if (isDriving) {
            mBluetoothImage.setImageResource(R.drawable.driving);
        } else {
            mBluetoothImage.setImageResource(R.drawable.not_driving);
        }
    }

    private void updateTheDisplayViewsGeneral(){
        viewTypeInGeneral = PreferencesHandler.getPreviousViewTypeGeneralForGPSRecordingScreen(this);

        if(viewTypeInGeneral == 2){
            animateViewTransition(trackMapContainer,R.anim.translate_slide_in_left_card,false);
            animateViewTransition(trackSingleMeterContainer,R.anim.translate_slide_out_right_card,true);
        }else{
            animateViewTransition(trackMapContainer,R.anim.translate_slide_out_left_card,true);
            animateViewTransition(trackSingleMeterContainer,R.anim.translate_slide_in_right_card,false);
        }

    }

    private void initAnimations(){
        animateViewTransition(trackDetailsContainer,R.anim.translate_slide_in_bottom_fragment,false);
        viewTypeInGeneral = PreferencesHandler.getPreviousViewTypeGeneralForGPSRecordingScreen(this);

        if(viewTypeInGeneral == 2){
            animateViewTransition(trackMapContainer,R.anim.translate_slide_in_top_fragment,false);
        }else{
            animateViewTransition(trackSingleMeterContainer,R.anim.translate_slide_in_top_fragment,false);
        }

    }

    /**
     * Applies an animation on the given view.
     *
     * @param view         the view to apply the animation on.
     * @param animResource the animation resource.
     * @param hide         should the view be hid?
     */
    private void animateViewTransition(final View view, int animResource, boolean hide) {
        Animation animation = AnimationUtils.loadAnimation(this, animResource);
        if (hide) {
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    // nothing to do..
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                    // nothing to do..
                }
            });
            view.startAnimation(animation);
        } else {
            view.setVisibility(View.VISIBLE);
            view.startAnimation(animation);
        }
    }
}
