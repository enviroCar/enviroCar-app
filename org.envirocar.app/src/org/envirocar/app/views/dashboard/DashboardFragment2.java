package org.envirocar.app.views.dashboard;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.ChangeBounds;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.snackbar.Snackbar;
import com.jakewharton.rxbinding3.appcompat.RxToolbar;
import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.handler.preferences.UserHandler;
import org.envirocar.app.handler.userstatistics.UserStatisticsUpdateEvent;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.recording.RecordingService;
import org.envirocar.app.recording.RecordingState;
import org.envirocar.app.recording.RecordingType;
import org.envirocar.app.recording.events.RecordingStateEvent;
import org.envirocar.app.views.carselection.CarSelectionActivity;
import org.envirocar.app.views.login.SigninActivity;
import org.envirocar.app.views.obdselection.OBDSelectionActivity;
import org.envirocar.app.views.recordingscreen.OBDPlusGPSTrackRecordingScreen;
import org.envirocar.core.entity.User;
import org.envirocar.core.events.NewCarTypeSelectedEvent;
import org.envirocar.core.events.NewUserSettingsEvent;
import org.envirocar.core.events.bluetooth.BluetoothDeviceSelectedEvent;
import org.envirocar.core.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.core.events.gps.GpsStateChangedEvent;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.PermissionUtils;
import org.envirocar.obd.events.TrackRecordingServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;

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
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * @author dewall
 */
public class DashboardFragment2 extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(DashboardFragment2.class);

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
    @BindView(R.id.fragment_dashboard_indicator_bluetooth)
    protected ImageView bluetoothIndicator;
    @BindView(R.id.fragment_dashboard_indicator_obd)
    protected ImageView obdIndicator;
    @BindView(R.id.fragment_dashboard_indicator_gps)
    protected ImageView gpsIndicator;
    @BindView(R.id.fragment_dashboard_indicator_car)
    protected ImageView carIndicator;

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

    // injected variables
    @Inject
    protected UserHandler userHandler;
    @Inject
    protected BluetoothHandler bluetoothHandler;

    private CompositeDisposable disposables;
    private boolean statisticsKnown = false;

    // some private variables
    private MaterialDialog connectingDialog;

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
        View contentView = inflater.inflate(R.layout.fragment_dashboard_view_new_2, container, false);

        // Bind views
        ButterKnife.bind(this, contentView);

        // inflate menus and init toolbar clicks
        toolbar.inflateMenu(R.menu.menu_dashboard_logged_out);
        toolbar.getOverflowIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        RxToolbar.itemClicks(this.toolbar).subscribe(this::onToolbarItemClicked);

        //
        PermissionUtils.requestLocationPermissionIfRequired(getActivity())
                .doOnComplete(() -> LOG.info("Accepted"))
                .doOnError(LOG::error)
                .subscribe();

        //
        this.updateUserLogin(userHandler.getUser());

        // set recording state
        PreferencesHandler.getSelectedRecordingTypeObservable(getContext())
                .doOnNext(this::setRecordingMode)
                .doOnError(LOG::error)
                .blockingFirst();

        return contentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        this.updateStatisticsVisibility(this.statisticsKnown);

        if (RecordingService.RECORDING_STATE == RecordingState.RECORDING_RUNNING) {
            OBDPlusGPSTrackRecordingScreen.start(getContext());
        }
    }

    private void onToolbarItemClicked(MenuItem menuItem) {
        LOG.info(String.format("Toolbar - Clicked on %s", menuItem.getTitle()));
        if (menuItem.getItemId() == R.id.dashboard_action_login) {
            // starting the login activity
            Intent intent = new Intent(getActivity(), SigninActivity.class);
            getActivity().startActivity(intent);
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
        PreferencesHandler.setSelectedRecordingType(getContext(), selectedRT);
    }

    private void setRecordingMode(RecordingType selectedRT){
        // check whether OBD is visible or not.
        int visibility = selectedRT == RecordingType.OBD_ADAPTER_BASED ? View.VISIBLE : View.GONE;

        if (visibility == View.GONE){
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
        this.bluetoothIndicator.setVisibility(visibility);
        this.obdIndicator.setVisibility(visibility);
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
        switch (this.modeSegmentedGroup.getCheckedRadioButtonId()) {
            case R.id.fragment_dashboard_obd_mode_button:
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

                ContextCompat.startForegroundService(getActivity(), obdRecordingIntent);
                break;
            case R.id.fragment_dashboard_gps_mode_button:
                Intent gpsOnlyIntent = new Intent(getActivity(), RecordingService.class);
                ContextCompat.startForegroundService(getActivity(), gpsOnlyIntent);
                break;
            default:
                break;
        }
    }

    @OnClick(R.id.fragment_dashboard_indicator_car)
    protected void onCarIndicatorClicked() {
        LOG.info("Car Indicator clicked");
        // TODO
    }

    @OnClick(R.id.fragment_dashboard_indicator_obd)
    protected void onObdIndicatorClicked() {
        LOG.info("OBD indicator clicked");
        // TODO
    }

    @OnClick(R.id.fragment_dashboard_indicator_bluetooth)
    protected void onBluetoothIndicatorClicked() {
        LOG.info("Bluetooth indicator clicked");
        // TODO
    }

    @OnClick(R.id.fragment_dashboard_indicator_gps)
    protected void onGPSIndicatorClicked() {
        LOG.info("GPS indicator clicked");
        // TODO
    }

    @Subscribe
    public void onReceiveRecordingStateChangedEvent(TrackRecordingServiceStateChangedEvent event) {
        LOG.info("Recieved Recording State Changed event");
        Observable.just(event.mState)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(state -> {
                    if (state == BluetoothServiceState.SERVICE_STARTED) {
                        OBDPlusGPSTrackRecordingScreen.start(getContext());
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
                this.carSelectionTextSecondary.setText(String.format("%s, %s ccm, %s",
                        "" + event.mCar.getConstructionYear(),
                        "" + event.mCar.getEngineDisplacement(),
                        "" + event.mCar.getFuelType().toString()));

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
            userDistanceTextView.setText(String.format("%s km", (int) event.totalDistance));
            userDurationTextView.setText(formatTimeForDashboard(event.totalDuration));
        });
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
            set.constrainPercentHeight(bannerLayout.getId(), 0.26f);
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
            set.constrainPercentHeight(bannerLayout.getId(), 0.15f);
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
        return String.format("%03d:%02d h",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)));
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
        this.startTrackButton.setEnabled(setEnabled);
    }

    private void updateByRecordingState(RecordingState state) {
        switch (state) {
            case RECORDING_INIT:
                break;
            case RECORDING_RUNNING:
                if (this.connectingDialog != null) {
                    this.connectingDialog.dismiss();
                    this.connectingDialog = null;
                }
                getActivity().startActivity(new Intent(getActivity(), OBDPlusGPSTrackRecordingScreen.class));
                break;
            case RECORDING_STOPPED:
                if (this.connectingDialog != null) {
                    this.connectingDialog.dismiss();
                    this.connectingDialog = null;
                }
                break;
        }
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
}
