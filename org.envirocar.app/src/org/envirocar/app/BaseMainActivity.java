package org.envirocar.app;

import android.app.Fragment;
import android.app.FragmentTransaction;
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
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.envirocar.app.activity.DialogUtil;
import org.envirocar.app.activity.HelpFragment;
import org.envirocar.app.activity.ListTracksFragment;
import org.envirocar.app.activity.LogbookFragment;
import org.envirocar.app.activity.SendLogFileFragment;
import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.activity.TroubleshootingFragment;
import org.envirocar.app.application.CarPreferenceHandler;
import org.envirocar.app.application.TemporaryFileManager;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.bluetooth.BluetoothHandler;
import org.envirocar.app.bluetooth.service.BluetoothServiceState;
import org.envirocar.app.events.NewUserSettingsEvent;
import org.envirocar.app.events.bluetooth.BluetoothServiceStateChangedEvent;
import org.envirocar.app.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.injection.module.InjectionActivityModule;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Announcement;
import org.envirocar.app.model.User;
import org.envirocar.app.model.dao.DAOProvider;
import org.envirocar.app.model.dao.exception.AnnouncementsRetrievalException;
import org.envirocar.app.util.Util;
import org.envirocar.app.util.VersionRange;
import org.envirocar.app.view.LoginFragment;
import org.envirocar.app.view.SettingsFragment;
import org.envirocar.app.view.dashboard.DashboardMainFragment;
import org.envirocar.app.view.preferences.PreferenceConstants;
import org.envirocar.app.view.tracklist.NewListFragment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import rx.Scheduler;
import rx.Subscription;
import rx.android.content.ContentObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Main UI application that cares about the auto-upload, auto-connect and global
 * UI elements
 *
 * @author dewall
 */
public class BaseMainActivity extends BaseInjectorActivity {
    private static final Logger LOGGER = Logger.getLogger(BaseApplication.class);

    public static final int TRACK_MODE_SINGLE = 0;
    public static final int TRACK_MODE_AUTO = 1;
    public static final int REQUEST_MY_GARAGE = 1336;
    public static final int REQUEST_REDIRECT_TO_GARAGE = 1337;
    private static final String TAG = BaseMainActivity.class.getSimpleName();


    private static final String TRACK_MODE = "trackMode";
    private static final String SEEN_ANNOUNCEMENTS = "seenAnnouncements";

    private static final String DASHBOARD_TAG = "DASHBOARD";
    private static final String LOGIN_TAG = "LOGIN";
    private static final String MY_TRACKS_TAG = "MY_TRACKS";
    private static final String HELP_TAG = "HELP";
    private static final String TROUBLESHOOTING_TAG = "TROUBLESHOOTING";
    private static final String SEND_LOG_TAG = "SEND_LOG";
    private static final String LOGBOOK_TAG = "LOGBOOK";
    private static final String SETTINGS_TAG = "SETTINGS";

    // Injected variables
    @Inject
    protected UserManager mUserManager;
    @Inject
    protected CarPreferenceHandler mCarManager;
    @Inject
    protected TemporaryFileManager mTemporaryFileManager;
    @Inject
    protected TrackHandler mTrackHandler;
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


    @InjectView(R.id.nav_drawer_list_header_username)
    protected TextView mUsernameText;
    @InjectView(R.id.nav_drawer_list_header_email)
    protected TextView mEmailText;


    private int trackMode = TRACK_MODE_SINGLE;
    private Set<String> seenAnnouncements = new HashSet<String>();
    private Runnable remainingTimeThread;
    private BroadcastReceiver errorInformationReceiver;

    private int selectedMenuItemID;


    private boolean paused;
    private ActionBarDrawerToggle mDrawerToggle;
    private Subscription mPreferenceSubscription;
    private BluetoothServiceState mServiceState = BluetoothServiceState.SERVICE_STOPPED;
    private Fragment mCurrentFragment;
    private Fragment mStartupFragment;

    private Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the content view of the application
        setContentView(R.layout.main_layout);
        ButterKnife.inject(this);

        checkKeepScreenOn();

        // Initializes the Toolbar.
        setSupportActionBar(mToolbar);

        // Register a listener for a menu item that gets selected.
        mNavigationView.setNavigationItemSelectedListener(menuItem -> {

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

            selectDrawerItem(menuItem);

            // Close the navigation drawer.
            mDrawerLayout.closeDrawers();
            return true;
        });


        // Initializes the navigation drawer.
        initNavigationDrawerLayout();

        // Set the DashboardFragment as initial fragment.
        mStartupFragment = new DashboardMainFragment();
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, mStartupFragment, mStartupFragment.getClass()
                        .getSimpleName())
                .commit();


        // Subscribe for specific StartStop button related preferences.
        mPreferenceSubscription = ContentObservable
                .fromSharedPreferencesChanges(PreferenceManager.getDefaultSharedPreferences(this))
                .observeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .filter(prefKey ->
                        PreferenceConstants.PREFERENCE_TAG_BLUETOOTH_NAME.equals(prefKey) ||
                                PreferenceConstants.PREFERENCE_TAG_CAR.equals(prefKey) ||
                                PreferenceConstants.CAR_HASH_CODE.equals(prefKey))
                .subscribe(prefKey -> {
                    // TODO
                    //                    updateStartStopButton();
                });


        errorInformationReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (paused) {
                    return;
                }

                Fragment fragment = getFragmentManager().findFragmentByTag(TROUBLESHOOTING_TAG);
                if (fragment == null) {
                    fragment = new TroubleshootingFragment();
                }
                fragment.setArguments(intent.getExtras());
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, fragment)
                        .commit();
            }
        };

        registerReceiver(errorInformationReceiver, new IntentFilter(TroubleshootingFragment
                .INTENT));

        resolvePersistentSeenAnnouncements();
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
    protected void onPause() {
        super.onPause();

        this.paused = false;

        //        mDrawerLayout.closeDrawer(mDrawerList);
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

    private void checkAffectingAnnouncements() {
        final List<Announcement> annos;
        try {
            annos = mDAOProvider.getAnnouncementsDAO().getAllAnnouncements();
        } catch (AnnouncementsRetrievalException e) {
            LOGGER.warn(e.getMessage(), e);
            return;
        }

        final VersionRange.Version version;
        try {
            String versionShort = Util.getVersionStringShort(getApplicationContext());
            version = VersionRange.Version.fromString(versionShort);
        } catch (PackageManager.NameNotFoundException e) {
            LOGGER.warn(e.getMessage());
            return;
        }

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                for (Announcement announcement : annos) {
                    if (!seenAnnouncements.contains(announcement.getId())) {
                        if (announcement.getVersionRange().isInRange(version)) {
                            showAnnouncement(announcement);
                        }
                    }
                }
            }
        });
    }

    private void showAnnouncement(final Announcement announcement) {
        String title = announcement.createUITitle(this);
        String content = announcement.getContent();

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
        String currentPersisted = preferences.getString(SettingsActivity
                .PERSISTENT_SEEN_ANNOUNCEMENTS, "");

        StringBuilder sb = new StringBuilder(currentPersisted);
        if (!currentPersisted.isEmpty()) {
            sb.append(",");
        }
        sb.append(id);

        preferences.edit().putString(SettingsActivity.PERSISTENT_SEEN_ANNOUNCEMENTS, sb.toString
                ()).commit();
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
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        User user = mUserManager.getUser();
        if (user != null && user.getUsername() != null) {
            mUsernameText.setText(user.getUsername());
            mEmailText.setText(user.getMail());
        }

        // Enables the home button.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Crouton.cancelAllCroutons();

        this.unregisterReceiver(errorInformationReceiver);

        //        if (remainingTimeHandler != null) {
        //            remainingTimeHandler.removeCallbacks(remainingTimeThread);
        //            discoveryTargetTime = 0;
        //            remainingTimeThread = null;
        //        }

        mTemporaryFileManager.shutdown();

        // Unsubscribe all subscriptions.
        mPreferenceSubscription.unsubscribe();
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

    /**
     * Called when an item of the navigation drawer has been clicked. This method is responsible
     * to replace the visible fragment with the corresponding fragment of the clicked item.
     *
     * @param menuItem the item clicked in the menu of the navigation drawer.
     */
    private void selectDrawerItem(MenuItem menuItem) {
        LOGGER.info(String.format("selectDrawerItem(%s)", menuItem.getTitle()));

        Fragment fragment = null;
        switch (menuItem.getItemId()) {
            case R.id.menu_nav_drawer_dashboard:
                //                fragment = new RealDashboardFragment();
                fragment = new DashboardMainFragment();
                break;
            case R.id.menu_nav_drawer_tracklist_new:
                fragment = new NewListFragment();
                break;
            case R.id.menu_nav_drawer_tracklist:
                fragment = new ListTracksFragment();
                break;
            case R.id.menu_nav_drawer_logbook:
                fragment = new LogbookFragment();
                break;
            case R.id.menu_nav_drawer_account_login:
                fragment = new LoginFragment();
                break;
            case R.id.menu_nav_drawer_settings_general:
                fragment = new SettingsFragment();
                break;
            case R.id.menu_nav_drawer_settings_help:
                fragment = new HelpFragment();
                break;
            case R.id.menu_nav_drawer_settings_sendlog:
                fragment = new SendLogFileFragment();
                break;
        }

        // If the fragment is null or the fragment is already visible, then do nothing.
        if (fragment == null || isFragmentVisible(fragment.getClass().getSimpleName()))
            return;

        // Insert the fragment by replacing the existent fragment in the content frame.

        replaceFragment(fragment,
                selectedMenuItemID > menuItem.getItemId() ?
                        R.anim.slide_in_left : R.anim.slide_in_right,
                selectedMenuItemID > menuItem.getItemId() ?
                        R.anim.slide_out_right : R.anim.slide_out_left);
        mCurrentFragment = fragment;

        selectedMenuItemID = menuItem.getItemId();

        /// update the title of the toolbar.
        setTitle(menuItem.getTitle());
    }

    /**
     * @param fragment
     * @param animIn
     * @param animOut
     */
    private void replaceFragment(Fragment fragment, int animIn, int animOut) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (animIn != -1 && animOut != -1) {
            ft.setCustomAnimations(animIn, animOut);
        }
        ft.replace(R.id.content_frame, fragment, fragment.getClass().getSimpleName());
        ft.addToBackStack(null);
        ft.commit();
    }

    @Override
    public List<Object> getInjectionModules() {
        return Arrays.<Object>asList(new InjectionActivityModule(this));
    }

    @Subscribe
    public void onReceiveTrackFinishedEvent(final TrackFinishedEvent event) {
        LOGGER.info(String.format("onReceiveTrackFinishedEvent(): event=%s", event.toString()));

        // Just show a message depending on the event-related track.
        mMainThreadWorker.schedule(() -> {
            if (event.mTrack == null) {
                // Track is null and thus there was an error.
                Crouton.makeText(this, R.string.track_finishing_failed, Style.ALERT).show();
            } else if (event.mTrack.getLastMeasurement() == null) {
                // Track has no measurements
                Crouton.makeText(this, R.string.track_finished_no_measurements, Style.ALERT).show();
            } else {
                LOGGER.info("last is not null.. " + event.mTrack.getLastMeasurement().toString());
                // Track has no measurements
                Crouton.makeText(this,
                        getString(R.string.track_finished).concat(event.mTrack.getName()),
                        Style.INFO).show();
            }
        });
    }

    @Subscribe
    public void onReceiveNewUserSettingsEvent(NewUserSettingsEvent event) {
        LOGGER.info(String.format("onReceiveNewUserSettingsEvent(): event=%s", event.toString()));
        runOnUiThread(() -> {
            if (event.mIsLoggedIn && event.mUser != null) {
                mUsernameText.setText(event.mUser.getUsername());
                mEmailText.setText(event.mUser.getMail());
            } else {
                mUsernameText.setText("Not Logged In");
                mEmailText.setText(" ");
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
        Fragment tmpFragment = getFragmentManager().findFragmentByTag(tag);
        if (tmpFragment != null && tmpFragment.isVisible()) {
            LOGGER.info("Fragment with tag: " + tag + " is already visible.");
            return true;
        }
        return false;

    }

    private boolean isAlwaysUpload() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity
                .ALWAYS_UPLOAD, false);
    }

    private boolean isUploadOnlyInWlan() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity
                .WIFI_UPLOAD, true);
    }

    private void checkKeepScreenOn() {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity
                .DISPLAY_STAYS_ACTIV, false)) {
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

    //    /**
    //     *
    //     */
    //    private void prepareNavDrawerItems() {
    //        if (this.navDrawerItems == null) {
    //            navDrawerItems = new NavMenuItem[8];
    //            navDrawerItems[LOGIN] = new NavMenuItem(LOGIN, getResources().getString(R.string
    //                    .menu_login), R.drawable.device_access_accounts);
    //            navDrawerItems[LOGBOOK] = new NavMenuItem(LOGBOOK, getResources().getString(R
    // .string
    //                    .menu_logbook), R.drawable.logbook);
    //            navDrawerItems[SETTINGS] = new NavMenuItem(SETTINGS, getResources().getString(R
    //                    .string.menu_settings), R.drawable.action_settings);
    //            navDrawerItems[START_STOP_MEASUREMENT] = new NavMenuItem(START_STOP_MEASUREMENT,
    //                    getResources().getString(R.string.menu_start), R.drawable.av_play);
    //            navDrawerItems[DASHBOARD] = new NavMenuItem(DASHBOARD, getResources().getString(R
    //                    .string.dashboard), R.drawable.dashboard);
    //            navDrawerItems[MY_TRACKS] = new NavMenuItem(MY_TRACKS, getResources().getString(R
    //                    .string.my_tracks), R.drawable.device_access_storage);
    //            navDrawerItems[HELP] = new NavMenuItem(HELP, getResources().getString(R.string
    //                    .menu_help), R.drawable.action_help);
    //            navDrawerItems[SEND_LOG] = new NavMenuItem(SEND_LOG, getResources().getString(R
    //                    .string.menu_send_log), R.drawable.action_report);
    //        }
    //
    //        if (mUserManager.isLoggedIn()) {
    //            navDrawerItems[LOGIN].setTitle(getResources().getString(R.string.menu_logout));
    //            navDrawerItems[LOGIN].setSubtitle(String.format(getResources()
    //                    .getString(R.string.logged_in_as), mUserManager.getUser().getUsername()));
    //        } else {
    //            navDrawerItems[LOGIN].setTitle(getResources().getString(R.string.menu_login));
    //            navDrawerItems[LOGIN].setSubtitle("");
    //        }
    //
    //        mNavDrawerAdapter.notifyDataSetChanged();
    //    }


    //    private void openFragment(int position) {
    //        FragmentManager manager = getFragmentManager();
    //
    //        switch (position) {
    //
    //            // Go to the dashboard
    //
    //            case DASHBOARD:
    //
    //                if (isFragmentVisible(DASHBOARD_TAG)) {
    //                    break;
    //                }
    //                manager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    //                manager.beginTransaction().replace(R.id.content_frame, null,
    //                        DASHBOARD_TAG).commit();
    //                break;
    //
    //            //Start the Login activity
    //
    //            case LOGIN:
    //                if (mUserManager.isLoggedIn()) {
    //                    mUserManager.logOut();
    //                    ListTracksFragment listMeasurementsFragment = (ListTracksFragment)
    //                            getFragmentManager().findFragmentByTag("MY_TRACKS");
    //                    // check if this fragment is initialized
    //                    if (listMeasurementsFragment != null) {
    //                        listMeasurementsFragment.clearRemoteTracks();
    //                    } else {
    //                        //the remote tracks need to be removed in any case
    //                        mDBAdapter.deleteAllRemoteTracks();
    //                    }
    //                    Crouton.makeText(this, R.string.bye_bye, Style.CONFIRM).show();
    //                } else {
    //                    if (isFragmentVisible(LOGIN_TAG)) {
    //                        break;
    //                    }
    //                    LoginFragment loginFragment = new LoginFragment();
    //                    manager.beginTransaction()
    //                            .replace(R.id.content_frame, loginFragment, LOGIN_TAG)
    //                            .addToBackStack(null)
    //                            .commit();
    //                }
    //                break;
    //
    //            // Go to the settings
    //
    //            case SETTINGS:
    ////                Intent configIntent = new Intent(this, SettingsActivity.class);
    ////                startActivity(configIntent);
    //
    //                SettingsFragment fragment = new SettingsFragment();
    //                manager.beginTransaction().replace(R.id.content_frame, fragment, SETTINGS_TAG)
    //                        .addToBackStack(null).commit();
    //                break;
    //
    //            // Go to the track list
    //
    //            case MY_TRACKS:
    //
    //                if (isFragmentVisible(MY_TRACKS_TAG)) {
    //                    break;
    //                }
    //                ListTracksFragment listMeasurementFragment = new ListTracksFragment();
    //                manager.beginTransaction()
    //                        .replace(R.id.content_frame, listMeasurementFragment, MY_TRACKS_TAG)
    //                        .addToBackStack(null)
    //                        .commit();
    //                break;
    //
    //            // Start or stop the measurement process
    //
    //            case START_STOP_MEASUREMENT:
    //                if (false) return;
    //
    //                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences
    //                        (this.getApplicationContext());
    //
    //                String remoteDevice = preferences.getString(org.envirocar.app.activity
    //                        .SettingsActivity.BLUETOOTH_KEY, null);
    //
    //                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    //                if (adapter != null && adapter.isEnabled() && remoteDevice != null) {
    //                    if (mCarPrefHandler.getCar() == null) {
    //                        Intent settingsIntent = new Intent(this, SettingsActivity.class);
    //                        startActivity(settingsIntent);
    //                    } else {
    //                    /*
    //                     * We are good to go. process the state and stuff
    //					 */
    //                        StartStopButtonUtil.OnTrackModeChangeListener trackModeListener =
    //                                new StartStopButtonUtil.OnTrackModeChangeListener() {
    //                                    @Override
    //                                    public void onTrackModeChange(int tm) {
    //                                        trackMode = tm;
    //                                    }
    //                                };
    ////                        mStart
    //                        StartStopButtonUtil startStopButtonUtil = new
    //                                StartStopButtonUtil(this, trackMode, mServiceState,
    // mServiceState
    //                                == BluetoothServiceState
    //                                .SERVICE_DEVICE_DISCOVERY_PENDING);
    //                        startStopButtonUtil.processButtonClick(trackModeListener);
    //                    }
    //                } else {
    //                    Intent settingsIntent = new Intent(this, SettingsActivity.class);
    //                    startActivity(settingsIntent);
    //                }
    //                break;
    //            case HELP:
    //
    //                if (isFragmentVisible(HELP_TAG)) {
    //                    break;
    //                }
    //                HelpFragment helpFragment = new HelpFragment();
    //                manager.beginTransaction().replace(R.id.content_frame, helpFragment, HELP_TAG)
    //                        .addToBackStack(null).commit();
    //                break;
    //            case SEND_LOG:
    //
    //                if (isFragmentVisible(SEND_LOG_TAG)) {
    //                    break;
    //                }
    //                SendLogFileFragment logFragment = new SendLogFileFragment();
    //                manager.beginTransaction().replace(R.id.content_frame, logFragment,
    // SEND_LOG_TAG)
    //                        .addToBackStack(null).commit();
    //            default:
    //                break;
    //
    //            case LOGBOOK:
    //
    //                if (isFragmentVisible(LOGBOOK_TAG)) {
    //                    break;
    //                }
    //                LogbookFragment logbookFragment = new LogbookFragment();
    //                manager.beginTransaction().replace(R.id.content_frame, logbookFragment,
    //                        LOGBOOK_TAG).addToBackStack(null).commit();
    //                break;
    //        }
    //
    ////        mDrawerLayout.closeDrawer(mDrawerList);
    //    }


    //    /**
    //     * start a thread that updates the UI until the device was
    //     * discovered
    //     */
    //    private void invokeRemainingTimeThread() {
    //        if (remainingTimeThread == null || discoveryTargetTime > System.currentTimeMillis()) {
    //            remainingTimeHandler = new Handler();
    //            remainingTimeThread = new Runnable() {
    //                @Override
    //                public void run() {
    //                    final long deltaSec = (discoveryTargetTime - System.currentTimeMillis()) /
    // 1000;
    //                    final long minutes = deltaSec / 60;
    //                    final long secs = deltaSec - (minutes * 60);
    //                    if (mServiceState == BluetoothServiceState
    // .SERVICE_DEVICE_DISCOVERY_PENDING
    //                            && deltaSec > 0) {
    //                        runOnUiThread(new Runnable() {
    //                            @Override
    //                            public void run() {
    //                                navDrawerItems[START_STOP_MEASUREMENT].setSubtitle(
    //                                        String.format(getString(R.string
    // .device_discovery_next_try),
    //                                                String.format("%02d", minutes), String.format
    //                                                        ("%02d", secs)
    //                                        ));
    //                                mNavDrawerAdapter.notifyDataSetChanged();
    //                            }
    //                        });
    //
    //						/*
    //                         * re-invoke the painting
    //						 */
    //                        remainingTimeHandler.postDelayed(remainingTimeThread, 1000);
    //                    } else {
    //                        LOGGER.info("NOT SHOWING!");
    //                    }
    //                }
    //            };
    //            remainingTimeHandler.post(remainingTimeThread);
    //        } else {
    //            LOGGER.info("not invoking the discovery time painting thread: " +
    //                    (remainingTimeThread == null) + ", " + (discoveryTargetTime - System
    //                    .currentTimeMillis()));
    //        }
    //    }

    protected void resolvePersistentSeenAnnouncements() {
        String pers = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingsActivity.PERSISTENT_SEEN_ANNOUNCEMENTS, "");

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

    //    private class NavAdapter extends BaseAdapter {
    //
    //        @Override
    //        public boolean isEnabled(int position) {
    //            //to allow things like start bluetooth or go to settings from "disabled" action
    //            return (position == START_STOP_MEASUREMENT ? true : navDrawerItems[position]
    //                    .isEnabled());
    //        }
    //
    //        @Override
    //        public int getCount() {
    //            return navDrawerItems.length;
    //        }
    //
    //        @Override
    //        public Object getItem(int position) {
    //            return navDrawerItems[position];
    //        }
    //
    //        @Override
    //        public long getItemId(int position) {
    //            return position;
    //        }
    //
    //        @Override
    //        public View getView(int position, View convertView, ViewGroup parent) {
    //            NavMenuItem currentItem = ((NavMenuItem) getItem(position));
    //            View item;
    //            if (currentItem.getSubtitle().equals("")) {
    //                item = View.inflate(BaseMainActivity.this, R.layout.nav_item_1, null);
    //
    //            } else {
    //                item = View.inflate(BaseMainActivity.this, R.layout.nav_item_2, null);
    //                TextView textView2 = (TextView) item.findViewById(android.R.id.text2);
    //                textView2.setText(currentItem.getSubtitle());
    //                if (!currentItem.isEnabled()) textView2.setTextColor(Color.GRAY);
    //            }
    //            ImageView icon = ((ImageView) item.findViewById(R.id.nav_item_icon));
    //            icon.setImageResource(currentItem.getIconRes());
    //            TextView textView = (TextView) item.findViewById(android.R.id.text1);
    //            textView.setText(currentItem.getTitle());
    //            if (!currentItem.isEnabled()) {
    //                textView.setTextColor(Color.GRAY);
    //                icon.setColorFilter(Color.GRAY);
    //            }
    //            TypefaceEC.applyCustomFont((ViewGroup) item, TypefaceEC.Raleway(BaseMainActivity
    // .this));
    //            return item;
    //        }
    //    }
}
