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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.squareup.otto.Subscribe;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.events.AvrgSpeedUpdateEvent;
import org.envirocar.app.events.DistanceValueUpdateEvent;
import org.envirocar.app.events.DrivingDetectedEvent;
import org.envirocar.app.events.StartingTimeEvent;
import org.envirocar.app.handler.ApplicationSettings;
import org.envirocar.app.handler.TrackRecordingHandler;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.injection.modules.RecordingScreenModule;
import org.envirocar.app.recording.RecordingService;
import org.envirocar.app.recording.RecordingState;
import org.envirocar.app.recording.RecordingType;
import org.envirocar.app.recording.events.RecordingStateEvent;
import org.envirocar.app.views.BaseMainActivity;
import org.envirocar.app.views.login.SigninActivity;
import org.envirocar.core.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.core.events.gps.GpsSatelliteFixEvent;
import org.envirocar.core.logging.Logger;

import java.text.DecimalFormat;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * @author dewall
 */
public class RecordingScreenActivity extends BaseInjectorActivity {
    private static final Logger LOG = Logger.getLogger(RecordingScreenActivity.class);

    private static final String PREF_SELECTED_VIEW = "pref_selected_view";
    private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("###.#");

    /**
     * Starts the activity.
     *
     * @param context
     */
    public static void navigate(Context context) {
        context.startActivity(new Intent(context, RecordingScreenActivity.class));
    }

    // Injected dependencies
    @Inject
    protected TrackMapFragment trackMapFragment;
    @Inject
    protected TempomatFragment tempomatFragment;
    @Inject
    protected TrackRecordingHandler trackRecordingHandler;

    // Injected views
    @BindView(R.id.activity_recscreen_trackdetails_gps)
    protected ImageView gpsImage;
    @BindView(R.id.activity_recscreen_trackdetails_bluetooth)
    protected ImageView bluetoothImage;
    @BindView(R.id.activity_recscreen_trackdetails_bluetooth_text)
    protected TextView bluetoothText;
    @BindView(R.id.activity_recscreen_trackdetails_timer)
    protected Chronometer timerText;
    @BindView(R.id.activity_recscreen_trackdetails_distance)
    protected TextView distanceText;
    @BindView(R.id.activity_recscreen_trackdetails_speed)
    protected TextView speedText;
    @BindView(R.id.activity_recscreen_trackdetails_container)
    protected LinearLayout trackDetailsContainer;
    @BindView(R.id.activity_recscreen_trackmap_container)
    protected LinearLayout mapContainer;
    @BindView(R.id.activity_recscreen_tempomat_container)
    protected LinearLayout tempomatContainer;
    @BindView(R.id.activity_recscreen_stopbutton)
    protected LinearLayout stopTrackRecordingButton;

    // state variables
    private RecordingType recordingType;

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent
                .plus(new RecordingScreenModule())
                .inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LOG.info("Creating RecordingScreenActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording_screen);

        //if the track recording service is stopped then finish this activity and goback to bottombar main activity
        if (RecordingService.RECORDING_STATE == RecordingState.RECORDING_STOPPED) {
            startActivity(new Intent(RecordingScreenActivity.this, BaseMainActivity.class));
            finish();
        }

        // Inject all dashboard-related views.
        ButterKnife.bind(this);

        this.recordingType = ApplicationSettings.getSelectedRecordingTypeObservable(this).blockingFirst();
        if (recordingType.equals(RecordingType.ACTIVITY_RECOGNITION_BASED)) {
            this.bluetoothImage.setImageResource(R.drawable.recscreen_drivingstate_drawable);
            this.bluetoothText.setText(R.string.driving);
        }

        // init state
        this.bluetoothImage.setSelected(false);
        this.gpsImage.setEnabled(false);

        // set keep screen on setting
        boolean keepScreenOn = ApplicationSettings.getDisplayStaysActiveObservable(this).blockingFirst();
        this.trackDetailsContainer.setKeepScreenOn(keepScreenOn);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // initialize fragment transactions
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.activity_recscreen_trackmap_container, this.trackMapFragment);
        fragmentTransaction.add(R.id.activity_recscreen_tempomat_container, this.tempomatFragment);
        fragmentTransaction.commit();

        // show initial animation
        initAnimations();
    }

    @Override
    protected void onResume() {
        LOG.info("Resuming RecordingScreenActivity");
        super.onResume();

        //if the track recording service is stopped then finish this activity and goback to bottombar main activity
        if (RecordingService.RECORDING_STATE == RecordingState.RECORDING_STOPPED) {
            startActivity(new Intent(RecordingScreenActivity.this, BaseMainActivity.class));
            finish();
        }
    }

    @Override
    protected void onPause() {
        LOG.info("Pausing RecordingSCreenActivity");
        super.onPause();

        //
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.remove(this.trackMapFragment);
        fragmentTransaction.remove(this.tempomatFragment);
        fragmentTransaction.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @OnClick(R.id.activity_recscreen_switchbutton)
    protected void onSwitchViewsButtonClicked() {
        LOG.info("Switch views button clicked");
        Observable.just(true)
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnNext(ignore -> switchViews())
                .doOnError(LOG::error)
                .subscribe();
    }

    @OnClick(R.id.activity_recscreen_stopbutton)
    protected void onStopButtonClicked() {
        LOG.info("Stop button has been clicked. Showing dialog to request confirmation");

        View contentView = LayoutInflater.from(this)
                .inflate(R.layout.general_dialog_layout, null, false);

        // Set toolbar style
        Toolbar toolbar1 = contentView.findViewById(R.id.genral_dialog_toolbar);
        toolbar1.setTitle(R.string.dashboard_dialog_stop_track);
        toolbar1.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.ic_outline_stop_circle_24));
        toolbar1.setTitleTextColor(getResources().getColor(R.color.white_cario));

        // Set text view
        TextView textview = contentView.findViewById(R.id.general_dialog_text);
        textview.setText(getString(R.string.dashboard_dialog_stop_track_content));

        new MaterialAlertDialogBuilder(this,R.style.MaterialDialog)
                .setView(contentView)
                .setPositiveButton(R.string.ok,(dialog, which) -> {
                    trackRecordingHandler.finishCurrentTrack();
                    finish();
                })
                .setNegativeButton(R.string.cancel,null)
                .show();
    }

    @Subscribe
    public void onGpsSatelliteFixEvent(GpsSatelliteFixEvent event) {
        LOG.info("Received event: %s", event.toString());
        Observable.just(event)
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnNext(e -> this.gpsImage.setEnabled(e.mGpsSatelliteFix.isFix()))
                .doOnError(LOG::error)
                .subscribe();
    }

    @Subscribe
    public void onStartingTimeEvent(StartingTimeEvent event) {
        Observable.just(event)
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnNext(e -> {
                    timerText.setBase(e.mStartingTime);
                    if (e.mIsStarted)
                        timerText.start();
                    else
                        timerText.stop();
                })
                .doOnError(LOG::error)
                .subscribe();
    }

    @Subscribe
    public void onBluetoothStateEvent(BluetoothStateChangedEvent event) {
        if (!this.recordingType.equals(RecordingType.OBD_ADAPTER_BASED)) {
            return;
        }
        LOG.info("Received event: %s", event.toString());
        Observable.just(event)
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnNext(e -> this.bluetoothImage.setEnabled(e.isBluetoothEnabled))
                .doOnError(LOG::error)
                .subscribe();
    }

    @Subscribe
    public void onDrivingStateEvent(DrivingDetectedEvent event) {
        if (!this.recordingType.equals(RecordingType.ACTIVITY_RECOGNITION_BASED)) {
            return;
        }
        LOG.info("Received event: %s", event.toString());
        Observable.just(event)
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnNext(e -> this.bluetoothImage.setEnabled(e.mDrivingDetected))
                .doOnError(LOG::error)
                .subscribe();
    }

    @Subscribe
    public void onDistanceUpdateEvent(DistanceValueUpdateEvent event) {
        Observable.just(event)
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnNext(e -> distanceText.setText(String.format("%s km", DECIMAL_FORMATTER.format(e.mDistanceValue))))
                .doOnError(LOG::error)
                .subscribe();
    }

    @Subscribe
    public void onAvrgSpeedEvent(AvrgSpeedUpdateEvent event) {
        Observable.just(event)
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnNext(e -> speedText.setText(String.format("%s km/h", Integer.toString(e.mAvrgSpeed))))
                .doOnError(LOG::error)
                .subscribe();
    }

    @Subscribe
    public void onRecordingStateEvent(RecordingStateEvent event){
        LOG.info("Received event: %s", event.toString());
        if (event.recordingState == RecordingState.RECORDING_STOPPED){
            runOnUiThread(() -> this.finish());
        }
    }

    /**
     * @return
     */
    private int getPreviousSelectedView() {
        return getClassPreferences().getInt(PREF_SELECTED_VIEW, 1);
    }

    /**
     * @param selectedView
     */
    private void setPreviousSelectedView(int selectedView) {
        getClassPreferences().edit().putInt(PREF_SELECTED_VIEW, selectedView).commit();
    }

    /**
     * @return
     */
    private SharedPreferences getClassPreferences() {
        return getSharedPreferences(getClass().getSimpleName(), MODE_PRIVATE);
    }

    /**
     * Applies the initial animations when starting the activity.
     */
    private void initAnimations() {
        LinearLayout containerToShow = getPreviousSelectedView() == 2 ? mapContainer : tempomatContainer;
        animateViewTransition(trackDetailsContainer, R.anim.translate_slide_in_bottom_fragment, false);
        animateViewTransition(containerToShow, R.anim.translate_slide_in_top_fragment, false);
    }

    /**
     * Switches the views from map to tempomat and vice versa.
     */
    private void switchViews() {
        int selectedView = (getPreviousSelectedView() == 1) ? 2 : 1;
        LOG.info("Switching views to %s", "" + selectedView);
        this.setPreviousSelectedView(selectedView);

        if (selectedView == 2) {
            animateViewTransition(mapContainer, R.anim.translate_slide_in_left_card, false);
            animateViewTransition(tempomatContainer, R.anim.translate_slide_out_right_card, true);
        } else {
            animateViewTransition(mapContainer, R.anim.translate_slide_out_left_card, true);
            animateViewTransition(tempomatContainer, R.anim.translate_slide_in_right_card, false);
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
