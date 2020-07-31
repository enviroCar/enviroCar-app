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
package org.envirocar.app.views.dashboard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.provider.Settings;
import android.transition.AutoTransition;
import android.transition.ChangeBounds;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.dialog.MaterialDialogs;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.jakewharton.rxbinding3.appcompat.RxToolbar;
import com.squareup.otto.Subscribe;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.handler.ApplicationSettings;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.handler.preferences.UserPreferenceHandler;
import org.envirocar.app.handler.userstatistics.UserStatisticsUpdateEvent;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.recording.RecordingService;
import org.envirocar.app.recording.RecordingState;
import org.envirocar.app.recording.RecordingType;
import org.envirocar.app.recording.events.EngineNotRunningEvent;
import org.envirocar.app.recording.events.RecordingStateEvent;
import org.envirocar.app.views.carselection.CarSelectionActivity;
import org.envirocar.app.views.login.SigninActivity;
import org.envirocar.app.views.obdselection.OBDSelectionActivity;
import org.envirocar.app.views.others.OthersFragment;
import org.envirocar.app.views.recordingscreen.RecordingScreenActivity;
import org.envirocar.app.views.tracklist.TrackListPagerFragment;
import org.envirocar.app.views.utils.DialogUtils;
import org.envirocar.app.views.utils.SizeSyncTextView;
import org.envirocar.core.entity.User;
import org.envirocar.core.events.NewCarTypeSelectedEvent;
import org.envirocar.core.events.NewUserSettingsEvent;
import org.envirocar.core.events.bluetooth.BluetoothDeviceSelectedEvent;
import org.envirocar.core.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.core.events.gps.GpsStateChangedEvent;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.PermissionUtils;
import org.envirocar.core.utils.ServiceUtils;
import org.envirocar.obd.events.TrackRecordingServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import info.hoang8f.android.segmented.SegmentedGroup;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.schedulers.Schedulers;
import smartdevelop.ir.eram.showcaseviewlib.GuideView;

/**
 * @author dewall
 */
public class DashboardFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(DashboardFragment.class);

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1203;

    // View Injections
    @BindView(R.id.fragment_dashboard_toolbar)
    protected Toolbar toolbar;
    @BindView(R.id.fragment_dashboard_username)
    protected TextView textView;
    @BindView(R.id.fragment_dashboard_logged_in_layout)
    protected View loggedInLayout;

    @BindView(R.id.fragment_dashboard_user_tracks_layout)
    protected View userTracksLayout;
    @BindView(R.id.fragment_dashboard_user_tracks_textview)
    protected TextView userTracksTextView;
    @BindView(R.id.fragment_dashboard_user_distance_layout)
    protected View userDistanceLayout;
    @BindView(R.id.fragment_dashboard_user_distance_textview)
    protected TextView userDistanceTextView;
    @BindView(R.id.fragment_dashboard_user_duration_layout)
    protected View userDurationLayout;
    @BindView(R.id.fragment_dashboard_user_duration_textview)
    protected TextView userDurationTextView;
    @BindView(R.id.fragment_dashboard_user_statistics_progress)
    protected ProgressBar userStatProgressBar;

    @BindView(R.id.fragment_dashboard_indicator_view)
    protected ViewGroup indicatorView;
    @BindView(R.id.fragment_dashboard_indicator_bluetooth_layout)
    protected View bluetoothIndicatorLayout;
    @BindView(R.id.fragment_dashboard_indicator_bluetooth)
    protected ImageView bluetoothIndicator;
    @BindView(R.id.fragment_dashboard_indicator_obd_layout)
    protected View obdIndicatorLayout;
    @BindView(R.id.fragment_dashboard_indicator_obd)
    protected ImageView obdIndicator;
    @BindView(R.id.fragment_dashboard_indicator_gps)
    protected ImageView gpsIndicator;
    @BindView(R.id.fragment_dashboard_indicator_car)
    protected ImageView carIndicator;

    @BindView(R.id.fragment_dashboard_indicator_bluetooth_text)
    protected SizeSyncTextView bluetoothIndicatorText;
    @BindView(R.id.fragment_dashboard_indicator_obd_text)
    protected SizeSyncTextView obdIndicatorText;
    @BindView(R.id.fragment_dashboard_indicator_gps_text)
    protected SizeSyncTextView gpsIndicatorText;
    @BindView(R.id.fragment_dashboard_indicator_car_text)
    protected SizeSyncTextView carIndicatorText;

    @BindView(R.id.fragment_dashboard_mode_selector)
    protected SegmentedGroup modeSegmentedGroup;
    @BindView(R.id.fragment_dashboard_obd_mode_button)
    protected RadioButton obdModeRadioButton;
    @BindView(R.id.fragment_dashboard_gps_mode_button)
    protected RadioButton gpsModeRadioButton;

    @BindView(R.id.fragment_dashboard_obdselection_layout)
    protected ViewGroup bluetoothSelectionView;
    @BindView(R.id.fragment_dashboard_obdselection_text_primary)
    protected TextView bluetoothSelectionTextPrimary;
    @BindView(R.id.fragment_dashboard_obdselection_text_secondary)
    protected TextView bluetoothSelectionTextSecondary;
    @BindView(R.id.fragment_dashboard_carselection_text_primary)
    protected TextView carSelectionTextPrimary;
    @BindView(R.id.fragment_dashboard_carselection_text_secondary)
    protected TextView carSelectionTextSecondary;

    @BindView(R.id.fragment_dashboard_banner)
    protected FrameLayout bannerLayout;
    @BindView(R.id.fragment_dashboard_main_layout)
    protected ConstraintLayout mainLayout;

    @BindView(R.id.fragment_dashboard_start_track_button)
    protected View startTrackButton;
    @BindView(R.id.fragment_dashboard_start_track_button_text)
    protected TextView startTrackButtonText;

    // injected variables
    @Inject
    protected UserPreferenceHandler userHandler;
    @Inject
    protected BluetoothHandler bluetoothHandler;

    private CompositeDisposable disposables;
    private boolean statisticsKnown = false;

    // some private variables
    private MaterialDialog connectingDialog;
    private List<SizeSyncTextView> indicatorSyncGroup;

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // for the login/register button
        setHasOptionsMenu(true);

        this.disposables = new CompositeDisposable();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate view first
        View contentView = inflater.inflate(R.layout.fragment_dashboard_view_new, container, false);

        // Bind views
        ButterKnife.bind(this, contentView);

        // inflate menus and init toolbar clicks
        toolbar.inflateMenu(R.menu.menu_dashboard_logged_out);
        toolbar.getOverflowIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        RxToolbar.itemClicks(this.toolbar).subscribe(this::onToolbarItemClicked);

        //
        this.updateUserLogin(userHandler.getUser());

        // init the text size synchronization
        initTextSynchronization();

        // set recording state
        ApplicationSettings.getSelectedRecordingTypeObservable(getContext())
                .doOnNext(this::setRecordingMode)
                .doOnError(LOG::error)
                .blockingFirst();

        spotlightShowCase(contentView, "OBD HELP", "Help to connect obd with app", 2, R.id.fragment_dashboard_obd_help);

        return contentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        this.updateStatisticsVisibility(this.statisticsKnown);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LOG.info("Location permission has been granted");
                    Snackbar.make(getView(), "Location Permission granted.",
                            BaseTransientBottomBar.LENGTH_SHORT).show();
                    onStartTrackButtonClicked();
                } else {
                    LOG.info("Location permission has been denied");
                    Snackbar.make(getView(), "Location Permission denied.",
                            BaseTransientBottomBar.LENGTH_LONG).show();
                }
            }
        }
    }

    private void onToolbarItemClicked(MenuItem menuItem) {
        LOG.info(String.format("Toolbar - Clicked on %s", menuItem.getTitle()));
        if (menuItem.getItemId() == R.id.dashboard_action_login) {
            // starting the login activity
            SigninActivity.startActivity(getContext());
        } else if (menuItem.getItemId() == R.id.dashboard_action_logout) {
            // show a logout dialog
            new MaterialDialog.Builder(getActivity())
                    .title(getString(R.string.menu_logout_envirocar_title))
                    .positiveText(getString(R.string.menu_logout_envirocar_positive))
                    .negativeText(getString(R.string.menu_logout_envirocar_negative))
                    .content(getString(R.string.menu_logout_envirocar_content))
                    .onPositive((dialog, which) -> userHandler.logOut().subscribe(onLogoutSubscriber()))
                    .show();
        }
    }

    private DisposableCompletableObserver onLogoutSubscriber() {
        return new DisposableCompletableObserver() {
            private MaterialDialog dialog = null;
            private User userTemp = null;

            @Override
            public void onStart() {
                this.userTemp = userHandler.getUser();
                // show progress dialog for the deletion
                this.dialog = new MaterialDialog.Builder(getContext())
                        .title(R.string.activity_login_logout_progress_dialog_title)
                        .content(R.string.activity_login_logout_progress_dialog_content)
                        .progress(true, 0)
                        .cancelable(false)
                        .show();
            }

            @Override
            public void onComplete() {
                // Show a snackbar that indicates the finished logout
                Snackbar.make(getActivity().findViewById(R.id.navigation),
                        String.format(getString(R.string.goodbye_message), userTemp.getUsername()),
                        Snackbar.LENGTH_LONG).show();
                dialog.dismiss();
            }

            @Override
            public void onError(Throwable e) {
                LOG.error(e.getMessage(), e);
                dialog.dismiss();
            }

        };
    }

    @OnCheckedChanged({R.id.fragment_dashboard_obd_mode_button, R.id.fragment_dashboard_gps_mode_button})
    public void onModeChangedClicked(CompoundButton button, boolean checked) {
        if (!checked)
            return;
        RecordingType selectedRT = button.getId() == R.id.fragment_dashboard_obd_mode_button ?
                RecordingType.OBD_ADAPTER_BASED : RecordingType.ACTIVITY_RECOGNITION_BASED;

        LOG.info("Mode selected " + button.getText());

        // adjust the ui
        this.setRecordingMode(selectedRT);

        // update the selected recording type
        ApplicationSettings.setSelectedRecordingType(getContext(), selectedRT);
        // update button
        Boolean setEnabled = false;
        switch (button.getId()) {
            case R.id.fragment_dashboard_gps_mode_button:
                setEnabled = (!this.carIndicator.isEnabled()
                        && !this.gpsIndicator.isEnabled());
                break;
            case R.id.fragment_dashboard_obd_mode_button:
                setEnabled = (!this.bluetoothIndicator.isEnabled()
                        && !this.gpsIndicator.isEnabled()
                        && !this.obdIndicator.isEnabled()
                        && !this.carIndicator.isEnabled());
                break;
        }
        this.startTrackButtonText.setText(R.string.dashboard_start_track);
        this.startTrackButton.setEnabled(setEnabled);
    }

    private void setRecordingMode(RecordingType selectedRT) {
        if (!ApplicationSettings.isGPSBasedTrackingEnabled(getContext())) {
            modeSegmentedGroup.setVisibility(View.GONE);
        }

        // check whether OBD is visible or not.
        int visibility = selectedRT == RecordingType.OBD_ADAPTER_BASED ? View.VISIBLE : View.GONE;

        if (visibility == View.GONE) {
            gpsModeRadioButton.setChecked(true);
            obdModeRadioButton.setChecked(false);
        }

        // shared transition set
        TransitionSet transitionSet = new TransitionSet()
                .addTransition(new ChangeBounds())
                .addTransition(new AutoTransition())
                .addTransition(new Slide(Gravity.LEFT));

        // animate transition
        TransitionManager.beginDelayedTransition(this.modeSegmentedGroup);
        TransitionManager.beginDelayedTransition(this.bluetoothSelectionView, transitionSet);
        this.bluetoothSelectionView.setVisibility(visibility);

        // indicator transition
        TransitionManager.beginDelayedTransition(this.indicatorView, transitionSet);
        this.bluetoothIndicatorLayout.setVisibility(visibility);
        this.obdIndicatorLayout.setVisibility(visibility);
    }

    // OnClick Handler
    @OnClick(R.id.fragment_dashboard_carselection_layout)
    protected void onCarSelectionClicked() {
        LOG.info("Clicked on Carselection.");
        Intent intent = new Intent(getActivity(), CarSelectionActivity.class);
        getActivity().startActivity(intent);
    }

    @OnClick(R.id.fragment_dashboard_obdselection_layout)
    protected void onBluetoothSelectionClicked() {
        LOG.info("Clicked on Bluetoothselection.");
        Intent intent = new Intent(getActivity(), OBDSelectionActivity.class);
        getActivity().startActivity(intent);
    }

    @OnClick(R.id.fragment_dashboard_start_track_button)
    protected void onStartTrackButtonClicked() {
        LOG.info("Clicked on Start Track Button");
        if (RecordingService.RECORDING_STATE == RecordingState.RECORDING_RUNNING) {
            RecordingScreenActivity.navigate(getContext());
            return;
        } else if (!PermissionUtils.hasLocationPermission(getContext())) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            switch (this.modeSegmentedGroup.getCheckedRadioButtonId()) {
                case R.id.fragment_dashboard_obd_mode_button:
                    if (!this.gpsIndicator.isEnabled()
                            && !this.carIndicator.isEnabled()
                            && !this.bluetoothIndicator.isEnabled()
                            && !this.obdIndicator.isEnabled()) {
                        BluetoothDevice device = bluetoothHandler.getSelectedBluetoothDevice();

                        Intent obdRecordingIntent = new Intent(getActivity(), RecordingService.class);
                        this.connectingDialog = new MaterialDialog.Builder(getActivity())
                                .iconRes(R.drawable.ic_bluetooth_searching_black_24dp)
                                .title(R.string.dashboard_connecting)
                                .content(String.format(getString(R.string.dashboard_connecting_find_template), device.getName()))
                                .progress(true, 0)
                                .negativeText(R.string.cancel)
                                .cancelable(false)
                                .onNegative((dialog, which) -> getActivity().stopService(obdRecordingIntent))
                                .show();

                        ServiceUtils.startService(getActivity(), obdRecordingIntent);
                    }
                    break;
                case R.id.fragment_dashboard_gps_mode_button:
                    Intent gpsOnlyIntent = new Intent(getActivity(), RecordingService.class);
                    ServiceUtils.startService(getActivity(), gpsOnlyIntent);
                    break;
                default:
                    break;
            }
        }
    }

    @OnClick(R.id.fragment_dashboard_indicator_car)
    protected void onCarIndicatorClicked() {
        LOG.info("Car Indicator clicked");
        Intent intent = new Intent(getActivity(), CarSelectionActivity.class);
        getActivity().startActivity(intent);
    }

    @OnClick(R.id.fragment_dashboard_indicator_obd)
    protected void onObdIndicatorClicked() {
        LOG.info("OBD indicator clicked");
        Intent intent = new Intent(getActivity(), OBDSelectionActivity.class);
        getActivity().startActivity(intent);
    }

    @OnClick(R.id.fragment_dashboard_indicator_bluetooth)
    protected void onBluetoothIndicatorClicked() {
        LOG.info("Bluetooth indicator clicked");
        Intent intent = new Intent(getActivity(), OBDSelectionActivity.class);
        getActivity().startActivity(intent);
    }

    @OnClick(R.id.fragment_dashboard_indicator_gps)
    protected void onGPSIndicatorClicked() {
        LOG.info("GPS indicator clicked");
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        getActivity().startActivity(intent);
    }

    @Subscribe
    public void onReceiveRecordingStateChangedEvent(TrackRecordingServiceStateChangedEvent event) {
        LOG.info("Recieved Recording State Changed event");
        Observable.just(event.mState)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(state -> {
                    if (state == BluetoothServiceState.SERVICE_STARTED) {
                        RecordingScreenActivity.navigate(getContext());
                    }
                    return state;
                })
                .subscribe(this::updateStartTrackButton, LOG::error);
    }

    @Subscribe
    public void onRecordingStateEvent(RecordingStateEvent event) {
        LOG.info("Retrieve Recording State Event: " + event.toString());
        Observable.just(event.recordingState)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateByRecordingState, LOG::error);
    }

    @Subscribe
    public void onEngineNotRunningEvent(EngineNotRunningEvent event) {
        LOG.info("Retrieved Engine not running event");
        if (connectingDialog != null) {
            connectingDialog.dismiss();
            connectingDialog = null;
        }

        Observable.just(event)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(engineNotRunningEveunt -> new MaterialDialog.Builder(getContext())
                        .title(R.string.dashboard_engine_not_running_dialog_title)
                        .content(R.string.dashboard_engine_not_running_dialog_content)
                        .iconRes(R.drawable.ic_error_black_24dp)
                        .positiveText(R.string.ok)
                        .cancelable(true)
                        .show());
    }

    /**
     * Receiver method for bluetooth activation events.
     *
     * @param event
     */
    @Subscribe
    public void receiveBluetoothStateChanged(BluetoothStateChangedEvent event) {
        // post on decor view to ensure that it gets executed when view has been inflated.
        runAfterInflation(() -> {
            this.bluetoothIndicator.setEnabled(!event.isBluetoothEnabled);
            this.updateOBDState(event.selectedDevice);
            this.updateStartTrackButton();
        });
    }

    /**
     * Receiver method for new Car selected events.
     */
    @Subscribe
    public void onReceiveNewCarTypeSelectedEvent(final NewCarTypeSelectedEvent event) {
        LOG.info("Received NewCarTypeSelected event. Updating views.");
        // post on decor view to ensure that it gets executed when view has been inflated.
        runAfterInflation(() -> {
            if (event.mCar != null) {
                this.carSelectionTextPrimary.setText(String.format("%s %s",
                        event.mCar.getManufacturer(), event.mCar.getModel()));
                this.carSelectionTextSecondary.setText(String.format("%s, %s cm³, %s",
                        "" + event.mCar.getConstructionYear(),
                        "" + event.mCar.getEngineDisplacement(),
                        "" + getString(event.mCar.getFuelType().getStringResource())));

                // set indicator color accordingly
                this.carIndicator.setEnabled(false);
            } else {
                // set warning indicator color
                this.carIndicator.setEnabled(true);
            }
            this.updateStartTrackButton();
        });
    }

    /**
     * Receiver method for bluetooth device selected events.
     *
     * @param event
     */
    @Subscribe
    public void onOBDAdapterSelectedEvent(BluetoothDeviceSelectedEvent event) {
        // post on decor view to ensure that it gets executed when view has been inflated.
        runAfterInflation(() -> {
            updateOBDState(event.mDevice);
        });
    }

    /**
     * Receiver method for GPS activation events.
     *
     * @param event
     */
    @Subscribe
    public void onGpsStateChangedEvent(final GpsStateChangedEvent event) {
        // post on decor view to ensure that it gets executed when view has been inflated.
        runAfterInflation(() -> {
            this.gpsIndicator.setEnabled(!event.mIsGPSEnabled);
            this.updateStartTrackButton();
        });
    }

    @Subscribe
    public void onNewUserSettingsEvent(final NewUserSettingsEvent event) {
        runAfterInflation(() -> {
            this.statisticsKnown = false;
            this.updateUserLogin(event.mUser);
        });
    }

    @Subscribe
    public void onUserStatisticsUpdateEvent(final UserStatisticsUpdateEvent event) {
        runAfterInflation(() -> {
            this.statisticsKnown = true;
            updateStatisticsVisibility(true);
            userTracksTextView.setText(String.format("%s", event.numTracks));
            userDistanceTextView.setText(String.format("%s km", Math.round(event.totalDistance)));
            userDurationTextView.setText(formatTimeForDashboard(event.totalDuration));
        });
    }

    @OnClick(R.id.fragment_dashboard_obd_help)
    void onOBDHelpClicked() {

        DialogFragment dialog = new ObdHelpFragment();
        dialog.show(getChildFragmentManager(), "dialog");
    }

    private void updateUserLogin(User user) {
        if (user != null) {
            // show progress bar
            updateStatisticsVisibility(this.statisticsKnown);

            this.loggedInLayout.setVisibility(View.VISIBLE);
            this.toolbar.getMenu().clear();
            this.toolbar.inflateMenu(R.menu.menu_dashboard_logged_in);
            this.textView.setText(user.getUsername());


            ConstraintSet set = new ConstraintSet();
            set.constrainPercentHeight(bannerLayout.getId(), 0.25f);
            set.connect(bannerLayout.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
            set.connect(bannerLayout.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
            set.connect(bannerLayout.getId(), ConstraintSet.TOP, toolbar.getId(), ConstraintSet.BOTTOM, 0);
            set.applyTo(this.mainLayout);
        } else {
            // show progress bar
            updateStatisticsVisibility(this.statisticsKnown);

            this.loggedInLayout.setVisibility(View.GONE);
            this.toolbar.getMenu().clear();
            this.toolbar.inflateMenu(R.menu.menu_dashboard_logged_out);

            ConstraintSet set = new ConstraintSet();
            set.constrainPercentHeight(bannerLayout.getId(), 0.115f);
            set.connect(bannerLayout.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
            set.connect(bannerLayout.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
            set.connect(bannerLayout.getId(), ConstraintSet.TOP, toolbar.getId(), ConstraintSet.BOTTOM, 0);
            set.applyTo(this.mainLayout);
        }
    }

    private void updateStatisticsVisibility(boolean statisticsKnown) {
        // update progress bar visibility
        int progressBarVisibility = statisticsKnown ? View.GONE : View.VISIBLE;
        userStatProgressBar.setVisibility(progressBarVisibility);

        // update statistics visibility
        int statisticsVisibility = statisticsKnown ? View.VISIBLE : View.INVISIBLE;
        userTracksLayout.setVisibility(statisticsVisibility);
        userDistanceLayout.setVisibility(statisticsVisibility);
        userDurationLayout.setVisibility(statisticsVisibility);
    }

    private String formatTimeForDashboard(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        String formatString = hours > 99 ? "%03d:%02d h" : "%02d:%02d h";
        return String.format(formatString, hours, TimeUnit.MILLISECONDS.toMinutes(millis)
                - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)));
    }

    private void updateOBDState(BluetoothDevice device) {
        if (device != null) {
            bluetoothSelectionTextPrimary.setText(device.getName());
            bluetoothSelectionTextSecondary.setText(device.getAddress());

            // set indicator color
            this.obdIndicator.setEnabled(false);
        } else {
            this.obdIndicator.setEnabled(true);
        }
        this.updateStartTrackButton();
    }

    private void updateStartTrackButton() {
        boolean setEnabled = false;
        switch (RecordingService.RECORDING_STATE) {
            case RECORDING_RUNNING:
                this.startTrackButtonText.setText(R.string.dashboard_goto_track);
                this.startTrackButton.setEnabled(true);
                break;
            case RECORDING_INIT:
                this.startTrackButtonText.setText(R.string.dashboard_track_is_starting);
                this.startTrackButton.setEnabled(true);
                break;
            case RECORDING_STOPPED:
                switch (this.modeSegmentedGroup.getCheckedRadioButtonId()) {
                    case R.id.fragment_dashboard_gps_mode_button:
                        setEnabled = (!this.carIndicator.isEnabled()
                                && !this.gpsIndicator.isEnabled());
                        break;
                    case R.id.fragment_dashboard_obd_mode_button:
                        setEnabled = (!this.bluetoothIndicator.isEnabled()
                                && !this.gpsIndicator.isEnabled()
                                && !this.obdIndicator.isEnabled()
                                && !this.carIndicator.isEnabled());
                        break;
                }
                this.startTrackButtonText.setText(R.string.dashboard_start_track);
                this.startTrackButton.setEnabled(setEnabled);
                break;
        }
    }

    private void updateByRecordingState(RecordingState state) {
        switch (state) {
            case RECORDING_INIT:
                break;
            case RECORDING_RUNNING:
                switch (this.modeSegmentedGroup.getCheckedRadioButtonId()) {
                    case R.id.fragment_dashboard_gps_mode_button:
                        RecordingScreenActivity.navigate(getContext());
                        break;
                    case R.id.fragment_dashboard_obd_mode_button:
                        if (this.connectingDialog != null) {
                            this.connectingDialog.dismiss();
                            this.connectingDialog = null;
                            RecordingScreenActivity.navigate(getContext());
                        }
                        break;
                }
                break;
            case RECORDING_STOPPED:
                if (this.connectingDialog != null) {
                    this.connectingDialog.dismiss();
                    this.connectingDialog = null;
                }
                break;
        }
        updateStartTrackButton();
    }

    private void updateStartTrackButton(BluetoothServiceState state) {
        switch (state) {
            case SERVICE_STOPPED:
                this.startTrackButton.setEnabled(true);
                break;
            case SERVICE_STARTED:
                break;
            case SERVICE_STARTING:
                break;
            case SERVICE_STOPPING:
                break;
            default:
                break;
        }
    }

    private void initTextSynchronization() {
        // text size synchonization grp for indicators
        this.indicatorSyncGroup = new ArrayList<>();
        this.indicatorSyncGroup.add(bluetoothIndicatorText);
        this.indicatorSyncGroup.add(obdIndicatorText);
        this.indicatorSyncGroup.add(gpsIndicatorText);
        this.indicatorSyncGroup.add(carIndicatorText);

        SizeSyncTextView.OnTextSizeChangedListener listener =
                new SizeSyncTextView.OnTextSizeChangedListener() {
                    @SuppressLint("RestrictedApi")
                    @Override
                    public void onTextSizeChanged(SizeSyncTextView view, float size) {
                        for (SizeSyncTextView textView : indicatorSyncGroup) {
                            if (!textView.equals(view) && textView.getText() != view.getText()) {
                                textView.setAutoSizeTextTypeUniformWithPresetSizes(
                                        new int[]{(int) size}, TypedValue.COMPLEX_UNIT_PX);
                            }
                        }
                    }
                };

        for (SizeSyncTextView textView : indicatorSyncGroup) {
            textView.setOnTextSizeChangedListener(listener);
        }
    }

    private void spotlightShowCase(View contentView, String head, String content, int nextTarget, int id) {
        new GuideView.Builder(getContext())
                .setTitle(head)
                .setContentText(content)
                .setGravity(GuideView.Gravity.center)
                .setTargetView(contentView.findViewById(id))
                .setContentTextSize(12)
                .setTitleTextSize(16)
                .setDismissType(GuideView.DismissType.outside)
                .setGuideListener(view -> {
                    switch (nextTarget) {
                        case 2:
                            spotlightShowCase(contentView, "Track Details", "Displays user Local and Remote Track data ", 3, R.id.user_statistics);
                            break;

                        case 3:
                            spotlightShowCase(contentView,"Buetooth","Indicator based on device bluetooth status",4,R.id.fragment_dashboard_indicator_bluetooth_layout);
                            break;

                        case 4:
                            spotlightShowCase(contentView,"OBD","Indiactor shows the OBD connectivity status",5,R.id.fragment_dashboard_indicator_obd_layout);
                            break;

                        case 5:
                            spotlightShowCase(contentView,"GPS","Indicator show device GPS status",6,R.id.fragment_dashboard_indicator_gps);
                            break;

                        case  6:
                            spotlightShowCase(contentView,"Car","Indicator show if car is selected or not",7,R.id.fragment_dashboard_indicator_car);
                            break;

                        case 7:
                            spotlightShowCase(contentView,"OBD select","Pair OBD device with app",8,R.id.fragment_dashboard_obdselection_layout);
                            break;

                        case 8:
                            spotlightShowCase(contentView,"Car select","Select your car type",9,R.id.fragment_dashboard_carselection_layout);
                            break;

                        case 9:
                            spotlightShowCase(contentView,"Track record","Start recording track on all indicator set",10,R.id.fragment_dashboard_start_track_button);
                            break;

                    }
                })
                .build()
                .show();


    }
}
