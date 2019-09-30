/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.recordingscreen;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentTransaction;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.events.AvrgSpeedUpdateEvent;
import org.envirocar.app.events.DistanceValueUpdateEvent;
import org.envirocar.app.events.StartingTimeEvent;
import org.envirocar.app.handler.PreferenceConstants;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.handler.TrackRecordingHandler;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.views.BaseMainActivity;
import org.envirocar.app.views.MainActivityComponent;
import org.envirocar.app.views.MainActivityModule;
import org.envirocar.app.recording.RecordingService;
import org.envirocar.app.recording.RecordingState;
import org.envirocar.app.recording.events.RecordingStateEvent;
import org.envirocar.core.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.core.events.gps.GpsSatelliteFixEvent;
import org.envirocar.core.logging.Logger;

import java.text.DecimalFormat;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class OBDPlusGPSTrackRecordingScreen extends BaseInjectorActivity {
    private static final Logger LOGGER = Logger.getLogger(OBDPlusGPSTrackRecordingScreen.class);

    /**
     * Starts this activity.
     *
     * @param context current context.
     */
    public static void start(Context context) {
        context.startActivity(new Intent(context, OBDPlusGPSTrackRecordingScreen.class));
    }

    private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("###.#");

    @BindView(R.id.mGpsImage)
    protected ImageView mGpsImage;
    @BindView(R.id.mBluetoothImage)
    protected ImageView mBluetoothImage;
    @BindView(R.id.mTimerText)
    protected Chronometer mTimerText;
    @BindView(R.id.mDistanceText)
    protected TextView mDistanceText;
    @BindView(R.id.mSpeedText)
    protected TextView mSpeedText;
    @BindView(R.id.trackDetailsContainer)
    protected LinearLayout trackDetailsContainer;
    @BindView(R.id.trackMapContainer)
    protected LinearLayout trackMapContainer;
    @BindView(R.id.trackSingleMeterContainer)
    protected LinearLayout trackSingleMeterContainer;
    @BindView(R.id.trackMultipleMeterContainer)
    protected LinearLayout trackMultipleMeterContainer;
    @BindView(R.id.stopTrackRecordingButton)
    protected LinearLayout stopTrackRecordingButton;

    @Inject
    protected TrackRecordingHandler mTrackRecordingHandler;


    //viewTypeInGeneral = 1 means meter view
    //viewTypeInGeneral = 2 means map view
    private static int viewTypeInGeneral = 1;

    //viewTypeMeter = 1 means single meter view
    //viewTypeMeter = 2 means multiple meter view
    private static int viewTypeMeter = 1;


    private Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        MainActivityComponent mainActivityComponent = baseApplicationComponent.plus(new MainActivityModule(this));
        mainActivityComponent.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obdplus_gpstrack_recording_screen);

        //if the track recording service is stopped then finish this activity and goback to bottombar main activity
        if (RecordingService.RECORDING_STATE == RecordingState.RECORDING_STOPPED) {
            startActivity(new Intent(OBDPlusGPSTrackRecordingScreen.this, BaseMainActivity.class));
            finish();
        }

        // Inject all dashboard-related views.
        ButterKnife.bind(this);

        // set keep screen on setting
        this.trackDetailsContainer.setKeepScreenOn(PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean(PreferenceConstants.DISPLAY_STAYS_ACTIV, false));

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.trackMapContainer, new TrackMapFragment());
        fragmentTransaction.add(R.id.trackSingleMeterContainer, new TempomatFragment());
        fragmentTransaction.commit();

        initAnimations();
    }

    @Override
    protected void onResume() {
        LOGGER.info("onResume()");
        super.onResume();

        //if the track recording service is stopped then finish this activity and goback to bottombar main activity
        if (RecordingService.RECORDING_STATE == RecordingState.RECORDING_STOPPED) {
            startActivity(new Intent(OBDPlusGPSTrackRecordingScreen.this, BaseMainActivity.class));
            finish();
        }
    }

    @Subscribe
    public void onReceiveBluetoothStateChangedEvent(BluetoothStateChangedEvent event) {
        LOGGER.info(String.format("Received event: %s", event.toString()));
        mMainThreadWorker.schedule(() -> updateBluetoothViews(event.isBluetoothEnabled));
    }

    @Subscribe
    public void onRecordingStateEvent(RecordingStateEvent event) {
        LOGGER.info(String.format("Received event: %s", event.toString()));
        Observable.just(event.recordingState)
                .subscribeOn(AndroidSchedulers.mainThread())
                .filter(recordingState -> recordingState == RecordingState.RECORDING_STOPPED)
                .subscribe(recordingState -> {
                    mTimerText.setBase(SystemClock.elapsedRealtime());
                    mTimerText.stop();
                    mDistanceText.setText("0.0 km");
                    mSpeedText.setText("0 km/h");
                    finish();
                });
    }

    @Subscribe
    public void onReceiveAvrgSpeedUpdateEvent(AvrgSpeedUpdateEvent event) {
        mMainThreadWorker.schedule(() -> mSpeedText.setText(String.format("%s km/h",
                Integer.toString(event.mAvrgSpeed))));
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
            if (event.mIsStarted)
                mTimerText.start();
            else
                mTimerText.stop();
        });
    }

    @OnClick(R.id.switchViewsButton)
    protected void onSwitchViewsButtonClicked() {
        if (viewTypeInGeneral == 1) viewTypeInGeneral = 2;
        else viewTypeInGeneral = 1;
        PreferencesHandler.setPreviousViewTypeGeneralRecordingScreen(this, viewTypeInGeneral);
        updateTheDisplayViewsGeneral();
    }

    @OnClick(R.id.stopTrackRecordingButton)
    protected void onStopTrackRecordingButtonClicked() {
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

    /**
     * @param isConnected
     */
    private void updateBluetoothViews(boolean isConnected) {
        if (isConnected) {
            mBluetoothImage.setImageResource(R.drawable.ic_bluetooth_white_24dp);
        } else {
            mBluetoothImage.setImageResource(R.drawable.ic_bluetooth_disabled_black_24dp);
        }
    }

    private void updateLocationViews(boolean isFix) {
        if (isFix) {
            mGpsImage.setImageResource(R.drawable.ic_location_on_white_24dp);
        } else {
            mGpsImage.setImageResource(R.drawable.ic_location_off_white_24dp);
        }
    }

    private void updateTheDisplayViewsGeneral() {
        viewTypeInGeneral = PreferencesHandler.getPreviousViewTypeGeneralRecordingScreen(this);
        viewTypeMeter = PreferencesHandler.getPreviousViewTypeMeterRecordingScreen(this);


        if (viewTypeInGeneral == 2) {
            animateViewTransition(trackMapContainer, R.anim.translate_slide_in_right_card, false);
            if (trackMultipleMeterContainer.getVisibility() == View.VISIBLE)
                animateViewTransition(trackMultipleMeterContainer, R.anim.translate_slide_out_left_card, true);
            if (trackSingleMeterContainer.getVisibility() == View.VISIBLE)
                animateViewTransition(trackSingleMeterContainer, R.anim.translate_slide_out_left_card, true);
        } else {
            if (viewTypeMeter == 1) {
                animateViewTransition(trackMapContainer, R.anim.translate_slide_out_right_card, true);
                animateViewTransition(trackSingleMeterContainer, R.anim.translate_slide_in_left_card, false);
            } else {
                animateViewTransition(trackMapContainer, R.anim.translate_slide_out_right_card, true);
                animateViewTransition(trackMultipleMeterContainer, R.anim.translate_slide_in_left_card, false);
            }
        }

    }

    private void updateTheDisplayViewsMeter() {
        viewTypeMeter = PreferencesHandler.getPreviousViewTypeMeterRecordingScreen(this);


        if (viewTypeMeter == 1) {
            animateViewTransition(trackMultipleMeterContainer, R.anim.translate_slide_out_right_card, true);
            animateViewTransition(trackSingleMeterContainer, R.anim.translate_slide_in_left_card, false);
        } else {
            animateViewTransition(trackMultipleMeterContainer, R.anim.translate_slide_in_right_card, false);
            animateViewTransition(trackSingleMeterContainer, R.anim.translate_slide_out_left_card, true);
        }
    }

    private void initAnimations() {
        animateViewTransition(trackDetailsContainer, R.anim.translate_slide_in_bottom_fragment, false);
        viewTypeInGeneral = PreferencesHandler.getPreviousViewTypeGeneralRecordingScreen(this);
        viewTypeMeter = PreferencesHandler.getPreviousViewTypeMeterRecordingScreen(this);


        if (viewTypeInGeneral == 2) {
            animateViewTransition(trackMapContainer, R.anim.translate_slide_in_top_fragment, false);
        } else if (viewTypeMeter == 1) {
            animateViewTransition(trackSingleMeterContainer, R.anim.translate_slide_in_top_fragment, false);
        } else {
            animateViewTransition(trackMultipleMeterContainer, R.anim.translate_slide_in_top_fragment, false);
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
