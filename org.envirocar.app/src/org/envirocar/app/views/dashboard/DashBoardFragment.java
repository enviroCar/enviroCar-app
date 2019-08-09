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
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.core.app.ActivityCompat;
import androidx.transition.AutoTransition;
import androidx.transition.ChangeBounds;
import androidx.transition.Fade;
import androidx.transition.Slide;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.otto.Subscribe;
import com.transitionseverywhere.Recolor;

import org.envirocar.app.BuildConfig;
import org.envirocar.app.R;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.LocationHandler;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.handler.TrackDAOHandler;
import org.envirocar.app.handler.UserHandler;
import org.envirocar.app.handler.agreement.AgreementManager;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.main.MainActivityComponent;
import org.envirocar.app.main.MainActivityModule;
import org.envirocar.app.services.recording.GPSOnlyRecordingService;
import org.envirocar.app.services.recording.OBDRecordingService;
import org.envirocar.app.views.carselection.CarSelectionActivity;
import org.envirocar.app.views.login.SigninActivity;
import org.envirocar.app.views.login.SignupActivity;
import org.envirocar.app.views.obdselection.OBDSelectionActivity;
import org.envirocar.app.views.recordingscreen.GPSOnlyTrackRecordingScreen;
import org.envirocar.app.views.recordingscreen.OBDPlusGPSTrackRecordingScreen;
import org.envirocar.app.views.utils.DialogUtils;
import org.envirocar.core.dao.TrackDAO;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Track;
import org.envirocar.core.events.NewCarTypeSelectedEvent;
import org.envirocar.core.events.bluetooth.BluetoothDeviceSelectedEvent;
import org.envirocar.core.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.core.events.gps.GpsStateChangedEvent;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.injection.InjectActivityScope;
import org.envirocar.core.logging.Logger;
import org.envirocar.obd.events.TrackRecordingServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static android.view.View.GONE;

public class DashBoardFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(DashBoardFragment.class);

    protected static final DecimalFormat DECIMAL_FORMATTER_TWO = new DecimalFormat("#.#");

    @InjectActivityScope
    @Inject
    protected Context context;
    @Inject
    protected UserHandler mUserManager;
    @Inject
    protected DAOProvider mDAOProvider;
    @Inject
    protected AgreementManager mAgreementManager;
    @Inject
    protected TrackDAOHandler mTrackDAOHandler;
    @Inject
    protected CarPreferenceHandler mCarPrefHandler;
    @Inject
    protected BluetoothHandler mBluetoothHandler;
    @Inject
    protected LocationHandler mLocationHandler;
    @Inject
    protected CarPreferenceHandler mCarManager;

    @BindView(R.id.dashboardLayout)
    protected ConstraintLayout dashboardLayout;
    @BindView(R.id.userStatisticsGroup)
    protected Group userStatisticsGroup;
    @BindView(R.id.userLoginSignupButtonGroup)
    protected Group userLoginSignupButtonGroup;
    @BindView(R.id.userTotalDuration)
    protected TextView userTotalDurationTV;
    @BindView(R.id.userTotalDurationAdd)
    protected TextView userTotalDurationAddTV;
    @BindView(R.id.userTrackCount)
    protected TextView userTrackCountTV;
    @BindView(R.id.userTotalDistance)
    protected TextView userTotalDistanceTV;
    @BindView(R.id.date)
    protected TextView date;
    @BindView(R.id.signInInitiatorButton)
    protected Button signInButton;
    @BindView(R.id.registerInitiatorButton)
    protected Button registerButton;
    @BindView(R.id.dashBoardUserImage)
    protected ImageView dashBoardUserImageView;
    @BindView(R.id.dashBoardName)
    protected TextView dashBoardName;

    @BindView(R.id.errorImageBluetooth)
    protected ImageView errorImageBluetooth;
    @BindView(R.id.errorImageOBDAdapter)
    protected ImageView errorImageOBDAdapter;
    @BindView(R.id.errorImageGPS)
    protected ImageView errorImageGPS;
    @BindView(R.id.errorImageCar)
    protected ImageView errorImageCar;

    @BindView(R.id.okImageBluetooth)
    protected ImageView okImageBluetooth;
    @BindView(R.id.okImageOBDAdapter)
    protected ImageView okImageOBDAdapter;
    @BindView(R.id.okImageGPS)
    protected ImageView okImageGPS;
    @BindView(R.id.okImageCar)
    protected ImageView okImageCar;

    @BindView(R.id.buttonBanner)
    protected LinearLayout buttonBanner;
    @BindView(R.id.bannerBluetoothContainer)
    protected LinearLayout bannerBluetoothContainer;
    @BindView(R.id.bannerOBDAdapterContainer)
    protected LinearLayout bannerOBDAdapterContainer;
    @BindView(R.id.bannerGPSContainer)
    protected LinearLayout bannerGPSContainer;
    @BindView(R.id.bannerCarContainer)
    protected LinearLayout bannerCarContainer;

    @BindView(R.id.disableChangingParametersLayout)
    protected View disableChangingParametersLayout;

    @BindView(R.id.CarOptions)
    protected ConstraintLayout CarOptions;
    @BindView(R.id.CarOptionsImg)
    protected ImageView CarOptionsImg;
    @BindView(R.id.CarOptionsTxt1)
    protected TextView CarOptionsTxt1;
    @BindView(R.id.CarOptionsTxt2)
    protected TextView CarOptionsTxt2;

    @BindView(R.id.GPSOptions)
    protected ConstraintLayout GPSOptions;
    @BindView(R.id.GPSOptionsImg)
    protected ImageView GPSOptionsImg;
    @BindView(R.id.GPSOptionsTxt1)
    protected TextView GPSOptionsTxt1;
    @BindView(R.id.GPSOptionsTxt2)
    protected TextView GPSOptionsTxt2;

    @BindView(R.id.obdOptions)
    protected ConstraintLayout obdOptions;
    @BindView(R.id.obdOptionsImg)
    protected ImageView obdOptionsImg;
    @BindView(R.id.obdOptionsTxt1)
    protected TextView obdOptionsTxt1;
    @BindView(R.id.obdOptionsTxt2)
    protected TextView obdOptionsTxt2;

    @BindView(R.id.obdPlusGPSButton)
    protected Button obdPlusGPSButton;
    @BindView(R.id.GPSOnlyButton)
    protected Button GPSOnlyButton;
    @BindView(R.id.obdPlusGPSLineIndicator)
    protected TextView obdGPSIndicator;
    @BindView(R.id.GPSLineIndicator)
    protected TextView GPSIndicator;

    @BindView(R.id.optionsLayout)
    protected ConstraintLayout optionsLayout;
    @BindView(R.id.startButton)
    protected Button startButton;

    private MaterialDialog mConnectingDialog;

    protected CompositeSubscription subscriptions = new CompositeSubscription();
    private final Scheduler.Worker mBackgroundWorker = Schedulers
            .newThread().createWorker();
    private final Scheduler.Worker mMainThreadWorker = AndroidSchedulers
            .mainThread().createWorker();
    protected Double distance = 0.0;
    protected long timeInMillis = 0;
    protected Boolean localTCount = false, remoteTCount = false;
    //trackType = 1 means OBD + GPS
    //trackType = 2 means GPS Only
    private static int trackType = 1;
    private int REQUEST_LOCATION_PERMISSION_REQUEST_CODE = 108;
    private int REQUEST_STORAGE_PERMISSION_REQUEST_CODE = 109;


    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        MainActivityComponent mainActivityComponent = baseApplicationComponent.plus(new MainActivityModule(getActivity()));
        mainActivityComponent.inject(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!PreferencesHandler.getEnableGPSBasedTrackRecording(context)) {
            PreferencesHandler.setPreviouslySelectedRecordingType(context, 1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // First inflate the general dashboard view.
        View contentView = inflater.inflate(R.layout.fragment_dashboard_view_test, container, false);

        ButterKnife.bind(this, contentView);
        String t = PreferencesHandler.getTotalTime(getActivity());
///*        userTotalDurationAddTV.setText(t.charAt(t.length() - 1) + "");
//        userTotalDurationTV.setText(t.substring(0, t.length() - 1) + "");*/
        Integer totalTracks = PreferencesHandler.getUploadedTrackCount(getActivity()) + PreferencesHandler.getLocalTrackCount(getActivity());
        userTrackCountTV.setText(totalTracks + "");
        userTotalDistanceTV.setText(DECIMAL_FORMATTER_TWO.format(PreferencesHandler.getTotalDistanceTravelledOfUser(getActivity())));

        CarOptions.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CarSelectionActivity.class);
            getActivity().startActivity(intent);
        });
        obdOptions.setOnClickListener(v -> {
            if(errorImageBluetooth.getVisibility() == View.VISIBLE){
                Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(intent);
            } else {
                Intent intent = new Intent(getActivity(), OBDSelectionActivity.class);
                getActivity().startActivity(intent);
            }
        });

        GPSOptions.setOnClickListener(v->{
            Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        });

        setCarTypeText(mCarPrefHandler.getCar());
        setOBDTypeText(mBluetoothHandler.getSelectedBluetoothDevice());
        setGPSOptions(mLocationHandler.isGPSEnabled());

        obdPlusGPSButton.setOnClickListener(v->{
            if(trackType != 1) {
                obdPlusGPSButton.setEnabled(false);
                GPSOnlyButton.setEnabled(false);
                trackType = 1;
                checkTrackTypeAndSet();
            }
        });

        GPSOnlyButton.setOnClickListener(v->{
            if(trackType != 2){
                obdPlusGPSButton.setEnabled(false);
                GPSOnlyButton.setEnabled(false);
                trackType = 2;
                checkTrackTypeAndSet();
            }
        });

        checkTrackTypeAndSet();
        setDate();

        if (!PreferencesHandler.getEnableGPSBasedTrackRecording(context)) {
            trackType = 1;
            checkTrackTypeAndSet();

            obdPlusGPSButton.setVisibility(GONE);
            GPSOnlyButton.setVisibility(GONE);
        } else{
            obdPlusGPSButton.setVisibility(View.VISIBLE);
            GPSOnlyButton.setVisibility(View.VISIBLE);
        }

        if (!checkStoragePermissions()) {
            requestStoragePermissions();
        }
        return contentView;
    }

    protected void checkTrackTypeAndSet(){
        TransitionSet transition = new TransitionSet()
                .addTransition(new ChangeBounds())
                .addTransition(new Fade());
        transition.addTarget(R.id.obdPlusGPSLineIndicator);
        transition.addTarget(R.id.GPSLineIndicator);
        transition.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(@NonNull Transition transition) {

            }

            @Override
            public void onTransitionEnd(@NonNull Transition transition) {
                obdPlusGPSButton.setEnabled(true);
                GPSOnlyButton.setEnabled(true);
            }

            @Override
            public void onTransitionCancel(@NonNull Transition transition) {

            }

            @Override
            public void onTransitionPause(@NonNull Transition transition) {

            }

            @Override
            public void onTransitionResume(@NonNull Transition transition) {

            }
        });

        switch (trackType) {
            case 1:
                trackType = 1;
                DashBoardFragment.this.showOBDPlusGPSSettings();
                PreferencesHandler.setPreviouslySelectedRecordingType(context.getApplicationContext(), 1);
                DashBoardFragment.this.updateStartStopButtonOBDPlusGPS(OBDRecordingService.CURRENT_SERVICE_STATE);
                TransitionManager.beginDelayedTransition(dashboardLayout, transition);
                obdGPSIndicator.setVisibility(View.VISIBLE);
                GPSIndicator.setVisibility(GONE);
                break;
            case 2:
                trackType = 2;
                DashBoardFragment.this.showGPSOnlySettings();
                PreferencesHandler.setPreviouslySelectedRecordingType(context.getApplicationContext(), 2);
                DashBoardFragment.this.updateStartStopButtonGPSOnly(GPSOnlyRecordingService.CURRENT_SERVICE_STATE);
                TransitionManager.beginDelayedTransition(dashboardLayout, transition);
                obdGPSIndicator.setVisibility(GONE);
                GPSIndicator.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    protected void setDate() {
        Calendar c = Calendar.getInstance();
        Date date = c.getTime();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        this.date.setText(simpleDateFormat.format(date));
    }

    @OnClick(R.id.signInInitiatorButton)
    protected void onLoginInitiatorButtonClicked() {
        Intent intent = new Intent(getActivity(), SigninActivity.class);
        intent.putExtra("from", "login");
        startActivity(intent);
    }

    @OnClick(R.id.registerInitiatorButton)
    protected void onRegisterInitiatorButtonClicked() {
        Intent intent = new Intent(getActivity(), SignupActivity.class);
        intent.putExtra("from", "register");
        startActivity(intent);
    }

    @OnClick(R.id.bannerBluetoothContainer)
    protected void onbannerBluetoothContainerClicked() {
        if (errorImageBluetooth.getVisibility() == View.VISIBLE) {
            Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
        }
    }

    @OnClick(R.id.bannerOBDAdapterContainer)
    protected void onbannerOBDAdapterContainerClicked() {
        if (errorImageOBDAdapter.getVisibility() == View.VISIBLE) {
            Intent intent = new Intent(getActivity(), OBDSelectionActivity.class);
            getActivity().startActivity(intent);
        }
    }

    @OnClick(R.id.bannerGPSContainer)
    protected void onbannerGPSContainerClicked() {
        if (errorImageGPS.getVisibility() == View.VISIBLE) {
            Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }

    @OnClick(R.id.bannerCarContainer)
    protected void onbannerCarContainerClicked() {
        if (errorImageCar.getVisibility() == View.VISIBLE) {
            Intent intent = new Intent(getActivity(), CarSelectionActivity.class);
            getActivity().startActivity(intent);
        }
    }

    @OnClick(R.id.disableChangingParametersLayout)
    protected void ondisableChangingParametersLayoutClicked() {
        Toast.makeText(context, "You cannot change these values while track recording is in progress", Toast.LENGTH_LONG).show();
    }


    @OnClick(R.id.startButton)
    public void onStartStopButtonClicked() {
        switch (trackType) {
            case 1:
                if (OBDRecordingService.CURRENT_SERVICE_STATE == BluetoothServiceState.SERVICE_STARTED) {
                    Intent intent = new Intent(getActivity(), OBDPlusGPSTrackRecordingScreen.class);
                    startActivity(intent);
                } else {
                    if (checkLocationPermission()) {
                        onOBDPlusGPSStartTrackButtonStartClicked();
                    } else {
                        requestLocationPermission();
                    }
                }
                break;
            case 2:
                if (GPSOnlyRecordingService.CURRENT_SERVICE_STATE == BluetoothServiceState.SERVICE_STARTED) {
                    Intent intent = new Intent(getActivity(), GPSOnlyTrackRecordingScreen.class);
                    startActivity(intent);
                } else {
                    if (checkLocationPermission()) {
                        onGPSOnlyStartTrackButtonStartClicked();
                    } else {
                        requestLocationPermission();
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSegmentedView();
        getUserCardDetails();
        updateUserDetailsView();
        if (trackType == 1) {
            updateStartStopButtonOBDPlusGPS(OBDRecordingService.CURRENT_SERVICE_STATE);
        } else if (trackType == 2) {
            updateStartStopButtonGPSOnly(GPSOnlyRecordingService.CURRENT_SERVICE_STATE);
        }
    }

    @Override
    public void onDestroyView() {
        LOG.info("onDestroyView()");
        super.onDestroyView();

        if (!subscriptions.isUnsubscribed()) {
            subscriptions.unsubscribe();
        }
    }

    private void updateSegmentedView() {
        //index 1 means OBD + GPS recording type
        //index 2 means GPS only recording type
        if (PreferencesHandler.getPreviouslySelectedRecordingType(context.getApplicationContext()) == 1) {
            obdGPSIndicator.setVisibility(View.VISIBLE);
            GPSIndicator.setVisibility(GONE);
            trackType = 1;
            showOBDPlusGPSSettings();

        } else {
            obdGPSIndicator.setVisibility(GONE);
            GPSIndicator.setVisibility(View.VISIBLE);
            trackType = 2;
            showGPSOnlySettings();
        }
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkLocationPermission() {
        int permissionState = ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i("Requesting Location", "Displaying permission rationale to provide additional context.");

            DialogUtils.createDefaultDialogBuilder(getContext(),
                    R.string.request_location_permission_title,
                    R.drawable.others_settings,
                    R.string.permission_rationale_location)
                    .positiveText(R.string.ok)
                    .onPositive((dialog, which) -> {
                        // Request permission
                        ActivityCompat.requestPermissions(getActivity(),
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                REQUEST_STORAGE_PERMISSION_REQUEST_CODE);
                    })
                    .show();

        } else {
            Log.i("Permissions", "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION_REQUEST_CODE);
        }
    }


    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkStoragePermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            LOG.debug("Requesting Storage. Displaying permission rationale to provide additional context.");

            DialogUtils.createDefaultDialogBuilder(getContext(),
                    R.string.request_storage_permission_title,
                    R.drawable.others_settings,
                    R.string.permission_rationale_file)
                    .positiveText(R.string.ok)
                    .onPositive((dialog, which) -> {
                        // Request permission
                        ActivityCompat.requestPermissions(getActivity(),
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                REQUEST_STORAGE_PERMISSION_REQUEST_CODE);
                    })
                    .show();

        } else {
            Log.i("Permissions", "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION_REQUEST_CODE);
        }
    }


    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        LOG.debug("onRequestPermissionResult");
        if (requestCode == REQUEST_LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                LOG.debug("User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                LOG.debug("Permission granted, updates requested, starting the recording procedure");
                onStartStopButtonClicked();
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, view -> {
                            // Build intent that displays the App settings screen.
                            Intent intent = new Intent();
                            intent.setAction(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package",
                                    BuildConfig.APPLICATION_ID, null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        });
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                LOG.debug("User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                LOG.debug("Permission granted, updates requested, starting the logging procedure");
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, view -> {
                            // Build intent that displays the App settings screen.
                            Intent intent = new Intent();
                            intent.setAction(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package",
                                    BuildConfig.APPLICATION_ID, null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        });
            }
        }
    }


    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                getActivity().findViewById(R.id.navigation),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    void showOBDPlusGPSSettings(){
        LOG.info("showOBDPlusGPSSettings");
        TransitionSet transitionSet = new TransitionSet()
                .addTransition(new ChangeBounds())
                .addTransition(new AutoTransition())
                .addTransition(new Slide(Gravity.LEFT)
                .addTarget(obdOptions));
        TransitionManager.beginDelayedTransition(optionsLayout, transitionSet);
        obdOptions.setVisibility(View.VISIBLE);
    }

    void showGPSOnlySettings(){
        LOG.info("showGPSOnlySettings");
        TransitionSet transitionSet = new TransitionSet()
                .addTransition(new ChangeBounds())
                .addTransition(new AutoTransition())
                .addTransition(new Slide(Gravity.LEFT)
                .addTarget(obdOptions));
        TransitionManager.beginDelayedTransition(optionsLayout, transitionSet);
        obdOptions.setVisibility(GONE);
    }

    void getUserCardDetails() {
        subscriptions.add(mDAOProvider.getTrackDAO().getTrackIdsObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Track>>() {
                    @Override
                    public void onStart() {
                        LOG.info("onStart() of getUserCardDetails");
                    }

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error(e.getMessage(), e);

                        if (e instanceof NotConnectedException) {
                            LOG.error("Error", e);
                        } else if (e instanceof UnauthorizedException) {
                            LOG.error("Unauthorised", e);
                        }
                        distance = -1.0;
                    }

                    @Override
                    public void onNext(List<Track> tracks) {
                        distance = -1.0;
                        timeInMillis = 0L;
                        for (Track track : tracks) {
                            distance += track.getLength();
                            timeInMillis += track.getTimeInMillis();
                        }

                        String time = convertMillisToDate();
                        PreferencesHandler.setTotalTime(context, time);
                        userTotalDurationAddTV.setText(time.charAt(time.length() - 1) + "");
                        LOG.info(time + " Duration");
                        userTotalDurationTV.setText(time.substring(0, time.length() - 1) + "");
                        PreferencesHandler.setTotalDistanceTravelledOfUser(context, distance);
                        userTotalDistanceTV.setText(DECIMAL_FORMATTER_TWO.format(PreferencesHandler.getTotalDistanceTravelledOfUser(context)));
                    }
                }));
    }

    String convertMillisToDate() {
        long diffSeconds = timeInMillis / 1000 % 60;
        long diffMinutes = timeInMillis / (60 * 1000) % 60;
        long diffHours = timeInMillis / (60 * 60 * 1000) % 24;
        long diffDays = timeInMillis / (24 * 60 * 60 * 1000);
        StringBuilder stringBuilder = new StringBuilder();
        if (diffDays != 0) {
            stringBuilder.append(diffDays);
            stringBuilder.append(" : ");
            if (diffHours > 1) {
                stringBuilder.append(diffHours);
            }
            stringBuilder.append("d");
        } else {
            if (diffHours != 0) {
                stringBuilder.append(diffHours);
                if (diffMinutes != 0) {
                    stringBuilder.append(" : ");
                    stringBuilder.append(diffMinutes);
                }
                stringBuilder.append("h");
            } else {
                if (diffMinutes != 0) {
                    stringBuilder.append(diffMinutes);
                    if (diffSeconds != 0) {
                        stringBuilder.append(" : ");
                        stringBuilder.append(diffSeconds);
                    }
                    stringBuilder.append("m");
                } else {
                    stringBuilder.append(diffSeconds);
                    stringBuilder.append("s");

                }
            }

        }
        return stringBuilder.toString();
    }

    void updateUserDetailsView(){
        if(mUserManager.isLoggedIn()){
            dashBoardName.setVisibility(View.VISIBLE);
            dashBoardUserImageView.setVisibility(View.VISIBLE);
            userStatisticsGroup.setVisibility(View.VISIBLE);
            userLoginSignupButtonGroup.setVisibility(GONE);

            if(mUserManager.getUser().getFirstName()==null)
                dashBoardName.setText(mUserManager.getUser().getUsername());
            else
                dashBoardName.setText(mUserManager.getUser().getName());

            // update the local track count.
            mTrackDAOHandler.getLocalTrackCount()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(integer -> {
                        PreferencesHandler.setLocalTrackCount(context, integer);
                        localTCount = true;
                        if (localTCount && remoteTCount) {
                            Integer total = integer + PreferencesHandler.getUploadedTrackCount(context);
                            userTrackCountTV.setText(total + "");
                        }
                    });

            // Update the Gravatar image.
            mUserManager.getGravatarBitmapObservable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(bitmap -> {
                        if (dashBoardUserImageView != null && dashBoardUserImageView.getVisibility() == View.VISIBLE && bitmap != null)
                            dashBoardUserImageView.setImageBitmap(bitmap);
                    });
            // Update the new values of the exp toolbar content.
            mBackgroundWorker.schedule(() -> {
                try {
                    final TrackDAO trackDAO = mDAOProvider.getTrackDAO();
                    final int totalTrackCount = trackDAO.getTotalTrackCount();
                    final int userTrackCount = trackDAO.getUserTrackCount();

                    String.format("%s (%s)", userTrackCount, totalTrackCount);
                    mMainThreadWorker.schedule(() -> {
                        remoteTCount = true;
                        if (localTCount && remoteTCount) {
                            Integer total = userTrackCount + PreferencesHandler.getLocalTrackCount(context);
                            userTrackCountTV.setText(total + "");
                        }
                        PreferencesHandler.setUploadedTrackCount(context, userTrackCount);
                        PreferencesHandler.setGlobalTrackCount(context, totalTrackCount);
                    });
                } catch (Exception e) {
                    LOG.warn(e.getMessage(), e);
                }
            });

        } else {
            dashBoardName.setVisibility(GONE);
            dashBoardUserImageView.setVisibility(GONE);
            userStatisticsGroup.setVisibility(GONE);
            userLoginSignupButtonGroup.setVisibility(View.VISIBLE);
        }
    }

    /**
     * @param device
     */
    private void setOBDTypeText(BluetoothDevice device) {
        getActivity().runOnUiThread(() -> {
            if (!mBluetoothHandler.isBluetoothEnabled()) {
                obdOptionsTxt1.setText(R.string.dashboard_bluetooth_disabled);
                obdOptionsTxt2.setText(R.string.dashboard_bluetooth_disabled_advise);
                obdOptionsImg.setImageResource(R.drawable.bluetooth_error);
                obdOptionsTxt2.setVisibility(View.VISIBLE);
            } else if (device == null) {
                obdOptionsTxt1.setText(R.string.dashboard_obd_not_selected);
                obdOptionsTxt2.setText(R.string.dashboard_obd_not_selected_advise);
                obdOptionsImg.setImageResource(R.drawable.obd_nc);
                obdOptionsTxt2.setVisibility(View.VISIBLE);
            } else if (mBluetoothHandler.isAutoconnecting()){
                obdOptionsTxt1.setText("Autoconnecting");
                obdOptionsTxt2.setText("Trying to Autoconnect to the OBD Device");
                obdOptionsImg.setImageResource(R.drawable.obd);
                obdOptionsTxt2.setVisibility(View.VISIBLE);
            } else if (!mBluetoothHandler.isBluetoothActive()){
                obdOptionsTxt1.setText("OBD-II adapter not found");
                obdOptionsTxt2.setText("Unable to pair with the current selected OBD-II adapter");
                obdOptionsImg.setImageResource(R.drawable.obd);
                obdOptionsTxt2.setVisibility(View.VISIBLE);
            } else {
                obdOptionsTxt1.setText(device.getName());
                obdOptionsTxt2.setText(device.getAddress());
                obdOptionsImg.setImageResource(R.drawable.obd_connected);
                obdOptionsTxt2.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * @param car
     */
    private void setCarTypeText(Car car) {
        if (car != null) {
            CarOptionsTxt1.setText(String.format("%s - %s",
                    car.getManufacturer(),
                    car.getModel()));


            CarOptionsTxt2.setText(String.format("%s    %s    %s ccm",
                    car.getConstructionYear(),
                    car.getFuelType(),
                    car.getEngineDisplacement()));

            CarOptionsTxt2.setVisibility(View.VISIBLE);
        } else {
            CarOptionsTxt1.setText(R.string.dashboard_carselection_no_car_selected);
            CarOptionsTxt2.setText(R.string.dashboard_carselection_no_car_selected_advise);
            CarOptionsTxt2.setVisibility(View.VISIBLE);
        }
    }

    private void setGPSOptions(boolean isGPSEnabled){
        if(isGPSEnabled == false){
            GPSOptionsTxt1.setText("GPS is not enabled");
            GPSOptionsTxt2.setText("Please turn on location services");
            GPSOptionsImg.setImageResource(R.drawable.gps_off);
        }
        else{
            GPSOptionsTxt1.setText("GPS is enabled");
            GPSOptionsTxt2.setText("enviroCar has access to your location");
            GPSOptionsImg.setImageResource(R.drawable.gps_connected);
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
        Animation animation = AnimationUtils.loadAnimation(getActivity(), animResource);
        if (hide) {
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    // nothing to do..
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.setVisibility(GONE);
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

    /**
     * Receiver method for {@link TrackRecordingServiceStateChangedEvent}s posted on the event bus.
     *
     * @param event the corresponding event type.
     */
    @Subscribe
    public void onReceiveTrackRecordingServiceStateChangedEvent(
            TrackRecordingServiceStateChangedEvent event) {
        LOG.info(String.format("onReceiveTrackRecordingServiceStateChangedEvent(): %s",
                event.toString()));
        if (event.mState == BluetoothServiceState.SERVICE_STARTED && mConnectingDialog != null) {
            mConnectingDialog.dismiss();
            mConnectingDialog = null;
        }

        mMainThreadWorker.schedule(() -> {
            // Update the start stop button.
            if (trackType == 1) {
                if (event.mState == BluetoothServiceState.SERVICE_STARTED) {
                    getActivity().startActivity(new Intent(getActivity(), OBDPlusGPSTrackRecordingScreen.class));
                }
                updateStartStopButtonOBDPlusGPS(event.mState);
            } else if (trackType == 2) {
                updateStartStopButtonGPSOnly(event.mState);
                if (event.mState == BluetoothServiceState.SERVICE_STARTED) {
                    getActivity().startActivity(new Intent(getActivity(), GPSOnlyTrackRecordingScreen.class));
                }
            }
        });
    }

    @Subscribe
    public void onReceiveBluetoothDeviceSelectedEvent(BluetoothDeviceSelectedEvent event) {
        LOG.debug(String.format("Received event: %s", event.toString()));
        setOBDTypeText(event.mDevice);
    }

    @Subscribe
    public void onReceiveBluetoothStateChangedEvent(BluetoothStateChangedEvent event) {
        LOG.info(String.format("onReceiveBluetoothStateChangedEvent(isEnabled=%s)",
                "" + event.isBluetoothEnabled));
        mMainThreadWorker.schedule(() -> {
            if (trackType == 1) {
                updateStartStopButtonOBDPlusGPS(OBDRecordingService.CURRENT_SERVICE_STATE);
            } else if (trackType == 2) {
                updateStartStopButtonGPSOnly(GPSOnlyRecordingService.CURRENT_SERVICE_STATE);
            }

            setOBDTypeText(mBluetoothHandler.getSelectedBluetoothDevice());
        });
    }

    @Subscribe
    public void onReceiveNewCarTypeSelectedEvent(NewCarTypeSelectedEvent event) {
        LOG.debug(String.format("Received event: %s", event.toString()));
        mMainThreadWorker.schedule(() -> {
            if (trackType == 1) {
                updateStartStopButtonOBDPlusGPS(OBDRecordingService.CURRENT_SERVICE_STATE);
            } else if (trackType == 2) {
                updateStartStopButtonGPSOnly(GPSOnlyRecordingService.CURRENT_SERVICE_STATE);
            }

            setCarTypeText(event.mCar);
        });
    }

    @Subscribe
    public void onReceiveGpsStatusChangedEvent(GpsStateChangedEvent event) {
        mMainThreadWorker.schedule(() -> {
            if (trackType == 1) {
                updateStartStopButtonOBDPlusGPS(OBDRecordingService.CURRENT_SERVICE_STATE);
            } else if (trackType == 2) {
                updateStartStopButtonGPSOnly(GPSOnlyRecordingService.CURRENT_SERVICE_STATE);
            }
            setGPSOptions(event.mIsGPSEnabled);
        });
    }

    private void updateBannerForGPSOnlyType() {
        LOG.info("updateBannerForGPSOnlyType() called");
        TransitionManager.beginDelayedTransition(buttonBanner);
        errorImageBluetooth.setVisibility(GONE);
        errorImageOBDAdapter.setVisibility(GONE);
        okImageBluetooth.setVisibility(GONE);
        okImageOBDAdapter.setVisibility(GONE);
        bannerBluetoothContainer.setVisibility(GONE);
        bannerOBDAdapterContainer.setVisibility(GONE);
        LOG.info("Bluetooth and OBD containers hidden");
        if (!mLocationHandler.isGPSEnabled()) {
            errorImageGPS.setVisibility(View.VISIBLE);
            okImageGPS.setVisibility(GONE);
        } else {
            errorImageGPS.setVisibility(GONE);
            okImageGPS.setVisibility(View.VISIBLE);
        }
        if (mCarManager.getCar() == null) {
            errorImageCar.setVisibility(View.VISIBLE);
            okImageCar.setVisibility(GONE);
        } else {
            errorImageCar.setVisibility(GONE);
            okImageCar.setVisibility(View.VISIBLE);
        }
    }

    private void updateBannerForOBDPlusGPSType() {
        LOG.info("updateBannerForOBDPlusGPSType() called");
        TransitionManager.beginDelayedTransition(buttonBanner);
        bannerBluetoothContainer.setVisibility(View.VISIBLE);
        bannerOBDAdapterContainer.setVisibility(View.VISIBLE);
        LOG.info("Bluetooth and OBD containers loaded.");
        if (!mBluetoothHandler.isBluetoothEnabled()) {
            errorImageBluetooth.setVisibility(View.VISIBLE);
            okImageBluetooth.setVisibility(GONE);
        } else {
            errorImageBluetooth.setVisibility(GONE);
            okImageBluetooth.setVisibility(View.VISIBLE);
        }
        if (mBluetoothHandler.getSelectedBluetoothDevice() == null) {
            errorImageOBDAdapter.setVisibility(View.VISIBLE);
            okImageOBDAdapter.setVisibility(GONE);
        } else {
            errorImageOBDAdapter.setVisibility(GONE);
            okImageOBDAdapter.setVisibility(View.VISIBLE);
        }
        if (!mLocationHandler.isGPSEnabled()) {
            errorImageGPS.setVisibility(View.VISIBLE);
            okImageGPS.setVisibility(GONE);
        } else {
            errorImageGPS.setVisibility(GONE);
            okImageGPS.setVisibility(View.VISIBLE);
        }
        if (mCarManager.getCar() == null) {
            errorImageCar.setVisibility(View.VISIBLE);
            okImageCar.setVisibility(GONE);
        } else {
            errorImageCar.setVisibility(GONE);
            okImageCar.setVisibility(View.VISIBLE);
        }
    }

    private void updateStartStopButtonOBDPlusGPS(BluetoothServiceState state) {
        //First update the banner
        updateBannerForOBDPlusGPSType();

        switch (state) {
            case SERVICE_STOPPED:
                disableChangingParametersLayout.setVisibility(GONE);
                if (hasSettingsSelectedFOROBD()) {
                    updateStartStopButton(R.drawable.btn_dark,
                            getString(R.string.dashboard_start_track), true);
                } else {
                    updateStartStopButton(R.drawable.btn_grey,
                            getString(R.string.dashboard_start_track), false);
                }
                break;
            case SERVICE_STARTED:
                disableChangingParametersLayout.setVisibility(View.VISIBLE);
                // Update the StartStopButton
                updateStartStopButton(R.drawable.btn_dark, getString(R.string.dashboard_goto_track), true);
                // hide the info field when the track is started.
                //mInfoField.setVisibility(View.INVISIBLE);
                break;
            case SERVICE_STARTING:
                disableChangingParametersLayout.setVisibility(View.VISIBLE);
                updateStartStopButton(R.drawable.btn_grey,
                        getString(R.string.dashboard_track_is_starting), false);
                break;
            case SERVICE_STOPPING:
                disableChangingParametersLayout.setVisibility(View.VISIBLE);
                updateStartStopButton(R.drawable.btn_grey,
                        getString(R.string.dashboard_track_is_stopping), false);
                break;
            default:
                break;
        }
    }

    private void updateStartStopButtonGPSOnly(BluetoothServiceState state) {

        //First update the banner
        updateBannerForGPSOnlyType();

        switch (state) {
            case SERVICE_STOPPED:
                disableChangingParametersLayout.setVisibility(GONE);
                if (hasSettingsSelectedFORGPSOnly()) {
                    updateStartStopButton(R.drawable.btn_dark,
                            getString(R.string.dashboard_start_track), true);
                } else {
                    updateStartStopButton(R.drawable.btn_grey,
                            getString(R.string.dashboard_start_track), false);
                }
                break;
            case SERVICE_STARTED:
                disableChangingParametersLayout.setVisibility(View.VISIBLE);
                // Update the StartStopButton
                updateStartStopButton(R.drawable.btn_dark, getString(R.string.dashboard_goto_track), true);
                // hide the info field when the track is started.
                //mInfoField.setVisibility(View.INVISIBLE);
                break;
            case SERVICE_STARTING:
                disableChangingParametersLayout.setVisibility(View.VISIBLE);
                updateStartStopButton(R.drawable.btn_grey,
                        getString(R.string.dashboard_track_is_starting), false);
                break;
            case SERVICE_STOPPING:
                disableChangingParametersLayout.setVisibility(View.VISIBLE);
                updateStartStopButton(R.drawable.btn_grey,
                        getString(R.string.dashboard_track_is_stopping), false);
                break;
            default:
                break;
        }
    }

    private boolean hasSettingsSelectedFOROBD() {
        return mBluetoothHandler.isBluetoothEnabled() &&
                mBluetoothHandler.getSelectedBluetoothDevice() != null &&
                mLocationHandler.isGPSEnabled() &&
                mCarManager.getCar() != null;
    }

    private boolean hasSettingsSelectedFORGPSOnly() {
        return mLocationHandler.isGPSEnabled() &&
                mCarManager.getCar() != null;
    }

    private void updateStartStopButton(int background, String text, boolean enabled) {
        TransitionSet transitionSet = new TransitionSet()
                .addTransition(new Recolor())
                .addTarget(R.id.startButton);

        TransitionManager.beginDelayedTransition(optionsLayout, transitionSet);
        startButton.setBackgroundResource(background);
            if(background == R.drawable.btn_grey)
                startButton.setTextColor(Color.BLACK);
            else
                startButton.setTextColor(Color.WHITE);
            startButton.setText(text);
            startButton.setEnabled(enabled);
    }

    private void onOBDPlusGPSStartTrackButtonStartClicked() {
        if (!mBluetoothHandler.isBluetoothEnabled()) {
            Snackbar.make(getView(), R.string.dashboard_bluetooth_disabled_snackbar,
                    Snackbar.LENGTH_LONG);
            return;
        }

        final BluetoothDevice device = mBluetoothHandler.getSelectedBluetoothDevice();

        // Start the background remoteService.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getActivity().startForegroundService(
                    new Intent(getActivity(), OBDRecordingService.class));
        } else {
            getActivity().startService(
                    new Intent(getActivity(), OBDRecordingService.class));
        }

        View contentView = getActivity().getLayoutInflater().inflate(
                R.layout.fragment_dashboard_connecting_dialog, null, false);
        TextView textView = contentView.findViewById(R.id.fragment_dashboard_connecting_dialog_text);
        textView.setText(String.format(getString(R.string.dashboard_connecting_find_template), device.getName()));

        this.mConnectingDialog = DialogUtils.createDefaultDialogBuilder(getContext(), R.string.dashboard_connecting, R.drawable.ic_bluetooth_searching_black_24dp, contentView)
                .cancelable(false)
                .negativeText(R.string.cancel)
                .onNegative((materialDialog, dialogAction) -> {
                    getActivity().stopService(new Intent(getActivity(), OBDRecordingService.class));
                })
                .show();
    }

    private void onGPSOnlyStartTrackButtonStartClicked() {
        // Start the background remoteService.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getActivity().startForegroundService(new Intent(getActivity(), GPSOnlyRecordingService.class));
        } else {
            getActivity().startService(new Intent(getActivity(), GPSOnlyRecordingService.class));
        }
    }

}
