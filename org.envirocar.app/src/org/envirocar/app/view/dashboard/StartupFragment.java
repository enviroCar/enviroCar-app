package org.envirocar.app.view.dashboard;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.TrackHandler;
import org.envirocar.app.application.CarPreferenceHandler;
import org.envirocar.app.bluetooth.BluetoothHandler;
import org.envirocar.app.bluetooth.service.BluetoothServiceState;
import org.envirocar.app.events.bluetooth.BluetoothServiceStateChangedEvent;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.services.OBDConnectionService;
import org.envirocar.app.services.ServiceUtils;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

/**
 * @author dewall
 */
public class StartupFragment extends BaseInjectorFragment {
    private static final Logger LOGGER = Logger.getLogger(StartupFragment.class);
    private OBDConnectionService mOBDConnectionService;
    private boolean mIsOBDConnectionBounded;

    @Inject
    protected BluetoothHandler mBluetoothHandler;
    @Inject
    protected CarPreferenceHandler mCarManager;
    @Inject
    protected TrackHandler mTrackHandler;
    @InjectView(R.id.fragment_startup_start_button)
    protected View mStartStopButton;


    // Defines callbacks for service binding, passed to bindService()
    private ServiceConnection mOBDConnectionServiceCon = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LOGGER.info("onServiceConnected(): Bound service connected.");
            // successfully bounded to the service, cast the binder interface to
            // get the service.
            Snackbar.make(mStartStopButton, "Connected", Snackbar.LENGTH_LONG).show();
            OBDConnectionService.OBDConnectionBinder binder = (OBDConnectionService
                    .OBDConnectionBinder) service;
            mOBDConnectionService = binder.getService();
            mIsOBDConnectionBounded = true;
            Toast.makeText(getActivity(), "CONNECTED", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LOGGER.info("onServiceDisconnected(): Bound service disconnected.");
            // Service has been disconnected.
            mOBDConnectionService = null;
            mIsOBDConnectionBounded = false;
        }
    };


    @InjectView(R.id.fragment_startup_start_button_inner)
    protected TextView mStartStopButtonInner;


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
        LOGGER.info("onCreateView()");

        // This setting is an essential requirement to catch the events of a sub-fragment's
        // options shown in the toolbar.
        setHasOptionsMenu(true);

        // First inflate the general dashboard view.
        View contentView = inflater.inflate(R.layout.fragment_startup, container, false);

        // Inject all dashboard-related views.
        ButterKnife.inject(this, contentView);

        // Get the settings fragment and the header fragment.
        mDashboardSettingsFragment = getFragmentManager()
                .findFragmentById(R.id.fragment_startup_settings_layout);
        mDashboardHeaderFragment = getFragmentManager()
                .findFragmentById(R.id.fragment_dashboard_header_fragment);

        // TODO fix this. The static service state is just a workaround.
        onShowServiceStateUI(OBDConnectionService.CURRENT_SERVICE_STATE);

        return contentView;
    }

    @Override
    public void onStart() {
        super.onStart();
        // If the service is running, the
        Toast.makeText(getActivity(), "check Running", Toast.LENGTH_SHORT).show();
        bindService();
    }

    @Override
    public void onResume() {
        super.onResume();
        //        getFragmentManager().beginTransaction()
        //                .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom)
        //                .show(mDashboardSettingsFragment)
        //                .commit();
    }


    @Override
    public void onDestroyView() {
        if (!getActivity().isFinishing() && mDashboardSettingsFragment != null) {
            getFragmentManager().beginTransaction()
                    .remove(mDashboardSettingsFragment)
                    .remove(mDashboardHeaderFragment)
                    .commit();
        }
        super.onDestroyView();
    }

    @Override
    public void onStop() {
        super.onStop();
        unbindService();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_dashboard_tempomat_map:
                if (mDashboardTrackMapFragment.isVisible())
                    return false;

                Toast.makeText(getActivity(), "map clicked!", Toast.LENGTH_LONG).show();
                replaceFragment(mDashboardTrackMapFragment,
                        R.id.fragment_startup_container,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right);
                return true;
            case R.id.menu_dashboard_tempomat_show_cruise:
                if(mDashboardTempomatFragment.isVisible())
                    return false;

                Toast.makeText(getActivity(), "tempomat clicked!", Toast.LENGTH_LONG).show();
                replaceFragment(mDashboardTempomatFragment,
                        R.id.fragment_startup_container,
                        R.anim.slide_in_right,
                        R.anim.slide_out_left);
                return true;
        }
        return false;
    }

    @OnClick(R.id.fragment_startup_start_button)
    public void onStartStopButtonClicked() {
        // TODO
        //        Intent intent = new Intent(SystemStartupService.ACTION_START_TRACK_RECORDING);
        //        getActivity().sendBroadcast(intent);

        switch (OBDConnectionService.CURRENT_SERVICE_STATE) {
            case SERVICE_STOPPED:
                getActivity().startService(
                        new Intent(getActivity(), OBDConnectionService.class));
                break;
            case SERVICE_STARTED:
                mTrackHandler.finishCurrentTrack();
                break;
            default:
                break;
        }

    }

    private void onButtonStopClicked() {
        new MaterialDialog.Builder(getActivity())
                .title("Finish Track?")
                .content("Do you really want to finish the track?")
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
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
        LOGGER.info(String.format("onReceiveBluetoothServiceStateChangedEvent(): %s",
                event.toString()));
        mServiceState = event.mState;
        onShowServiceStateUI(event.mState);
    }

    private void onShowServiceStateUI(BluetoothServiceState state) {
        switch (state) {
            case SERVICE_STOPPED:
                // Show the settings Fragment
                //                showFragment(mDashboardSettingsFragment,
                //                        R.anim.slide_out_bottom,
                //                        R.anim.slide_out_bottom);
                showFragment(mDashboardSettingsFragment, R.anim.slide_in_left,
                        R.anim.slide_out_left);

                // Hide the header fragment
                hideFragment(mDashboardHeaderFragment,
                        mCurrentlyVisible != null ? R.anim.slide_in_top : -1,
                        mCurrentlyVisible != null ? R.anim.slide_out_top : -1);

                // Replace the container with the mapview.
                if (mCurrentlyVisible != mDashboardMapFragment)
                    replaceFragment(mDashboardMapFragment, R.id.fragment_startup_container,
                            mCurrentlyVisible != null ? R.anim.slide_in_left : -1,
                            mCurrentlyVisible != null ? R.anim.slide_out_right : -1);

                mCurrentlyVisible = mDashboardMapFragment;

                // Update the StartStopButton
                updateStartStopButton(getResources().getColor(R.color.green_dark_cario),
                        "START TRACK", true);
                break;
            case SERVICE_DEVICE_DISCOVERY_PENDING:
                break;
            case SERVICE_DEVICE_DISCOVERY_RUNNING:
                break;
            case SERVICE_STARTING:
                updateStartStopButton(Color.GRAY, "TRACK IS STARTING...", false);
                break;
            case SERVICE_STARTED:
                // Hide the settings if visible
                if (mDashboardSettingsFragment != null && mDashboardSettingsFragment.isVisible()) {
                    hideFragment(mDashboardSettingsFragment,
                            R.anim.slide_in_left,
                            R.anim.slide_out_left);
                }

                showFragment(mDashboardHeaderFragment,
                        R.anim.slide_in_top,
                        R.anim.slide_out_top);

                // Show the tempomat fragment
                replaceFragment(mDashboardTempomatFragment,
                        R.id.fragment_startup_container,
                        R.anim.slide_in_right,
                        R.anim.slide_out_left);

                mCurrentlyVisible = mDashboardTempomatFragment;

                // Update the StartStopButton
                updateStartStopButton(Color.RED, "STOP TRACK", true);
                break;
            case SERVICE_STOPPING:
                updateStartStopButton(Color.GRAY, "TRACK IS STOPPING...", false);
                break;
        }
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
        if (fragment == null)
            return;

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if (enterAnimation != -1 && exitAnimation != -1) {
            transaction.setCustomAnimations(enterAnimation, exitAnimation);
        }
        transaction.replace(container, fragment);
        transaction.commit();

        mCurrentlyVisible = fragment;
    }

    /**
     * @param fragment
     * @param enterAnimation
     * @param exitAnimation
     */
    private void showFragment(Fragment fragment, int enterAnimation, int exitAnimation) {
        if (fragment == null)
            return;

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if (enterAnimation != -1) {
            transaction.setCustomAnimations(enterAnimation, exitAnimation);
        }
        transaction.show(fragment);
        transaction.commit();
    }

    /**
     * @param fragment
     * @param enterAnimation
     * @param exitAnimation
     */
    private void hideFragment(Fragment fragment, int enterAnimation, int exitAnimation) {
        if (fragment == null)
            return;

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if (exitAnimation != -1) {
            transaction.setCustomAnimations(enterAnimation, exitAnimation);
        }
        transaction.hide(fragment);
        transaction.commit();
    }

    @UiThread
    private void onServiceStarting() {
        bindService();
    }

    @UiThread
    private void onServiceStarted() {
        // Hide the dashboard settings fragment if visible.
        if (mDashboardSettingsFragment.isVisible()) {
            getFragmentManager()
                    .beginTransaction()
                    .hide(mDashboardSettingsFragment)
                    .commit();
        }

        getFragmentManager().beginTransaction().show(mDashboardHeaderFragment).commit();

        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_startup_container, new DashboardTempomatFragment())
                .commit();

        updateStartToStopButton();
    }

    @UiThread
    private void onServiceStopping() {
        unbindService();
    }

    @UiThread
    private void onServiceStopped() {
        getFragmentManager().beginTransaction()
                .hide(mDashboardTempomatFragment)
                .hide(mDashboardHeaderFragment)
                .replace(R.id.fragment_startup_container, mDashboardMapFragment)
                .commit();

        if (mDashboardSettingsFragment != null) {
            getFragmentManager()
                    .beginTransaction()
                    .show(mDashboardSettingsFragment)
                    .commit();
        }

        updateStartToStopButton();
    }

    /**
     * Creates a binding for the {@link OBDConnectionService}.
     */
    private void bindService() {
        // if the service is currently running, then bind to the service.
        if (ServiceUtils.isServiceRunning(getActivity(), OBDConnectionService.class)) {
            Toast.makeText(getActivity(), "is Running", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), OBDConnectionService.class);
            getActivity().bindService(intent, mOBDConnectionServiceCon, Context.BIND_AUTO_CREATE);
        }
    }

    private void unbindService() {
        // If it is bounded, then unbind the service.
        if (mIsOBDConnectionBounded) {
            LOGGER.info("onStop(): disconnect bound service");
            getActivity().unbindService(mOBDConnectionServiceCon);
            mIsOBDConnectionBounded = false;
        }
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
