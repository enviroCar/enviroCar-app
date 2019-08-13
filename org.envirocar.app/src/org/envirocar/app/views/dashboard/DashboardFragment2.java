package org.envirocar.app.views.dashboard;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.snackbar.Snackbar;
import com.jakewharton.rxbinding.support.v7.widget.RxToolbar;
import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.handler.UserHandler;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.views.carselection.CarSelectionActivity;
import org.envirocar.app.views.login.SigninActivity;
import org.envirocar.app.views.obdselection.OBDSelectionActivity;
import org.envirocar.core.entity.User;
import org.envirocar.core.events.NewCarTypeSelectedEvent;
import org.envirocar.core.events.NewUserSettingsEvent;
import org.envirocar.core.events.bluetooth.BluetoothDeviceSelectedEvent;
import org.envirocar.core.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.core.events.gps.GpsStateChangedEvent;
import org.envirocar.core.logging.Logger;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscriber;

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
    protected View userTracksTextView;
    @BindView(R.id.fragment_dashboard_user_distance_layout)
    protected View userDistanceLayout;
    @BindView(R.id.fragment_dashboard_user_distance_textview)
    protected View userDistanceTextView;
    @BindView(R.id.fragment_dashboard_user_duration_layout)
    protected View userDurationLayout;
    @BindView(R.id.fragment_dashboard_user_duration_textview)
    protected View userDurationTextView;

    @BindView(R.id.fragment_dashboard_indicator_bluetooth)
    protected ImageView bluetoothIndicator;
    @BindView(R.id.fragment_dashboard_indicator_obd)
    protected ImageView obdIndicator;
    @BindView(R.id.fragment_dashboard_indicator_gps)
    protected ImageView gpsIndicator;
    @BindView(R.id.fragment_dashboard_indicator_car)
    protected ImageView carIndicator;

    @BindView(R.id.fragment_dashboard_obdselection_text_primary)
    protected TextView bluetoothSelectionTextPrimary;
    @BindView(R.id.fragment_dashboard_obdselection_text_secondary)
    protected TextView bluetoothSelectionTextSecondary;
    @BindView(R.id.fragment_dashboard_carselection_text_primary)
    protected TextView carSelectionTextPrimary;
    @BindView(R.id.fragment_dashboard_carselection_text_secondary)
    protected TextView carSelectionTextSecondary;

    @BindView(R.id.fragment_dashboard_start_track_button)
    protected View startTrackButton;


    @Inject
    protected UserHandler userHandler;

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // for the login/register button
        setHasOptionsMenu(true);
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

        return contentView;
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
                    .onPositive((dialog, which) -> userHandler.logOutObservable().subscribe(onLogoutSubscriber()))
                    .show();
        }
    }

    private Subscriber<Boolean> onLogoutSubscriber() {
        return new Subscriber<Boolean>() {
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
            public void onNext(Boolean aBoolean) {
                // nothing to do
            }

            @Override
            public void onCompleted() {
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
        // TODO
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

    /**
     * Receiver method for bluetooth activation events.
     *
     * @param event
     */
    @Subscribe
    public void receiveBluetoothStateChanged(BluetoothStateChangedEvent event) {
        // post on decor view to ensure that it gets executed when view has been inflated.
        getActivity().getWindow().getDecorView().post(() -> {
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
        // post on decor view to ensure that it gets executed when view has been inflated.
        getActivity().getWindow().getDecorView().post(() -> {
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
        getActivity().getWindow().getDecorView().post(() -> {
            updateOBDState(event.mDevice);
        });
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

    /**
     * Receiver method for GPS activation events.
     *
     * @param event
     */
    @Subscribe
    public void onGpsStateChangedEvent(GpsStateChangedEvent event) {
        // post on decor view to ensure that it gets executed when view has been inflated.
        getActivity().getWindow().getDecorView().post(() -> {
            this.gpsIndicator.setEnabled(!event.mIsGPSEnabled);
            this.updateStartTrackButton();
        });
    }

    @Subscribe
    public void onNewUserSettingsEvent(NewUserSettingsEvent event) {
        getActivity().getWindow().getDecorView().post(() -> {
            if (event.mIsLoggedIn) {
                this.loggedInLayout.setVisibility(View.VISIBLE);
                this.toolbar.getMenu().clear();
                this.toolbar.inflateMenu(R.menu.menu_dashboard_logged_in);
                this.textView.setText(event.mUser.getUsername());
            } else {
                this.loggedInLayout.setVisibility(View.GONE);
                this.toolbar.getMenu().clear();
                this.toolbar.inflateMenu(R.menu.menu_dashboard_logged_out);
            }
        });
    }

    private void updateStartTrackButton() {
        boolean setEnabled = (!this.bluetoothIndicator.isEnabled() && !this.gpsIndicator.isEnabled() &&
                !this.obdIndicator.isEnabled() && !this.carIndicator.isEnabled());
        this.startTrackButton.setEnabled(setEnabled);
    }

}
