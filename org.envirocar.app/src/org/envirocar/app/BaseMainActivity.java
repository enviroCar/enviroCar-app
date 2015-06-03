package org.envirocar.app;

import android.app.Fragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.envirocar.app.activity.DashboardFragment;
import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.activity.StartStopButtonUtil;
import org.envirocar.app.application.CarManager;
import org.envirocar.app.application.NavMenuItem;
import org.envirocar.app.application.TemporaryFileManager;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.application.service.AbstractBackgroundServiceStateReceiver;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.views.TypefaceEC;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import de.keyboardsurfer.android.widget.crouton.Crouton;

/**
 * Main UI application that cares about the auto-upload, auto-connect and global
 * UI elements
 *
 * @author dewall
 */
public class BaseMainActivity extends BaseInjectorActivity {
    public static final int TRACK_MODE_SINGLE = 0;
    public static final int TRACK_MODE_AUTO = 1;
    public static final int REQUEST_MY_GARAGE = 1336;
    public static final int REQUEST_REDIRECT_TO_GARAGE = 1337;
    private static final String TAG = BaseMainActivity.class.getSimpleName();
    private static final Logger LOGGER = Logger.getLogger(BaseApplication.class);
    private static final int DASHBOARD = 0;
    private static final int LOGIN = 1;
    private static final int MY_TRACKS = 2;
    private static final int START_STOP_MEASUREMENT = 3;
    private static final int SETTINGS = 4;
    private static final int LOGBOOK = 5;
    private static final int HELP = 6;
    private static final int SEND_LOG = 7;

    private static final String TRACK_MODE = "trackMode";
    private static final String SEEN_ANNOUNCEMENTS = "seenAnnouncements";

    private static final String DASHBOARD_TAG = "DASHBOARD";
    private static final String LOGIN_TAG = "LOGIN";
    private static final String MY_TRACKS_TAG = "MY_TRACKS";
    private static final String HELP_TAG = "HELP";
    private static final String TROUBLESHOOTING_TAG = "TROUBLESHOOTING";
    private static final String SEND_LOG_TAG = "SEND_LOG";
    private static final String LOGBOOK_TAG = "LOGBOOK";

    private final AdapterView.OnItemClickListener onNavDrawerClickListener =
            new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            openFragment(position);
        }
    };

    // Injected variables
    @Inject
    protected UserManager mUserManager;
    @Inject
    protected CarManager mCarManager;
    @Inject
    protected TemporaryFileManager mTemporaryFileManager;
    @Inject
    protected DashboardFragment mDashboardFragment;
    @Inject
    protected TrackHandler mTrackHandler;


    // REMOVE
    @Inject
    protected DbAdapter mDBAdapter;
    protected AbstractBackgroundServiceStateReceiver.ServiceState serviceState
            = AbstractBackgroundServiceStateReceiver.ServiceState.SERVICE_STOPPED;
    private int trackMode = TRACK_MODE_SINGLE;
    private Set<String> seenAnnouncements = new HashSet<String>();

    // Menu Items
    private NavMenuItem[] navDrawerItems;
    //Navigation Drawer
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private NavAdapter mNavDrawerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the content view of the application
        setContentView(R.layout.main_layout);

        checkKeepScreenOn();

        // Initialize the navigation drawer.
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mNavDrawerAdapter = new NavAdapter();
        prepareNavDrawerItems();
        updateStartStopButton();
        mDrawerList.setAdapter(mNavDrawerAdapter);

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, R.drawable.ic_drawer, R.string.open_drawer,
                R.string.close_drawer) {

            @Override
            public void onDrawerOpened(View drawerView) {
                prepareNavDrawerItems();
                super.onDrawerOpened(drawerView);
            }
        };

        mDrawerLayout.setDrawerListener(actionBarDrawerToggle);
        mDrawerList.setOnItemClickListener(onNavDrawerClickListener);


        // Set the DashboardFragment as initial fragment.
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, mDashboardFragment, DASHBOARD_TAG).commit();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Crouton.cancelAllCroutons();

        mTemporaryFileManager.shutdown();

    }


    @Override
    public List<Object> getInjectionModules() {
        return Arrays.<Object>asList(new InjectionActivityModule(this));
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
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        StartStopButtonUtil startStopUtil = new StartStopButtonUtil(this, trackMode, serviceState,
                serviceState == AbstractBackgroundServiceStateReceiver.ServiceState.SERVICE_DEVICE_DISCOVERY_PENDING);
        if (adapter != null && adapter.isEnabled()) { // was requirementsFulfilled
            startStopUtil.updateStartStopButtonOnServiceStateChange(navDrawerItems[START_STOP_MEASUREMENT]);
        } else {
            startStopUtil.defineButtonContents(navDrawerItems[START_STOP_MEASUREMENT],
                    false, R.drawable.not_available, getString(R.string.pref_bluetooth_disabled),
                    getString(R.string.menu_start));
        }

        mNavDrawerAdapter.notifyDataSetChanged();
    }

    /**
     *
     */
    private void prepareNavDrawerItems() {
        if (this.navDrawerItems == null) {
            navDrawerItems = new NavMenuItem[8];
            navDrawerItems[LOGIN] = new NavMenuItem(LOGIN, getResources().getString(R.string.menu_login), R.drawable.device_access_accounts);
            navDrawerItems[LOGBOOK] = new NavMenuItem(LOGBOOK, getResources().getString(R.string.menu_logbook), R.drawable.logbook);
            navDrawerItems[SETTINGS] = new NavMenuItem(SETTINGS, getResources().getString(R.string.menu_settings), R.drawable.action_settings);
            navDrawerItems[START_STOP_MEASUREMENT] = new NavMenuItem(START_STOP_MEASUREMENT, getResources().getString(R.string.menu_start), R.drawable.av_play);
            navDrawerItems[DASHBOARD] = new NavMenuItem(DASHBOARD, getResources().getString(R.string.dashboard), R.drawable.dashboard);
            navDrawerItems[MY_TRACKS] = new NavMenuItem(MY_TRACKS, getResources().getString(R.string.my_tracks), R.drawable.device_access_storage);
            navDrawerItems[HELP] = new NavMenuItem(HELP, getResources().getString(R.string.menu_help), R.drawable.action_help);
            navDrawerItems[SEND_LOG] = new NavMenuItem(SEND_LOG, getResources().getString(R.string.menu_send_log), R.drawable.action_report);
        }

        if (mUserManager.isLoggedIn()) {
            navDrawerItems[LOGIN].setTitle(getResources().getString(R.string.menu_logout));
            navDrawerItems[LOGIN].setSubtitle(String.format(getResources()
                    .getString(R.string.logged_in_as), mUserManager.getUser().getUsername()));
        } else {
            navDrawerItems[LOGIN].setTitle(getResources().getString(R.string.menu_login));
            navDrawerItems[LOGIN].setSubtitle("");
        }

        mNavDrawerAdapter.notifyDataSetChanged();
    }



    private void openFragment(int position) {
        FragmentManager manager = getFragmentManager();

//        switch (position) {
//
//            // Go to the dashboard
//
//            case DASHBOARD:
//
//                if (isFragmentVisible(DASHBOARD_TAG)) {
//                    break;
//                }
//                Fragment dashboardFragment = getFragmentManager().findFragmentByTag(DASHBOARD_TAG);
//                if (dashboardFragment == null) {
//                    dashboardFragment = new DashboardFragment();
//                }
//                manager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
//                manager.beginTransaction().replace(R.id.content_frame, dashboardFragment, DASHBOARD_TAG).commit();
//                break;
//
//            //Start the Login activity
//
//            case LOGIN:
//                if (mUserManager.isLoggedIn()) {
//                    mUserManager.logOut();
//                    ListTracksFragment listMeasurementsFragment = (ListTracksFragment) getFragmentManager().findFragmentByTag("MY_TRACKS");
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
//                Intent configIntent = new Intent(this, SettingsActivity.class);
//                startActivity(configIntent);
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
//                manager.beginTransaction().replace(R.id.content_frame, listMeasurementFragment, MY_TRACKS_TAG).addToBackStack(null).commit();
//                break;
//
//            // Start or stop the measurement process
//
//            case START_STOP_MEASUREMENT:
//                if (!navDrawerItems[position].isEnabled()) return;
//
//                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
//
//                String remoteDevice = preferences.getString(org.envirocar.app.activity.SettingsActivity.BLUETOOTH_KEY, null);
//
//                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
//                if (adapter != null && adapter.isEnabled() && remoteDevice != null) {
//                    if (mCarManager.getCar() == null) {
//                        Intent settingsIntent = new Intent(this, SettingsActivity.class);
//                        startActivity(settingsIntent);
//                    } else {
//                    /*
//                     * We are good to go. process the state and stuff
//					 */
//                        StartStopButtonUtil.OnTrackModeChangeListener trackModeListener = new StartStopButtonUtil.OnTrackModeChangeListener() {
//                            @Override
//                            public void onTrackModeChange(int tm) {
//                                trackMode = tm;
//                            }
//                        };
//                        StartStopButtonUtil startStopButtonUtil = new
//                                createStartStopUtil().processButtonClick(trackModeListener);
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
//                manager.beginTransaction().replace(R.id.content_frame, helpFragment, HELP_TAG).addToBackStack(null).commit();
//                break;
//            case SEND_LOG:
//
//                if (isFragmentVisible(SEND_LOG_TAG)) {
//                    break;
//                }
//                SendLogFileFragment logFragment = new SendLogFileFragment();
//                manager.beginTransaction().replace(R.id.content_frame, logFragment, SEND_LOG_TAG).addToBackStack(null).commit();
//            default:
//                break;
//
//            case LOGBOOK:
//
//                if (isFragmentVisible(LOGBOOK_TAG)) {
//                    break;
//                }
//                LogbookFragment logbookFragment = new LogbookFragment();
//                manager.beginTransaction().replace(R.id.content_frame, logbookFragment, LOGBOOK_TAG).addToBackStack(null).commit();
//                break;
//        }

        mDrawerLayout.closeDrawer(mDrawerList);
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

    private void switchToFragment(Fragment fragment, String tag) {
        getFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment, tag)
                .addToBackStack(null)
                .commit();
    }

    private class NavAdapter extends BaseAdapter {

        @Override
        public boolean isEnabled(int position) {
            //to allow things like start bluetooth or go to settings from "disabled" action
            return (position == START_STOP_MEASUREMENT ? true : navDrawerItems[position].isEnabled());
        }

        @Override
        public int getCount() {
            return navDrawerItems.length;
        }

        @Override
        public Object getItem(int position) {
            return navDrawerItems[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            NavMenuItem currentItem = ((NavMenuItem) getItem(position));
            View item;
            if (currentItem.getSubtitle().equals("")) {
                item = View.inflate(BaseMainActivity.this, R.layout.nav_item_1, null);

            } else {
                item = View.inflate(BaseMainActivity.this, R.layout.nav_item_2, null);
                TextView textView2 = (TextView) item.findViewById(android.R.id.text2);
                textView2.setText(currentItem.getSubtitle());
                if (!currentItem.isEnabled()) textView2.setTextColor(Color.GRAY);
            }
            ImageView icon = ((ImageView) item.findViewById(R.id.nav_item_icon));
            icon.setImageResource(currentItem.getIconRes());
            TextView textView = (TextView) item.findViewById(android.R.id.text1);
            textView.setText(currentItem.getTitle());
            if (!currentItem.isEnabled()) {
                textView.setTextColor(Color.GRAY);
                icon.setColorFilter(Color.GRAY);
            }
            TypefaceEC.applyCustomFont((ViewGroup) item, TypefaceEC.Raleway(BaseMainActivity.this));
            return item;
        }
    }
}
