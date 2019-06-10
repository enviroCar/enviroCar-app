package org.envirocar.app.views.statistics;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import org.envirocar.app.R;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.TrackDAOHandler;
import org.envirocar.app.handler.UserHandler;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.main.MainActivityComponent;
import org.envirocar.app.main.MainActivityModule;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.logging.Logger;
import org.envirocar.storage.EnviroCarDB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class StatisticsFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(StatisticsFragment.class);

    @Inject
    protected UserHandler mUserManager;

    @Inject
    protected DAOProvider mDAOProvider;

    @Inject
    protected TrackDAOHandler mTrackDAOHandler;

    @Inject
    protected EnviroCarDB mEnvirocarDB;

    @BindView(R.id.stat_last_track_header)
    protected TextView LastTrackHeader;

    @BindView(R.id.stat_track_name)
    protected TextView LastTrackName;

    @BindView(R.id.stat_track_time)
    protected TextView LastTrackTime;

    @BindView(R.id.stat_card)
    protected CardView cardView;

    @BindView(R.id.stat_scrollView)
    protected ScrollView scrollView;

    @BindView(R.id.stat_track_date)
    protected TextView LastTrackDate;

    @BindView(R.id.stat_fuel_consum)
    protected TextView LastTrackFuel;

    @BindView(R.id.stat_avg_speed)
    protected TextView LastTrackSpeed;

    @BindView(R.id.stat_comp_fuel)
    protected TextView LastTrackStatFuel;

    @BindView(R.id.stat_comp_speed)
    protected TextView LastTrackStatSpeed;

    @BindView(R.id.stat_graph_spinner)
    protected Spinner GraphSpinner;

    @BindView(R.id.stat_tabLayout)
    protected TabLayout tabLayout;

    @BindView(R.id.stat_viewPager)
    protected ViewPager viewPager;

    @BindView(R.id.stat_progress)
    protected TextView ProgressMessage;

    protected boolean isUserSignedIn;
    protected Unbinder unbinder;

    private boolean hasLoadedRemote = false;
    private boolean hasLoadedStored = false;
    private boolean isSorted = false;
    protected final Object attachingActivityLock = new Object();
    protected boolean isAttached = false;
    private CompositeSubscription subscriptions ;
    protected Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    protected boolean tracksLoaded = false;
    protected final List<Track> mTrackList = Collections.synchronizedList(new ArrayList<>());


    public StatisticsFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@Nullable LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View statView= inflater.inflate(R.layout.fragment_statistics, container, false);
        unbinder = ButterKnife.bind(this, statView);
        isUserSignedIn = mUserManager.isLoggedIn();
        ProgressMessage.setVisibility(View.INVISIBLE);
        tracksLoaded = false;
        subscriptions = new CompositeSubscription();
        synchronized (attachingActivityLock) {
            isAttached = true;
            attachingActivityLock.notifyAll();
        }

        if(!isUserSignedIn)
        {
            cardView.setVisibility(View.INVISIBLE);
            scrollView.setVisibility(View.INVISIBLE);
        }
        else{
            loadDataset();
            ViewPagerAdapter adapter = new ViewPagerAdapter(getFragmentManager());
            viewPager.setAdapter(adapter);
            tabLayout.setupWithViewPager(viewPager);
            //statisticsTrackInterface.sendTracks(mTrackList);
            mUserManager.getUser();


        }


        return statView;
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        MainActivityComponent mainActivityComponent =  baseApplicationComponent.plus(new MainActivityModule(getActivity()));
        mainActivityComponent.inject(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mUserManager.isLoggedIn()) {
            ViewPagerAdapter adapter = new ViewPagerAdapter(getFragmentManager());
            viewPager.setAdapter(adapter);
            tabLayout.setupWithViewPager(viewPager);
            mUserManager.getUser();
            loadDataset();
        } else {
            cardView.setVisibility(View.INVISIBLE);
            scrollView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        LOG.info("onDestroyView()");
        super.onDestroyView();

        if (!subscriptions.isUnsubscribed()) {
            subscriptions.unsubscribe();
        }
        unbinder.unbind();
    }

    private final class LoadRemoteTracksTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {

            synchronized (attachingActivityLock) {
                while (!isAttached) {
                    try {
                        attachingActivityLock.wait();
                    } catch (InterruptedException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            }

            subscriptions.add(mDAOProvider.getTrackDAO().getTrackIdsObservable(1,1)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<List<Track>>() {

                        @Override
                        public void onStart() {
                            LOG.info("onStart() tracks in db");
                            mMainThreadWorker.schedule(() -> {
                                ProgressMessage.setVisibility(View.VISIBLE);
                            });
                        }

                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            LOG.error(e.getMessage(), e);
                            showSnackbar(R.string.track_list_loading_lremote_tracks_error);
                        }

                        @Override
                        public void onNext(List<Track> tracks) {
                            LOG.info("onNext(" + tracks.size() + ") locally stored tracks");
                            for (Track track : tracks) {
                                if (track.getMeasurements() != null &&
                                        !track.getMeasurements().isEmpty()) {
                                    if (mTrackList.contains(track)) {
                                        mTrackList.set(mTrackList.indexOf(track), track);
                                    } else {
                                        mTrackList.add(track);
                                    }
                                }
                            }

                            hasLoadedStored = true;
                            updateView();
                        }
                    }));

            subscriptions.add(mDAOProvider.getTrackDAO().getTrackIdsObservable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<List<Track>>() {

                        @Override
                        public void onStart() {
                            LOG.info("onStart() tracks in db");
                            mMainThreadWorker.schedule(() -> {
                                ProgressMessage.setVisibility(View.VISIBLE);
                            });
                        }

                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            LOG.error(e.getMessage(), e);

                            if (e instanceof NotConnectedException) {
                                LOG.error("Error", e);
                                if (mTrackList.isEmpty()) {
                                    LOG.debug("TrackList Empty");
                                    showSnackbar(R.string.track_list_bg_no_connection);
                                }
                            } else if (e instanceof UnauthorizedException) {
                                LOG.error("Unauthorised",e);
                                showSnackbar(R.string.track_list_bg_unauthorized);
                                if (mTrackList.isEmpty()) {
                                    LOG.debug("TrackList Empty");
                                    showSnackbar(R.string.track_list_bg_unauthorized);
                                }
                            }

                            ProgressMessage.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onNext(List<Track> tracks) {
                            LOG.info("onNext(" + tracks.size() + ") remotely stored tracks");

                            for (Track track : tracks) {
                                if (!mTrackList.contains(track)) {
                                    mTrackList.add(track);
                                }
                            }
                            hasLoadedRemote = true;

                            updateView();
                        }
                    }));

            return null;
        }
    }

    private void updateView() {
        if (hasLoadedStored && hasLoadedRemote) {
            if (!isSorted) {
                isSorted = true;
                Collections.sort(mTrackList);
            }
            ProgressMessage.setVisibility(View.INVISIBLE);

            if (mTrackList.isEmpty()) {
                showSnackbar(R.string.track_list_bg_no_remote_tracks);
            }
        }

        if (!mTrackList.isEmpty()) {
            LastTrackDate.setText(mTrackList.get(0).getName());
            LastTrackTime.setText(mTrackList.size()+"");

        }
    }
    protected void showSnackbar(final int message) {
        mMainThreadWorker.schedule(() -> {
            if (getView() != null) {
                Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    protected void showSnackbar(final String message) {
        mMainThreadWorker.schedule(() -> {
            if (getView() != null) {
                Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    protected void loadDataset() {
        if (mUserManager.isLoggedIn() && !tracksLoaded) {
            tracksLoaded = true;
            //Toast.makeText(getContext(), "Starting", Toast.LENGTH_SHORT).show();
            new LoadRemoteTracksTask().execute();
        }
    }
}
