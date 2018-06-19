package org.envirocar.app.view.recordingscreen;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.otto.Subscribe;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.MainActivityComponent;
import org.envirocar.app.MainActivityModule;
import org.envirocar.app.R;
import org.envirocar.app.events.AvrgSpeedUpdateEvent;
import org.envirocar.app.events.DistanceValueUpdateEvent;
import org.envirocar.app.events.StartingTimeEvent;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.handler.TrackRecordingHandler;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.view.dashboard.DashboardTempomatFragment;
import org.envirocar.app.view.dashboard.DashboardTrackMapFragment;
import org.envirocar.core.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.core.events.gps.GpsSatelliteFixEvent;
import org.envirocar.core.logging.Logger;
import org.envirocar.obd.events.TrackRecordingServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;

import java.text.DecimalFormat;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

public class OBDPlusGPSTrackRecordingScreen extends BaseInjectorActivity {
    private static final Logger LOGGER = Logger.getLogger(OBDPlusGPSTrackRecordingScreen.class);

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
    @BindView(R.id.switchMetersButton)
    protected LinearLayout switchMetersButton;
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
        MainActivityComponent mainActivityComponent =  baseApplicationComponent.plus(new MainActivityModule(this));
        mainActivityComponent.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obdplus_gpstrack_recording_screen);


        // Inject all dashboard-related views.
        ButterKnife.bind(this);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.trackMapContainer, new DashboardTrackMapFragment());
        fragmentTransaction.add(R.id.trackSingleMeterContainer, new DashboardTempomatFragment());
        fragmentTransaction.add(R.id.trackMultipleMeterContainer, new MultipleMetersViewFragment());
        fragmentTransaction.commit();

        initAnimations();
    }

    @Subscribe
    public void onReceiveBluetoothStateChangedEvent(BluetoothStateChangedEvent event) {
        LOGGER.info(String.format("Received event: %s", event.toString()));
        mMainThreadWorker.schedule(() -> updateBluetoothViews(event.isBluetoothEnabled));
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

    @OnClick(R.id.switchMetersButton)
    protected void onSwitchMetersButtonClicked(){
        if(viewTypeMeter == 1) viewTypeMeter = 2;
        else viewTypeMeter = 1;
        PreferencesHandler.setPreviousViewTypeMeterRecordingScreen(this,viewTypeMeter);
        updateTheDisplayViewsMeter();
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

    private void updateTheDisplayViewsGeneral(){
        viewTypeInGeneral = PreferencesHandler.getPreviousViewTypeGeneralRecordingScreen(this);
        viewTypeMeter = PreferencesHandler.getPreviousViewTypeMeterRecordingScreen(this);

        if(viewTypeInGeneral == 2) switchMetersButton.setVisibility(View.GONE);
        else switchMetersButton.setVisibility(View.VISIBLE);

        if(viewTypeInGeneral == 2){
            animateViewTransition(trackMapContainer,R.anim.translate_slide_in_right_card,false);
            if(trackMultipleMeterContainer.getVisibility() == View.VISIBLE)
            animateViewTransition(trackMultipleMeterContainer,R.anim.translate_slide_out_left_card,true);
            if(trackSingleMeterContainer.getVisibility() == View.VISIBLE)
            animateViewTransition(trackSingleMeterContainer,R.anim.translate_slide_out_left_card,true);
        }else{
            if(viewTypeMeter == 1){
                animateViewTransition(trackMapContainer,R.anim.translate_slide_out_right_card,true);
                animateViewTransition(trackSingleMeterContainer,R.anim.translate_slide_in_left_card,false);
            }else{
                animateViewTransition(trackMapContainer,R.anim.translate_slide_out_right_card,true);
                animateViewTransition(trackMultipleMeterContainer,R.anim.translate_slide_in_left_card,false);
            }
        }

    }

    private void updateTheDisplayViewsMeter(){
        viewTypeMeter = PreferencesHandler.getPreviousViewTypeMeterRecordingScreen(this);

        if(viewTypeInGeneral == 2) switchMetersButton.setVisibility(View.GONE);
        else switchMetersButton.setVisibility(View.VISIBLE);

        if(viewTypeMeter == 1){
            animateViewTransition(trackMultipleMeterContainer,R.anim.translate_slide_out_right_card,true);
            animateViewTransition(trackSingleMeterContainer,R.anim.translate_slide_in_left_card,false);
        }else{
            animateViewTransition(trackMultipleMeterContainer,R.anim.translate_slide_in_right_card,false);
            animateViewTransition(trackSingleMeterContainer,R.anim.translate_slide_out_left_card,true);
        }
    }

    private void initAnimations(){
        animateViewTransition(trackDetailsContainer,R.anim.translate_slide_in_bottom_fragment,false);
        viewTypeInGeneral = PreferencesHandler.getPreviousViewTypeGeneralRecordingScreen(this);
        viewTypeMeter = PreferencesHandler.getPreviousViewTypeMeterRecordingScreen(this);

        if(viewTypeInGeneral == 2) switchMetersButton.setVisibility(View.GONE);
        else switchMetersButton.setVisibility(View.VISIBLE);

        if(viewTypeInGeneral == 2){
            animateViewTransition(trackMapContainer,R.anim.translate_slide_in_top_fragment,false);
        }else if(viewTypeMeter == 1){
            animateViewTransition(trackSingleMeterContainer,R.anim.translate_slide_in_top_fragment,false);
        }else{
            animateViewTransition(trackMultipleMeterContainer,R.anim.translate_slide_in_top_fragment,false);
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
