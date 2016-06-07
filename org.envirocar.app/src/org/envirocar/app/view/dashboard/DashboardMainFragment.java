/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.app.view.dashboard;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.handler.LocationHandler;
import org.envirocar.app.handler.TrackRecordingHandler;
import org.envirocar.app.services.OBDConnectionService;
import org.envirocar.app.view.utils.DialogUtils;
import org.envirocar.core.events.NewCarTypeSelectedEvent;
import org.envirocar.core.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.core.events.gps.GpsStateChangedEvent;
import org.envirocar.core.injection.BaseInjectorFragment;
import org.envirocar.core.logging.Logger;
import org.envirocar.obd.events.BluetoothServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

/**
 * @author dewall
 */
public class DashboardMainFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(DashboardMainFragment.class);
    private OBDConnectionService mOBDConnectionService;
    private boolean mIsOBDConnectionBounded;

    @Inject
    protected BluetoothHandler mBluetoothHandler;
    @Inject
    protected CarPreferenceHandler mCarManager;
    @Inject
    protected TrackRecordingHandler mTrackRecordingHandler;
    @Inject
    protected LocationHandler mLocationHandler;

    @InjectView(R.id.fragment_startup_info_field)
    protected View mInfoField;
    @InjectView(R.id.fragment_startup_info_text)
    protected TextView mInfoText;
    @InjectView(R.id.fragment_startup_start_button)
    protected View mStartStopButton;
    @InjectView(R.id.fragment_startup_start_button_inner)
    protected TextView mStartStopButtonInner;

    private MaterialDialog mConnectingDialog;

    private Scheduler.Worker mMainThreadScheduler = AndroidSchedulers.mainThread().createWorker();

    private BluetoothServiceState mServiceState = BluetoothServiceState.SERVICE_STOPPED;

    // All the sub-fragments to show in this fragment.
    private Fragment mCurrentlyVisible;
    private Fragment mDashboardHeaderFragment;
    private Fragment mDashboardSettingsFragment;
    private Fragment mDashboardMapFragment = new DashboardMapFragment();
    private Fragment mDashboardTempomatFragment = new DashboardTempomatFragment();
    private Fragment mDashboardTrackMapFragment = new DashboardTrackMapFragment();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        LOG.info("onCreateView()");

        // This setting is an essential requirement to catch the events of a sub-fragment's
        // options shown in the toolbar.
        setHasOptionsMenu(true);

        // First inflate the general dashboard view.
        View contentView = inflater.inflate(R.layout.fragment_startup, container, false);

        // Inject all dashboard-related views.
        ButterKnife.inject(this, contentView);

        // Get the settings fragment and the header fragment.
        mDashboardSettingsFragment = getChildFragmentManager()
                .findFragmentById(R.id.fragment_startup_settings_layout);
        mDashboardHeaderFragment = getChildFragmentManager()
                .findFragmentById(R.id.fragment_dashboard_header_fragment);

        // TODO fix this. The static remoteService state is just a workaround.
        onShowServiceStateUI(OBDConnectionService.CURRENT_SERVICE_STATE);

        // Initially hide the header fragment.
        //        hideFragment(mDashboardHeaderFragment, -1, -1);

        return contentView;
    }

    @Override
    public void onResume() {
        updateStartStopButton(OBDConnectionService.CURRENT_SERVICE_STATE);
        updateInfoField();
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        if (!getActivity().isFinishing() && mDashboardSettingsFragment != null) {
            try {
                getFragmentManager().beginTransaction()
                        .remove(mDashboardSettingsFragment)
                        .remove(mDashboardHeaderFragment)
                        .commit();
            } catch (IllegalStateException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        LOG.info("onDestroy()");

        if(!getActivity().isFinishing() && mDashboardSettingsFragment != null){
            try{
                getFragmentManager().beginTransaction()
                        .remove(mDashboardMapFragment)
                        .commit();
            } catch (IllegalStateException e){
                LOG.warn(e.getMessage(), e);
            }
        }

        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_dashboard_tempomat_map:
                if (mDashboardTrackMapFragment.isVisible())
                    return false;

                replaceFragment(mDashboardTrackMapFragment,
                        R.id.fragment_startup_container,
                        R.anim.translate_slide_in_left_fragment,
                        R.anim.translate_slide_out_right_fragment);
                return true;
            case R.id.menu_dashboard_tempomat_show_cruise:
                if (mDashboardTempomatFragment.isVisible())
                    return false;

                replaceFragment(mDashboardTempomatFragment,
                        R.id.fragment_startup_container,
                        R.anim.translate_slide_in_right_fragment,
                        R.anim.translate_slide_out_left_fragment);
                return true;
        }
        return false;
    }

    @OnClick(R.id.fragment_startup_start_button)
    public void onStartStopButtonClicked() {
        switch (OBDConnectionService.CURRENT_SERVICE_STATE) {
            case SERVICE_STOPPED:
                onButtonStartClicked();
                break;
            case SERVICE_STARTED:
                onButtonStopClicked();
                break;
            default:
                break;
        }
    }

    private void onButtonStartClicked() {
        if (!mBluetoothHandler.isBluetoothEnabled()) {
            Snackbar.make(getView(), R.string.dashboard_bluetooth_disabled_snackbar,
                    Snackbar.LENGTH_LONG);
            return;
        }

        final BluetoothDevice device = mBluetoothHandler.getSelectedBluetoothDevice();

        mBluetoothHandler.startBluetoothDiscoveryForSingleDevice(device)
                .subscribe(new Subscriber<BluetoothDevice>() {
                    private boolean found = false;
                    private View contentView;
                    private TextView textView;

                    @Override
                    public void onStart() {
                        contentView = getActivity().getLayoutInflater().inflate(
                                R.layout.fragment_dashboard_connecting_dialog, null, false);
                        textView = (TextView) contentView.findViewById(
                                R.id.fragment_dashboard_connecting_dialog_text);
                        textView.setText(String.format(
                                getString(R.string.dashboard_connecting_find_template),
                                device.getName()));

                        mConnectingDialog = DialogUtils.createDefaultDialogBuilder(getContext(),
                                R.string.dashboard_connecting,
                                R.drawable.ic_bluetooth_searching_white_24dp,
                                contentView)
                                .cancelable(false)
                                .negativeText(R.string.cancel)
                                .onNegative((materialDialog, dialogAction) -> {
                                    // On cancel, first stop the discovery of other
                                    // bluetooth devices.
                                    mBluetoothHandler.stopBluetoothDeviceDiscovery();
                                    if (found) {
                                        // and if the remoteService is already started, then
                                        // stop it.
                                        getActivity().stopService(new Intent
                                                (getActivity(), OBDConnectionService
                                                        .class));
                                    }
                                    found = true;
                                })
                                .show();
                    }

                    @Override
                    public void onCompleted() {
                        if (!found) {
                            mConnectingDialog.dismiss();
                            mConnectingDialog = DialogUtils.createDefaultDialogBuilder(getContext(),
                                    R.string.dashboard_dialog_obd_not_found,
                                    R.drawable.ic_bluetooth_searching_white_24dp,
                                    String.format(getString(
                                            R.string.dashboard_dialog_obd_not_found_content_template),
                                            device.getName()))
                                    .negativeText(R.string.ok)
                                    .show();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        mConnectingDialog.setActionButton(DialogAction.NEGATIVE, "Okay");
                    }

                    @Override
                    public void onNext(BluetoothDevice bluetoothDevice) {
                        found = true;

                        // Stop the Bluetooth discovery such that the connection can be
                        // faster established.
                        mBluetoothHandler.stopBluetoothDeviceDiscovery();

                        // Update the content of the connecting dialog.
                        textView.setText(String.format(getString(
                                R.string.dashboard_connecting_found_template), device.getName()));

                        // Start the background remoteService.
                        getActivity().startService(
                                new Intent(getActivity(), OBDConnectionService.class));
                    }
                });

    }

    private void onButtonStopClicked() {
        new MaterialDialog.Builder(getActivity())
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
     * Receiver method for {@link BluetoothServiceStateChangedEvent}s posted on the event bus.
     *
     * @param event the corresponding event type.
     */
    @Subscribe
    public void onReceiveBluetoothServiceStateChangedEvent(
            BluetoothServiceStateChangedEvent event) {
        LOG.info(String.format("onReceiveBluetoothServiceStateChangedEvent(): %s",
                event.toString()));
        mServiceState = event.mState;
        if (mServiceState == BluetoothServiceState.SERVICE_STARTED && mConnectingDialog != null) {
            mConnectingDialog.dismiss();
            mConnectingDialog = null;
        }

        mMainThreadScheduler.schedule(() -> {
            onShowServiceStateUI(event.mState);
            // Update the start stop button.
            updateStartStopButton(event.mState);
            // Update the info field.
            updateInfoField();
        });
    }

    @Subscribe
    public void onReceiveBluetoothStateChangedEvent(BluetoothStateChangedEvent event) {
        LOG.info(String.format("onReceiveBluetoothStateChangedEvent(isEnabled=%s)",
                "" + event.isBluetoothEnabled));
        mMainThreadScheduler.schedule(() -> {
            updateStartStopButton(OBDConnectionService.CURRENT_SERVICE_STATE);
            // Update the info field.
            updateInfoField();
        });
    }

    @Subscribe
    public void onReceiveNewCarTypeSelectedEvent(NewCarTypeSelectedEvent event) {
        mMainThreadScheduler.schedule(() -> {
            updateStartStopButton(OBDConnectionService.CURRENT_SERVICE_STATE);
            // Update the info field.
            updateInfoField();
        });
    }

    @Subscribe
    public void onReceiveGpsStatusChangedEvent(GpsStateChangedEvent event) {
        mMainThreadScheduler.schedule(() -> {
            updateStartStopButton(OBDConnectionService.CURRENT_SERVICE_STATE);
            // Update the info field.
            updateInfoField();
        });
    }


    private void onShowServiceStateUI(BluetoothServiceState state) {
        switch (state) {
            case SERVICE_STOPPED:
                if (getFragmentManager() == null)
                    return;

                if (!mDashboardSettingsFragment.isVisible())
                    showFragment(mDashboardSettingsFragment,
                            R.anim.translate_slide_in_left_fragment,
                            R.anim.translate_slide_out_left_fragment);

                // Hide the header fragment
                hideFragment(mDashboardHeaderFragment,
                        mCurrentlyVisible != null ? R.anim.translate_slide_in_top_fragment : -1,
                        mCurrentlyVisible != null ? R.anim.translate_slide_out_top_fragment : -1);

                // Replace the container with the mapview.
                if (mCurrentlyVisible != mDashboardMapFragment) {
                    // TODO HERE CHANGE TO TRACK MAP FRAGMENT
                    replaceFragment(mDashboardMapFragment, R.id.fragment_startup_container,
                            mCurrentlyVisible != null ?
                                    R.anim.translate_slide_in_left_fragment : -1,
                            mCurrentlyVisible != null ?
                                    R.anim.translate_slide_out_right_fragment : -1);
                }

                mCurrentlyVisible = mDashboardMapFragment;


                break;
            case SERVICE_STARTED:
                // Hide the settings if visible
                //                if (mDashboardSettingsFragment.isVisible()) {
                hideFragment(mDashboardSettingsFragment,
                        R.anim.translate_slide_in_left_fragment,
                        R.anim.translate_slide_out_left_fragment);
                //                }

                //                if (!mDashboardHeaderFragment.isVisible())
                showFragment(mDashboardHeaderFragment,
                        R.anim.translate_slide_in_top_fragment,
                        R.anim.translate_slide_out_top_fragment);

                // Show the tempomat fragment
                if (!mDashboardTempomatFragment.isVisible())
                    replaceFragment(mDashboardTempomatFragment,
                            R.id.fragment_startup_container,
                            R.anim.translate_slide_in_right_fragment,
                            R.anim.translate_slide_out_left_fragment);

                mCurrentlyVisible = mDashboardTempomatFragment;

                break;
            case SERVICE_STOPPING:
                break;
        }
    }

    /**
     * Updates the info field of the default startup fragment.
     */
    @UiThread
    private void updateInfoField() {
        boolean showInfo = false;
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.dashboard_info_base));
        if (!mLocationHandler.isGPSEnabled()) {
            sb.append(getString(R.string.dashboard_info_activate_gps));
            showInfo = true;
        }
        if (!mBluetoothHandler.isBluetoothEnabled()) {
            sb.append(getString(R.string.dashboard_info_activate_bluetooth));
            showInfo = true;
        } else if (mBluetoothHandler.getSelectedBluetoothDevice() == null) {
            sb.append(getString(R.string.dashboard_info_select_obd_adapter));
            showInfo = true;
        }
        if (mCarManager.getCar() == null) {
            sb.append(getString(R.string.dashboard_info_select_car_type));
            showInfo = true;
        }
        if (showInfo) {
            mInfoText.setText(sb.toString());
            mInfoField.setVisibility(View.VISIBLE);
        } else {
            mInfoField.setVisibility(View.INVISIBLE);
        }
    }


    private void updateStartStopButton(BluetoothServiceState state) {
        switch (state) {
            case SERVICE_STOPPED:
                if (hasSettingsSelected()) {
                    updateStartStopButton(getResources().getColor(R.color.green_dark_cario),
                            getString(R.string.dashboard_start_track), true);
                } else {
                    updateStartStopButton(Color.GRAY,
                            getString(R.string.dashboard_start_track), false);
                }
                break;
            case SERVICE_STARTED:
                // Update the StartStopButton
                updateStartStopButton(Color.RED, getString(R.string.dashboard_stop_track), true);
                // hide the info field when the track is started.
                mInfoField.setVisibility(View.INVISIBLE);
                break;
            case SERVICE_STARTING:
                updateStartStopButton(Color.GRAY,
                        getString(R.string.dashboard_track_is_starting), false);
                break;
            case SERVICE_STOPPING:
                updateStartStopButton(Color.GRAY,
                        getString(R.string.dashboard_track_is_stopping), false);
                break;
            default:
                break;
        }
    }

    private boolean hasSettingsSelected() {
        return mBluetoothHandler.isBluetoothEnabled() &&
                mBluetoothHandler.getSelectedBluetoothDevice() != null &&
                mLocationHandler.isGPSEnabled() &&
                mCarManager.getCar() != null;
    }

    private void updateStartStopButton(int color, String text, boolean enabled) {
        mMainThreadScheduler.schedule(() -> {
            mStartStopButtonInner.setBackgroundColor(color);
            mStartStopButtonInner.setText(text);
            mStartStopButton.setEnabled(enabled);
        });
    }

    /**
     * @param fragment
     * @param container
     * @param enterAnimation
     * @param exitAnimation
     */
    private void replaceFragment(Fragment fragment, int container, int enterAnimation, int
            exitAnimation) {
        if (fragment == null || getFragmentManager() == null)
            return;

        FragmentTransaction transaction = getActivity()
                .getSupportFragmentManager()
                .beginTransaction();
        if (enterAnimation != -1 && exitAnimation != -1) {
            transaction.setCustomAnimations(enterAnimation, exitAnimation);
        }

        transaction.replace(container, fragment);
        transaction.commitAllowingStateLoss();

        mCurrentlyVisible = fragment;
    }

    /**
     * @param fragment
     * @param enterAnimation
     * @param exitAnimation
     */
    private void showFragment(Fragment fragment, int enterAnimation, int exitAnimation) {
        if (fragment == null || getFragmentManager() == null)
            return;

        FragmentTransaction transaction = getActivity().getSupportFragmentManager()
                .beginTransaction();
        if (enterAnimation != -1) {
            transaction.setCustomAnimations(enterAnimation, exitAnimation);
        }
        transaction.show(fragment);
        transaction.commitAllowingStateLoss();
    }

    /**
     * @param fragment
     * @param enterAnimation
     * @param exitAnimation
     */
    private void hideFragment(Fragment fragment, int enterAnimation, int exitAnimation) {
        if (fragment == null || getFragmentManager() == null)
            return;

        FragmentTransaction transaction = getActivity().getSupportFragmentManager()
                .beginTransaction();
        if (exitAnimation != -1) {
            transaction.setCustomAnimations(enterAnimation, exitAnimation);
        }
        transaction.hide(fragment);
        transaction.commitAllowingStateLoss();
    }

    @UiThread
    private void onServiceStarting() {
//        bindService();
    }

    @UiThread
    private void onServiceStarted() {
        // Hide the dashboard settings fragment if visible.
        if (mDashboardSettingsFragment.isVisible()) {
            getFragmentManager()
                    .beginTransaction()
                    .hide(mDashboardSettingsFragment)
                    .commitAllowingStateLoss();
        }

        getFragmentManager()
                .beginTransaction()
                .show(mDashboardHeaderFragment)
                .commitAllowingStateLoss();

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_startup_container, new DashboardTempomatFragment())
                .commitAllowingStateLoss();

        updateStartToStopButton();
    }

    @UiThread
    private void onServiceStopping() {
//        unbindService();
    }

    @UiThread
    private void onServiceStopped() {
        getFragmentManager().beginTransaction()
                .hide(mDashboardTempomatFragment)
                .hide(mDashboardHeaderFragment)
                .replace(R.id.fragment_startup_container, mDashboardMapFragment)
                .commitAllowingStateLoss();

        if (mDashboardSettingsFragment != null) {
            getFragmentManager()
                    .beginTransaction()
                    .show(mDashboardSettingsFragment)
                    .commitAllowingStateLoss();
        }

        updateStartToStopButton();
    }

    @UiThread
    private void updateStartToStopButton() {
        if (mServiceState == BluetoothServiceState.SERVICE_STARTED) {
            mStartStopButtonInner.setBackgroundColor(Color.RED);
            mStartStopButtonInner.setText("STOP TRACK");
        } else if (mServiceState == BluetoothServiceState.SERVICE_STOPPED) {
            mStartStopButtonInner.setBackgroundColor(
                    getResources().getColor(R.color.green_dark_cario));
            mStartStopButtonInner.setText("START TRACK");
        }
    }

}
