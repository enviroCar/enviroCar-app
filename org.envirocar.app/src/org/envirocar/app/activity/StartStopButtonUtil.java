/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.app.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.envirocar.app.BaseApplication;
import org.envirocar.app.BaseMainActivity;
import org.envirocar.app.R;
import org.envirocar.app.activity.DialogUtil.DialogCallback;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.handler.PreferenceConstants;
import org.envirocar.app.handler.TrackRecordingHandler;
import org.envirocar.app.services.OBDConnectionService;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.InjectApplicationScope;
import org.envirocar.obd.service.BluetoothServiceState;

import javax.inject.Inject;

/**
 * Outsource of the start/stop button interaction and content updates.
 * objects of this class can be created without leaking any resources -
 * they simply hold the state-related flags and backlinks to the Activity.
 *
 * @author matthes rieke
 */
public class StartStopButtonUtil {
    private static final Logger LOG = Logger.getLogger(StartStopButtonUtil.class);

    @Inject
    @InjectApplicationScope
    protected Context mContext;
    @Inject
    protected CarPreferenceHandler mCarManager;
    @Inject
    protected TrackRecordingHandler mTrackRecordingHandler;

    private int trackMode;
    private BluetoothServiceState serviceState = BluetoothServiceState.SERVICE_STOPPED;
    private boolean deviceDiscoveryActive;

    /**
     * @param context
     * @param trackMode
     * @param serviceState
     * @param deviceDiscoveryActive
     */
    public StartStopButtonUtil(Context context, int trackMode,
                               BluetoothServiceState serviceState, boolean deviceDiscoveryActive) {
        // Inject variables.
        BaseApplication.get(context).getBaseApplicationComponent().inject(this);

        this.trackMode = trackMode;
        this.serviceState = serviceState;
        this.deviceDiscoveryActive = deviceDiscoveryActive;
    }

    /**
     * Update the UI contents of the button. This method
     * DOES NOT fire any remoteService state changes, it is completely
     * passive.
     *
     * @param button the drawer button
     */
    public void updateStartStopButtonOnServiceStateChange(NavMenuItem button) {
        LOG.info("updateStartStopButtonOnServiceStateChange called with state: " +
                serviceState + " / trackMode: " + trackMode +
                " discovery: " + deviceDiscoveryActive);

        switch (serviceState) {
            case SERVICE_STOPPED:
                handleServiceStoppedState(button);
                break;
            case SERVICE_DEVICE_DISCOVERY_PENDING:
                handleServiceDeviceDiscoveryPendingState(button);
                // TODO ist das so gewollt ohne break?
            case SERVICE_STARTING:
                handleServiceStartingState(button);
                break;
            case SERVICE_STARTED:
                handleServiceStartedState(button);
                break;
            default:
                break;
        }
    }


    /**
     * React to a button click, considering the current state of the
     * application and its services. This method fires events
     * and remoteService starts actively.
     *
     * @param trackModeListener a callback to handle the inputs of the user
     */
    public void processButtonClick(OnTrackModeChangeListener trackModeListener) {
        LOG.info("processButtonClick called with state: " + serviceState + " / trackMode: " +
                trackMode +
                " discovery: " + deviceDiscoveryActive);

        switch (serviceState) {
            case SERVICE_STOPPED:
                processStoppedStateClick(trackModeListener);
                break;
            case SERVICE_DEVICE_DISCOVERY_PENDING:
                //                processPendingStateClick();
                break;
            case SERVICE_STARTING:
                processStartingStateClick();
                break;
            case SERVICE_STARTED:
                processStartedStateClick(trackModeListener);
                break;
            default:
                break;
        }
    }

    /**
     * convenience method to define the contents of a button,
     * using String objects.
     */
    public void defineButtonContents(NavMenuItem button, boolean enabled,
                                     int iconRes, String subtitle, String title) {
        button.setEnabled(enabled);
        button.setIconRes(iconRes);
        button.setSubtitle(subtitle);
        if (title != null) {
            button.setTitle(title);
        }
    }

    /**
     * convenience method to define the contents of a button,
     * using string resource id for subtitle and String for title.
     */
    public void defineButtonContents(NavMenuItem button, boolean enabled,
                                     int iconRes, String subtitle) {
        defineButtonContents(button, enabled, iconRes, subtitle, null);
    }

    private void defineButtonContents(NavMenuItem button, boolean enabled,
                                      int iconRes, int subtitleRes) {
        defineButtonContents(button, enabled, iconRes, mContext.getString(subtitleRes));
    }

    private void createStopTrackDialog(final OnTrackModeChangeListener trackModeListener) {
        int titleId;
        int messageId;
        switch (trackMode) {
            case BaseMainActivity.TRACK_MODE_SINGLE:
                titleId = R.string.finish_track;
                messageId = R.string.finish_track_long;
                break;
            case BaseMainActivity.TRACK_MODE_AUTO:
                titleId = R.string.stop_automatic_mode;
                messageId = R.string.stop_automatic_mode_long;
                break;
            default:
                //                Crouton.makeText(mContext, "not supported", Style.INFO).show();
                return;
        }

        DialogUtil.createTitleMessageDialog(titleId, messageId,
                new DialogUtil.PositiveNegativeCallback() {
                    @Override
                    public void positive() {
                        mTrackRecordingHandler.finishCurrentTrack();
                        trackModeListener.onTrackModeChange(BaseMainActivity.TRACK_MODE_SINGLE);
                    }

                    @Override
                    public void negative() {
                    }
                }, mContext);

    }

    private void createStartTrackDialog(final OnTrackModeChangeListener listener) {
        String[] items = new String[]{mContext.getString(R.string.track_mode_single),
                mContext.getString(R.string.track_mode_auto)};
        DialogUtil.createSingleChoiceItemsDialog(
                mContext.getString(R.string.question_track_mode),
                items,
                new DialogCallback() {
                    @Override
                    public void itemSelected(int which) {
                        switch (which) {
                            case 0:
                                listener.onTrackModeChange(BaseMainActivity.TRACK_MODE_SINGLE);
                                mContext.getApplicationContext().startService(
                                        new Intent(mContext, OBDConnectionService.class));
                                break;
                            case 1:
                                //                                listener.onTrackModeChange
                                // (BaseMainActivity.TRACK_MODE_AUTO);
                                //                                mContext.getApplicationContext
                                // ().startService(
                                //                                        new Intent(mContext,
                                // DeviceInRangeService.class));
                                break;
                        }

                        //                        Crouton.makeText(mContext, R.string
                        // .start_connection, Style.INFO).show();
                    }

                    @Override
                    public void cancelled() {

                    }
                }, mContext);
    }

    private void handleServiceStoppedState(NavMenuItem button) {
        switch (trackMode) {
            case BaseMainActivity.TRACK_MODE_SINGLE:
                resetToStartButtonState(button);
                break;
            case BaseMainActivity.TRACK_MODE_AUTO:
                if (deviceDiscoveryActive) {
                    button.setTitle(mContext.getString(R.string.menu_cancel));
                    defineButtonContents(button, true, R.drawable.av_stop, "");
                } else {
                    resetToStartButtonState(button);
                }
                break;
            default:
                break;
        }

    }

    private void handleServiceDeviceDiscoveryPendingState(NavMenuItem button) {
        button.setTitle(mContext.getString(R.string.menu_cancel));
        defineButtonContents(button, true, R.drawable.av_cancel, R.string.device_discovery_pending);
    }

    private void handleServiceStartingState(NavMenuItem button) {
        button.setTitle(mContext.getString(R.string.menu_cancel));
        defineButtonContents(button, true, R.drawable.av_cancel, R.string.menu_starting);
    }

    private void handleServiceStartedState(NavMenuItem button) {
        button.setTitle(mContext.getString(R.string.menu_stop));

        int subtitleRes;
        switch (trackMode) {
            case BaseMainActivity.TRACK_MODE_SINGLE:
                subtitleRes = R.string.track_mode_single;
                break;
            case BaseMainActivity.TRACK_MODE_AUTO:
                subtitleRes = R.string.track_mode_auto;
                break;
            default:
                subtitleRes = R.string.track_mode_single;
                break;
        }

        defineButtonContents(button, true, R.drawable.av_stop, subtitleRes);
    }

    private void resetToStartButtonState(NavMenuItem button) {
        button.setTitle(mContext.getString(R.string.menu_start));
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(mContext);

        String remoteDevice = preferences.getString(PreferenceConstants
                        .PREF_BLUETOOTH_LIST, null);

        if (remoteDevice != null) {
            if (mCarManager.getCar() == null) {
                defineButtonContents(button, false, R.drawable.not_available, R.string
                        .no_sensor_selected);
            } else {
                defineButtonContents(button, true, R.drawable.av_play, preferences.getString
                        (PreferenceConstants.PREF_BLUETOOTH_NAME, ""));
            }
        } else {
            defineButtonContents(button, false, R.drawable.not_available, R.string
                    .pref_bluetooth_select_adapter_summary);
        }
    }


    private void processStoppedStateClick(
            OnTrackModeChangeListener onTrackModeChangeListener) {
        createStartTrackDialog(onTrackModeChangeListener);
    }

    //    private void processPendingStateClick() {
    //        /*
    //         * this broadcast stops the DeviceInRangeService
    //		 */
    //        mContext.getApplicationContext().stopService(
    //                new Intent(mContext.getApplicationContext(), DeviceInRangeService.class));
    //    }

    private void processStartingStateClick() {
        mContext.getApplicationContext().stopService(
                new Intent(mContext, OBDConnectionService.class));
        //        Crouton.makeText(mContext, R.string.stop_connection, Style.INFO).show();
    }

    private void processStartedStateClick(OnTrackModeChangeListener l) {
        createStopTrackDialog(l);
    }


    /*
     * CALLBACK INTERFACES
     */
    public interface OnTrackModeChangeListener {

        /**
         * called when the mode changes on user input or
         * a certain state change.
         *
         * @param trackModeSingle the track mode
         */
        void onTrackModeChange(int trackModeSingle);

    }

}
