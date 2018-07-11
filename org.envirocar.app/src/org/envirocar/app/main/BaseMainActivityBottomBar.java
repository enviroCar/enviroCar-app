package org.envirocar.app.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.WindowManager;

import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.PreferenceConstants;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.handler.TemporaryFileManager;
import org.envirocar.app.handler.UserHandler;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.services.SystemStartupService;
import org.envirocar.app.views.OthersFragment;
import org.envirocar.app.views.TroubleshootingFragment;
import org.envirocar.app.views.dashboard.DashBoardFragment;
import org.envirocar.app.views.tracklist.TrackListPagerFragment;
import org.envirocar.core.events.TrackFinishedEvent;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.logging.Logger;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

public class BaseMainActivityBottomBar extends BaseInjectorActivity {

    private static final Logger LOGGER = Logger.getLogger(BaseMainActivityBottomBar.class);

    private static final String TROUBLESHOOTING_TAG = "TROUBLESHOOTING";

    // Injected variables
    @Inject
    protected UserHandler mUserManager;
    @Inject
    protected CarPreferenceHandler mCarManager;
    @Inject
    protected TemporaryFileManager mTemporaryFileManager;
    @Inject
    protected DAOProvider mDAOProvider;
    @Inject
    protected BluetoothHandler mBluetoothHandler;
    @Inject
    protected DashBoardFragment mDashBoardFragment;
    @Inject
    protected TrackListPagerFragment mTrackListPagerFragment;
    @Inject
    protected OthersFragment mOthersFragment;

    @BindView(R.id.navigation)
    protected BottomNavigationView navigationBottomBar;

    private int selectedMenuItemID = 0;

    private CompositeSubscription subscriptions = new CompositeSubscription();
    private BroadcastReceiver errorInformationReceiver;
    private boolean paused;


    private Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();



    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = item -> {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                switch (item.getItemId()) {
                    case R.id.navigation_dashboard:
                        if(selectedMenuItemID != 1){
                            fragmentTransaction.replace(R.id.fragmentContainer, mDashBoardFragment);
                            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                            fragmentTransaction.commit();
                            selectedMenuItemID = 1;
                        }
                        return true;
                    case R.id.navigation_my_tracks:
                        if(selectedMenuItemID != 2){
                            fragmentTransaction.replace(R.id.fragmentContainer, new TrackListPagerFragment());
                            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                            fragmentTransaction.commit();
                            selectedMenuItemID = 2;
                        }
                        return true;
                    case R.id.navigation_others:
                        if(selectedMenuItemID != 3){
                            fragmentTransaction.replace(R.id.fragmentContainer, mOthersFragment);
                            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                            fragmentTransaction.commit();
                            selectedMenuItemID = 3;
                        }
                        return true;
                }
                return false;
            };

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        MainActivityComponent mainActivityComponent =  baseApplicationComponent.plus(new MainActivityModule(this));
        mainActivityComponent.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LOGGER.info("BaseMainActivityBottomBar : onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_main_bottom_bar);
        ButterKnife.bind(this);

        navigationBottomBar.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigationBottomBar.setSelectedItemId(R.id.navigation_dashboard);

        // Subscribe for preference subscriptions. In this case, subscribe for changes to the
        // active screen settings.
        // TODO
        addPreferenceSubscriptions();

        errorInformationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (paused) {
                    return;
                }

                Fragment fragment = getSupportFragmentManager().findFragmentByTag
                        (TROUBLESHOOTING_TAG);
                if (fragment == null) {
                    fragment = new TroubleshootingFragment();
                }
                fragment.setArguments(intent.getExtras());
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .commit();
            }
        };


        registerReceiver(errorInformationReceiver, new IntentFilter(TroubleshootingFragment
                .INTENT));
    }

    @Override
    protected void onPause() {
        LOGGER.info("BaseMainActivityBottomBar : onPause");
        super.onPause();
        this.paused = false;

        //first init
        firstInit();

        checkKeepScreenOn();
    }

    @Override
    protected void onResume() {
        LOGGER.info("BaseMainActivityBottomBar : onResume()");
        super.onResume();
        // Check whether the screen is required to keep the screen on.
        checkKeepScreenOn();
    }

    @Override
    protected void onDestroy() {
        LOGGER.info("BaseMainActivityBottomBar : onDestroy()");
        super.onDestroy();

        this.unregisterReceiver(errorInformationReceiver);

        mTemporaryFileManager.shutdown();

        if (!subscriptions.isUnsubscribed()) {
            subscriptions.unsubscribe();
        }
    }

    private void firstInit() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.contains("first_init")) {

            SharedPreferences.Editor e = preferences.edit();
            e.putString("first_init", "seen");
            e.putBoolean("pref_privacy", true);
            e.commit();
        }
    }

    private void addPreferenceSubscriptions() {
        // Keep screen active setting;
        subscriptions.add(
                PreferencesHandler.getDisplayStaysActiveObservable(this)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(aBoolean -> {
                            checkKeepScreenOn();
                        })
        );

        // Start Background handler
        subscriptions.add(
                PreferencesHandler.getBackgroundHandlerEnabledObservable(this)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(aBoolean -> {
                            if (aBoolean) {
                                SystemStartupService.startService(this);
                            } else {
                                SystemStartupService.stopService(this);
                            }
                        })
        );
    }

    private void checkKeepScreenOn() {
        if (PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean(PreferenceConstants.DISPLAY_STAYS_ACTIV, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Subscribe
    public void onReceiveTrackFinishedEvent(final TrackFinishedEvent event) {
        LOGGER.info(String.format("onReceiveTrackFinishedEvent(): event=%s", event.toString()));

        // Just show a message depending on the event-related track.
        mMainThreadWorker.schedule(() -> {
            if (event.mTrack == null) {
                // Track is null and thus there was an error.
                showSnackbar(R.string.track_finishing_failed);
            } else try {
                if (event.mTrack.getLastMeasurement() != null) {
                    LOGGER.info("last is not null.. " + event.mTrack.getLastMeasurement()
                            .toString());

                    // Track has no measurements
                    showSnackbar(getString(R.string.track_finished).concat(event.mTrack.getName()));
                }
            } catch (NoMeasurementsException e) {
                LOGGER.warn("Track has been finished without measurements", e);
                // Track has no measurements
                showSnackbar(R.string.track_finished_no_measurements);
            }
        });
    }

    private void showSnackbar(int infoRes) {
        showSnackbar(getString(infoRes));
    }

    private void showSnackbar(String info) {
        Snackbar.make(navigationBottomBar, info, Snackbar.LENGTH_LONG).show();
    }


}
