/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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
package org.envirocar.app.views;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.mapbox.mapboxsdk.Mapbox;
import com.squareup.otto.Subscribe;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.databinding.ActivityBaseMainBottomBarBinding;
import org.envirocar.app.handler.ApplicationSettings;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.TemporaryFileManager;
import org.envirocar.app.handler.agreement.AgreementManager;
import org.envirocar.app.handler.preferences.CarPreferenceHandler;
import org.envirocar.app.handler.preferences.UserPreferenceHandler;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.injection.modules.MainActivityModule;
import org.envirocar.app.interactor.ValidateAcceptedTerms;
import org.envirocar.app.services.autoconnect.AutoRecordingService;
import org.envirocar.app.views.dashboard.DashboardFragment;
import org.envirocar.app.views.others.OthersFragment;
import org.envirocar.app.views.others.TroubleshootingFragment;
import org.envirocar.app.views.tracklist.TrackListPagerFragment;
import org.envirocar.core.events.TrackFinishedEvent;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.ServiceUtils;

import java.util.Stack;

import javax.inject.Inject;



import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;

/**
 * @authro dewall
 */
public class BaseMainActivity extends BaseInjectorActivity {
    private static final Logger LOGGER = Logger.getLogger(BaseMainActivity.class);

    private static final String TROUBLESHOOTING_TAG = "TROUBLESHOOTING";

    private FragmentStatePagerAdapter fragmentStatePagerAdapter;
    private MenuItem prevMenuItem;

    // Custom Callback Stack
    Stack<Integer> callbackStack = new Stack<Integer>();

    // Injected variables
    @Inject
    protected UserPreferenceHandler mUserManager;
    @Inject
    protected CarPreferenceHandler mCarManager;
    @Inject
    protected TemporaryFileManager mTemporaryFileManager;
    @Inject
    protected DAOProvider mDAOProvider;
    @Inject
    protected BluetoothHandler mBluetoothHandler;

    // activity scoped
    @Inject
    protected DashboardFragment dashboardFragment;
    @Inject
    protected TrackListPagerFragment trackListPagerFragment;
    @Inject
    protected OthersFragment othersFragment;
    @Inject
    protected Mapbox mapbox;
    @Inject
    protected AgreementManager agreementManager;

    @Inject
    protected ValidateAcceptedTerms validateTermsOfUse;


    protected BottomNavigationView navigationBottomBar;


    protected ViewPager viewPager;

    private CompositeDisposable subscriptions = new CompositeDisposable();
    private BroadcastReceiver errorInformationReceiver;
    private boolean paused;

    private Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = item -> {
        switch (item.getItemId()) {
            case R.id.navigation_dashboard:
                viewPager.setCurrentItem(0);
                return true;
            case R.id.navigation_my_tracks:
                viewPager.setCurrentItem(1);
                return true;
            case R.id.navigation_others:
                viewPager.setCurrentItem(2);
                return true;
        }
        return false;
    };

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent
                .plus(new MainActivityModule(this))
                .inject(this);
    }
    private ActivityBaseMainBottomBarBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        LOGGER.info("BaseMainActivity : onCreate");
        super.onCreate(savedInstanceState);
        binding = ActivityBaseMainBottomBarBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        navigationBottomBar = binding.navigation;
        viewPager = binding.fragmentContainer;

        navigationBottomBar.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigationBottomBar.setSelectedItemId(R.id.navigation_dashboard);

        fragmentStatePagerAdapter = new PageSlider(getSupportFragmentManager(), FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        viewPager.setAdapter(fragmentStatePagerAdapter);

        // Custom Back Navigation for fragments in BaseMainActivity
        callbackStack.push(0);
        OnBackPressedCallback callback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                // Handle the back button event
                callbackStack.pop();
                viewPager.setCurrentItem(callbackStack.peek());
                if(callbackStack.size() < 2)
                    this.setEnabled(false);
            }
        };
        this.getOnBackPressedDispatcher().addCallback(this, callback);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (prevMenuItem != null) {
                    prevMenuItem.setChecked(false);
                } else {
                    navigationBottomBar.getMenu().getItem(0).setChecked(false);
                }
                // add page to callbackStack
                if (callbackStack.peek() != position) {
                    callbackStack.push(position);
                    callback.setEnabled(true);
                }
                navigationBottomBar.getMenu().getItem(position).setChecked(true);
                prevMenuItem = navigationBottomBar.getMenu().getItem(position);
                fragmentStatePagerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
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

        // Check whether newest TermsOfUse have been accepted.
        validateTermsOfUse.execute(handleTermsOfUseValidation(),
                new ValidateAcceptedTerms.Params(mUserManager.getUser(), this));


        registerReceiver(errorInformationReceiver, new IntentFilter(TroubleshootingFragment.INTENT));
    }

    private DisposableObserver<Boolean> handleTermsOfUseValidation() {
        return new DisposableObserver<Boolean>() {
            @Override
            public void onNext(Boolean aBoolean) {
                LOGGER.info("accepted? " + aBoolean);
            }

            @Override
            public void onError(Throwable e) {
                LOGGER.error(e);
            }

            @Override
            public void onComplete() {
                LOGGER.info("onComplete");
            }
        };
    }

    @Override
    protected void onPause() {
        LOGGER.info("BaseMainActivity : onPause");
        super.onPause();
        this.paused = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        LOGGER.info("BaseMainActivity : onSaveInstanceState");
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        LOGGER.info("BaseMainActivity : onResume()");
        super.onResume();
        // Check whether the screen is required to keep the screen on.
        checkKeepScreenOn();
    }

    @Override
    protected void onDestroy() {
        LOGGER.info("BaseMainActivity : onDestroy()");
        super.onDestroy();

        this.unregisterReceiver(errorInformationReceiver);
        mTemporaryFileManager.shutdown();

        if (!subscriptions.isDisposed()) {
            subscriptions.dispose();
        }
    }

    private void addPreferenceSubscriptions() {
        // Keep screen active setting;
        subscriptions.add(
                ApplicationSettings.getDisplayStaysActiveObservable(this)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(aBoolean -> {
                            checkKeepScreenOn();
                        }));

        // Start Background handler
        subscriptions.add(
                ApplicationSettings.getAutoconnectEnabledObservable(this)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(aBoolean -> {
                            if (aBoolean) {
                                if (!ServiceUtils.isServiceRunning(this, AutoRecordingService.class))
                                    AutoRecordingService.startService(this);
                            } else {
                                if (ServiceUtils.isServiceRunning(this, AutoRecordingService.class))
                                    AutoRecordingService.stopService(this);
                            }
                        }, LOGGER::error));
    }

    private void checkKeepScreenOn() {
        if (ApplicationSettings.getDisplayStaysActiveObservable(this).blockingFirst()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            this.navigationBottomBar.setKeepScreenOn(true);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            this.navigationBottomBar.setKeepScreenOn(false);
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

    private class PageSlider extends FragmentStatePagerAdapter {

        private Fragment[] fragments;

        public PageSlider(@NonNull FragmentManager fm, int behavior) {
            super(fm, behavior);
            fragments = new Fragment[]{
                    dashboardFragment,
                    trackListPagerFragment,
                    othersFragment
            };
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments[position];
        }

        @Override
        public int getCount() {
            return 3;
        }
    }
}