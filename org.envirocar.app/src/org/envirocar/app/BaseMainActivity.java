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
package org.envirocar.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.otto.Subscribe;

import org.envirocar.app.activity.DialogUtil;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.handler.PreferenceConstants;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.handler.TemporaryFileManager;
import org.envirocar.app.handler.UserHandler;
import org.envirocar.app.services.OBDConnectionService;
import org.envirocar.app.services.SystemStartupService;
import org.envirocar.app.view.HelpActivity;
import org.envirocar.app.view.LoginActivity;
import org.envirocar.app.view.SendLogFileFragment;
import org.envirocar.app.view.TroubleshootingFragment;
import org.envirocar.app.view.dashboard.DashboardMainFragment;
import org.envirocar.app.view.logbook.LogbookActivity;
import org.envirocar.app.view.settings.SettingsActivity;
import org.envirocar.app.view.tracklist.TrackListPagerFragment;
import org.envirocar.core.entity.Announcement;
import org.envirocar.core.entity.User;
import org.envirocar.core.events.NewUserSettingsEvent;
import org.envirocar.core.events.TrackFinishedEvent;
import org.envirocar.core.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.injection.BaseInjectorActivity;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.Util;
import org.envirocar.core.util.VersionRange;
import org.envirocar.obd.events.BluetoothServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;
import org.envirocar.remote.DAOProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Main UI application that cares about the auto-upload, auto-connect and global
 * UI elements
 *
 * @author dewall
 */
public class BaseMainActivity extends BaseInjectorActivity {
    private static final Logger LOGGER = Logger.getLogger(BaseMainActivity.class);

    public static final int TRACK_MODE_SINGLE = 0;
    public static final int TRACK_MODE_AUTO = 1;

    private static final String TRACK_MODE = "trackMode";
    private static final String SEEN_ANNOUNCEMENTS = "seenAnnouncements";
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


    @InjectView(R.id.main_layout_toolbar)
    protected Toolbar mToolbar;
    @InjectView(R.id.drawer_layout)
    protected DrawerLayout mDrawerLayout;
    @InjectView(R.id.nav_drawer_navigation_view)
    protected NavigationView mNavigationView;

    @InjectView(R.id.nav_drawer_list_header_layout)
    protected View mHeaderLayout;
    @InjectView(R.id.nav_drawer_list_header_username)
    protected TextView mUsernameText;
    @InjectView(R.id.nav_drawer_list_header_email)
    protected TextView mEmailText;


    private int trackMode = TRACK_MODE_SINGLE;
    private Set<String> seenAnnouncements = new HashSet<String>();
    private BroadcastReceiver errorInformationReceiver;

    private int selectedMenuItemID;


    private boolean paused;
    private ActionBarDrawerToggle mDrawerToggle;
    private BluetoothServiceState mServiceState = BluetoothServiceState.SERVICE_STOPPED;
    private Fragment mCurrentFragment;
    private Fragment mStartupFragment;

    private CompositeSubscription subscriptions = new CompositeSubscription();

    private Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /**
         * try-catch: very dirty hack for broken fragmentmanager impl on some (one?) device
         */
        boolean noInstantiatedExceptionReceived = false;
        try {
            super.onCreate(savedInstanceState);
        } catch (IllegalStateException e) {
            LOGGER.warn("Trying to reconstruct fragment state. Got Exception", e);
            if (e.getMessage().contains("No instantiated fragment for index #")) {
                noInstantiatedExceptionReceived = true;
            }
        }

        // Set the content view of the application
        setContentView(R.layout.main_layout);
        mNavigationView = (NavigationView) findViewById(R.id.nav_drawer_navigation_view);
        LayoutInflater.from(this).inflate(R.layout.nav_drawer_list_header, mNavigationView);
        ButterKnife.inject(this);

        // Initializes the Toolbar.
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.menu_nav_drawer_dashboard);

        if (noInstantiatedExceptionReceived) {
            TrackListPagerFragment pagerFragment = new TrackListPagerFragment();
            if (mNavigationView != null && mNavigationView.getMenu() != null) {
                MenuItem menuItem = mNavigationView.getMenu().findItem(R.id
                        .menu_nav_drawer_tracklist_new);
                transitToFragment(menuItem, pagerFragment);
            } else {
                LOGGER.warn("Could not re-create TrackListPagerFragment: mNavigationView=" +
                        mNavigationView);
            }
        }

        // Register a listener for a menu item that gets selected.
        mNavigationView.setNavigationItemSelectedListener(menuItem -> {

            if (selectDrawerItem(menuItem)) {
                // we want to have shared checked states between different groups. Therefore, we
                // cannot use the provided "single" checkedBehavior. For this reason, it is first
                // required to make the item checkable first.
                menuItem.setCheckable(true);
                menuItem.setChecked(true);

                // Uncheck all other items.
                Menu m = mNavigationView.getMenu();
                for (int i = 0; i < m.size(); i++) {
                    MenuItem mi = m.getItem(i);
                    if (!(mi.getItemId() == menuItem.getItemId()))
                        mi.setChecked(false);
                }
            }

            // Close the navigation drawer.
            mDrawerLayout.closeDrawers();
            return true;
        });

        // Initializes the navigation drawer.
        initNavigationDrawerLayout();

        // Set the DashboardFragment as initial fragment.
        mStartupFragment = new DashboardMainFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, mStartupFragment, mStartupFragment.getClass()
                        .getSimpleName())
                .commit();

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
                        .replace(R.id.content_frame, fragment)
                        .commit();
            }
        };


        registerReceiver(errorInformationReceiver, new IntentFilter(TroubleshootingFragment
                .INTENT));

        resolvePersistentSeenAnnouncements();
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

    @Subscribe
    public void onReceiveBluetoothStateChangedEvent(BluetoothStateChangedEvent event) {
        LOGGER.info(String.format("Received event: %s", event.toString()));
        updateStartStopButton();
    }

    @Subscribe
    public void onReceiveBluetoothServiceStateChangedEvent(
            BluetoothServiceStateChangedEvent event) {
        LOGGER.info(String.format("Received event: %s", event.toString()));
        this.mServiceState = event.mState;
        mMainThreadWorker.schedule(() -> updateStartStopButton());
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    protected void onStart() {
        LOGGER.info("onStart()");
        super.onStart();
        // Update the header.
        updateNavDrawerAccountHeader();
    }

    @Override
    protected void onResume() {
        LOGGER.info("onResume()");
        super.onResume();
        // Check whether the screen is required to keep the screen on.
        checkKeepScreenOn();
    }

    @Override
    protected void onPause() {
        LOGGER.info("onResume()");
        super.onPause();

        this.paused = false;

        //first init
        firstInit();

        checkKeepScreenOn();


        //        new AsyncTask<Void, Void, Void>() {
        //            @Override
        //            protected Void doInBackground(Void... params) {
        //                checkAffectingAnnouncements();
        //                return null;
        //            }
        //        }.execute();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        this.unregisterReceiver(errorInformationReceiver);

        mTemporaryFileManager.shutdown();

        if (!subscriptions.isUnsubscribed()) {
            subscriptions.unsubscribe();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Open or close the drawer.
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }

        if (mDrawerToggle.onOptionsItemSelected(item))
            return true;

        return super.onOptionsItemSelected(item);
    }

    private void checkAffectingAnnouncements() {
        final List<Announcement> annos = new ArrayList<>();
        try {
            annos.addAll(mDAOProvider.getAnnouncementsDAO().getAllAnnouncements());
        } catch (DataRetrievalFailureException e) {
            LOGGER.warn(e.getMessage(), e);
            return;
        } catch (NotConnectedException e) {
            e.printStackTrace();
        }

        final VersionRange.Version version;
        try {
            String versionShort = Util.getVersionStringShort(getApplicationContext());
            version = VersionRange.Version.fromString(versionShort);
        } catch (PackageManager.NameNotFoundException e) {
            LOGGER.warn(e.getMessage());
            return;
        }

        runOnUiThread(() -> {
            for (Announcement announcement : annos) {
                if (!seenAnnouncements.contains(announcement.getId())) {
                    if (announcement.getVersionRange().isInRange(version)) {
                        showAnnouncement(announcement);
                    }
                }
            }
        });
    }

    private void showAnnouncement(final Announcement announcement) {
        String priorityi18n;
        if (announcement.getPriority().equals(Announcement.Priority.HIGH)) {
            priorityi18n = this.getString(R.string.category_high);
        } else if (announcement.getPriority().equals(Announcement.Priority.MEDIUM)) {
            priorityi18n = this.getString(R.string.category_normal);
        } else {
            priorityi18n = this.getString(R.string.category_low);
        }
        String title = String.format("[%s] %s %s", priorityi18n, announcement.getPriority(), this
                .getString(R.string
                        .announcement));
        String content = announcement.getContent().getAsString();

        DialogUtil.createTitleMessageInfoDialog(title, Html.fromHtml(content), true, new
                DialogUtil.PositiveNegativeCallback() {
                    @Override
                    public void negative() {
                        seenAnnouncements.add(announcement.getId());
                    }

                    @Override
                    public void positive() {
                        addPersistentSeenAnnouncement(announcement.getId());
                        seenAnnouncements.add(announcement.getId());
                    }
                }, this);
    }

    protected void addPersistentSeenAnnouncement(String id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String currentPersisted = preferences.getString(PreferenceConstants
                .PERSISTENT_SEEN_ANNOUNCEMENTS, "");

        StringBuilder sb = new StringBuilder(currentPersisted);
        if (!currentPersisted.isEmpty()) {
            sb.append(",");
        }
        sb.append(id);

        preferences.edit().putString(
                PreferenceConstants.PERSISTENT_SEEN_ANNOUNCEMENTS,
                sb.toString()).commit();
    }

    // TODO check
    private void firstInit() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.contains("first_init")) {
            //            mDrawerLayout.openDrawer(mDrawerList);

            SharedPreferences.Editor e = preferences.edit();
            e.putString("first_init", "seen");
            e.putBoolean("pref_privacy", true);
            e.commit();
        }
    }

    private void initNavigationDrawerLayout() {
        // Initialize the navigation drawer
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        updateStartStopButton();

        // Initializes the toggle for the navigation drawer.
        mDrawerToggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, mToolbar, R.string.open_drawer,
                R.string.close_drawer) {

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                hideKeyboard(drawerView);
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // Enables the home button.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mHeaderLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BaseMainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }


    /**
     * Called when an item of the navigation drawer has been clicked. This method is responsible
     * to replace the visible fragment with the corresponding fragment of the clicked item.
     *
     * @param menuItem the item clicked in the menu of the navigation drawer.
     */
    private boolean selectDrawerItem(MenuItem menuItem) {
        LOGGER.info(String.format("selectDrawerItem(%s)", menuItem.getTitle()));

        Fragment fragment = null;
        switch (menuItem.getItemId()) {
            case R.id.menu_nav_drawer_dashboard:
                //                fragment = new RealDashboardFragment();
                fragment = new DashboardMainFragment();
                break;
            case R.id.menu_nav_drawer_tracklist_new:
                fragment = new TrackListPagerFragment();
                break;
            case R.id.menu_nav_drawer_logbook:
                Intent intent3 = new Intent(BaseMainActivity.this, LogbookActivity.class);
                startActivity(intent3);
                return false;
            case R.id.menu_nav_drawer_account_login:
                Intent intent = new Intent(BaseMainActivity.this, LoginActivity.class);
                startActivity(intent);
                return false;
            case R.id.menu_nav_drawer_settings_general:
                Intent intent2 = new Intent(BaseMainActivity.this, SettingsActivity.class);
                startActivity(intent2);
                return false;
            case R.id.menu_nav_drawer_settings_help:
                Intent help = new Intent(BaseMainActivity.this, HelpActivity.class);
                startActivity(help);
                return false;
            case R.id.menu_nav_drawer_settings_sendlog:
                fragment = new SendLogFileFragment();
                break;
            case R.id.menu_nav_drawer_quit_app:
                new MaterialDialog.Builder(this)
                        .title(getString(R.string.menu_close_envirocar_title))
                        .positiveText(getString(R.string.menu_close_envirocar_positive))
                        .negativeText(getString(R.string.cancel))
                        .content(getString(R.string.menu_close_envirocar_content))
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                shutdownEnviroCar();
                            }
                        })
                        .show();
                return false;
        }

        // If the fragment is null or the fragment is already visible, then do nothing.
        if (fragment == null || isFragmentVisible(fragment.getClass().getSimpleName()))
            return false;

        //now do the transition
        transitToFragment(menuItem, fragment);

        return true;
    }

    private void transitToFragment(MenuItem menuItem, Fragment fragment) {
        // Insert the fragment by replacing the existent fragment in the content frame.
        replaceFragment(fragment,
                selectedMenuItemID > menuItem.getItemId() ?
                        R.anim.translate_slide_in_left_fragment :
                        R.anim.translate_slide_in_right_fragment,
                selectedMenuItemID > menuItem.getItemId() ?
                        R.anim.translate_slide_out_right_fragment :
                        R.anim.translate_slide_out_left_fragment);
        mCurrentFragment = fragment;

        selectedMenuItemID = menuItem.getItemId();

        /// update the title of the toolbar.
        setTitle(menuItem.getTitle());
    }

    private void shutdownEnviroCar() {
        SystemStartupService.stopService(this);
        OBDConnectionService.stopService(this);

        mMainThreadWorker.schedule(() -> {
            System.runFinalizersOnExit(true);
            System.exit(0);
        }, 750, TimeUnit.MILLISECONDS);
    }

    /**
     * @param fragment
     * @param animIn
     * @param animOut
     */
    private void replaceFragment(Fragment fragment, int animIn, int animOut) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (animIn != -1 && animOut != -1) {
            ft.setCustomAnimations(animIn, animOut);
        }
        ft.replace(R.id.content_frame, fragment, fragment.getClass().getSimpleName());
        //        ft.addToBackStack(null);
        ft.commit();
    }

    @Override
    public List<Object> getInjectionModules() {
        return Arrays.<Object>asList(new MainActivityModule(this));
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

    @Subscribe
    public void onReceiveNewUserSettingsEvent(NewUserSettingsEvent event) {
        LOGGER.info(String.format("onReceiveNewUserSettingsEvent(): event=%s", event.toString()));
        updateNavDrawerAccountHeader();
    }

    private void updateNavDrawerAccountHeader() {
        runOnUiThread(() -> {
            boolean isLoggedIn = mUserManager.isLoggedIn();
            User user = mUserManager.getUser();
            if (isLoggedIn) {
                mUsernameText.setText(user.getUsername());
                mEmailText.setText(user.getMail());
            } else {
                mUsernameText.setText(R.string.menu_not_logged_in);
                mEmailText.setText(R.string.menu_not_logged_in_sub);
            }
        });
    }


    /**
     * This method checks, whether a Fragment with a certain tag is visible.
     *
     * @param tag The tag of the Fragment.
     * @return True if the Fragment is visible, false if not.
     */
    public boolean isFragmentVisible(String tag) {
        Fragment tmpFragment = getSupportFragmentManager().findFragmentByTag(tag);
        if (tmpFragment != null && tmpFragment.isVisible()) {
            LOGGER.info("Fragment with tag: " + tag + " is already visible.");
            return true;
        }
        return false;

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

    private void updateStartStopButton() {
        //        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        //        StartStopButtonUtil startStopUtil = new StartStopButtonUtil(this, trackMode,
        // mServiceState,
        //                mServiceState == BluetoothServiceState.SERVICE_DEVICE_DISCOVERY_PENDING);
        //        if (adapter != null && adapter.isEnabled()) { // was requirementsFulfilled
        //            startStopUtil.updateStartStopButtonOnServiceStateChange
        //                    (navDrawerItems[START_STOP_MEASUREMENT]);
        //        } else {
        //            startStopUtil.defineButtonContents(navDrawerItems[START_STOP_MEASUREMENT],
        //                    false, R.drawable.not_available, getString(R.string
        // .pref_bluetooth_disabled),
        //                    getString(R.string.menu_start));
        //        }
        //
        //        mNavDrawerAdapter.notifyDataSetChanged();
    }


    protected void resolvePersistentSeenAnnouncements() {
        String pers = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(PreferenceConstants.PERSISTENT_SEEN_ANNOUNCEMENTS, "");

        if (!pers.isEmpty()) {
            if (pers.contains(",")) {
                String[] arr = pers.split(",");
                for (String string : arr) {
                    seenAnnouncements.add(string);
                }
            } else {
                seenAnnouncements.add(pers);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(TRACK_MODE, trackMode);
        outState.putSerializable(SEEN_ANNOUNCEMENTS, this.seenAnnouncements.toArray());
    }

    private void showSnackbar(int infoRes) {
        showSnackbar(getString(infoRes));
    }

    private void showSnackbar(String info) {
        mMainThreadWorker.schedule(() -> {
            if (mDrawerLayout != null) {
                Snackbar.make(mDrawerLayout, info, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void readSavedState(Bundle savedInstanceState) {
        if (savedInstanceState == null) return;

        this.trackMode = savedInstanceState.getInt(TRACK_MODE);

        String[] arr = (String[]) savedInstanceState.getSerializable(SEEN_ANNOUNCEMENTS);
        if (arr != null) {
            for (String string : arr) {
                this.seenAnnouncements.add(string);
            }
        }
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

}
