package org.envirocar.app.views.statistics;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.envirocar.app.R;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.TrackDAOHandler;
import org.envirocar.app.handler.UserHandler;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.main.MainActivityComponent;
import org.envirocar.app.main.MainActivityModule;
import org.envirocar.core.entity.Phenomenon;
import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.TrackStatistics;
import org.envirocar.core.entity.UserStatistics;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.logging.Logger;
import org.envirocar.storage.EnviroCarDB;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class StatisticsFragment extends BaseInjectorFragment implements AdapterView.OnItemSelectedListener {

    private static final Logger LOG = Logger.getLogger(StatisticsFragment.class);
    private static final DecimalFormat TWO_DIGITS_FORMATTER = new DecimalFormat("#.##");

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
    protected NestedScrollView scrollView;

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

    @BindView(R.id.loading_layout)
    protected ConstraintLayout ProgressMessage;

    @BindView(R.id.stat_info)
    protected TextView StatInfo;

    @BindView(R.id.stat_img_comp_fuel)
    protected ImageView compFuel;

    @BindView(R.id.stat_img_comp_speed)
    protected ImageView compSpeed;

    @BindView(R.id.no_tracks_card)
    protected ConstraintLayout noTracksCard;

    @BindView(R.id.no_user_layout)
    protected ConstraintLayout noUser;

    @BindView(R.id.header_gradient)
    protected ImageView headerGradient;

    @BindView(R.id.header)
    protected ImageView header;

    @BindView(R.id.tracks_card)
    protected ConstraintLayout TracksCard;

    protected boolean isUserSignedIn;
    protected Unbinder unbinder;
    private ChoiceViewModel choiceViewModel;
    private CompositeSubscription subscriptions ;
    protected Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    protected boolean tracksLoaded = false;
    protected boolean userStatsLoaded = false;
    protected boolean trackStatsLoaded = false;
    protected List<Track> mTrackList = Collections.synchronizedList(new ArrayList<>());
    protected UserStatistics mUserStatistics;
    protected TrackStatistics mTrackStatistics;
    Float trackAvgSpeed;
    Float trackAvgFuel;



    public StatisticsFragment() {

    }

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        MainActivityComponent mainActivityComponent =  baseApplicationComponent.
                plus(new MainActivityModule(getActivity()));
        mainActivityComponent.inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        choiceViewModel = ViewModelProviders.of(this.getActivity()).get(ChoiceViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@Nullable LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View statView= inflater.inflate(R.layout.fragment_statistics, container, false);
        unbinder = ButterKnife.bind(this, statView);
        isUserSignedIn = mUserManager.isLoggedIn();

        ArrayAdapter spinnerAdapter = ArrayAdapter.createFromResource(getContext(),R.array.stat_array, R.layout.spinner_item);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_item);
        GraphSpinner.setAdapter(spinnerAdapter);
        GraphSpinner.setOnItemSelectedListener(this);

        LOG.info("Statistics Fragment Created.");
        tracksLoaded = false;
        userStatsLoaded = false;
        trackStatsLoaded = false;
        subscriptions = new CompositeSubscription();

        if (!isUserSignedIn) {
            cardView.setVisibility(View.GONE);
            scrollView.setVisibility(View.GONE);
            header.setVisibility(View.VISIBLE);
            headerGradient.setVisibility(View.GONE);
            noUser.setVisibility(View.VISIBLE);
        } else {
            ProgressMessage.setVisibility(View.VISIBLE);
            headerGradient.setVisibility(View.VISIBLE);
            header.setVisibility(View.GONE);
            noUser.setVisibility(View.GONE);
            ViewPagerAdapter adapter = new ViewPagerAdapter(getChildFragmentManager());
            viewPager.setAdapter(adapter);
            tabLayout.setupWithViewPager(viewPager);
            loadDataset();
        }
        return statView;
    }

    /*
    @Override
    public void onResume() {
        super.onResume();

        if (mUserManager.isLoggedIn()) {
            LOG.info("Statistics Fragment Resumed.");
            noUser.setVisibility(View.GONE);
            ViewPagerAdapter adapter = new ViewPagerAdapter(getChildFragmentManager());
            viewPager.setAdapter(adapter);
            tabLayout.setupWithViewPager(viewPager);
            mUserManager.getUser();
            loadDataset();
        } else {
            noUser.setVisibility(View.VISIBLE);
            cardView.setVisibility(View.INVISIBLE);
            scrollView.setVisibility(View.INVISIBLE);
        }
    }
    */
    @Override
    public void onDestroyView() {
        LOG.info("onDestroyView()");
        super.onDestroyView();

        if (!subscriptions.isUnsubscribed()) {
            subscriptions.unsubscribe();
        }
        unbinder.unbind();
    }

    protected void loadDataset() {
        getStatsOfUser();
        getAllTracksFromDB();
    }

    public void getStatsOfUser(){

        subscriptions.add(mDAOProvider.getUserStatisticsDAO().getUserStatisticsObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<UserStatistics>() {
                    @Override
                    public void onStart() {
                        LOG.info("onStart() of getStatsOfUser");
                        mMainThreadWorker.schedule(() -> {
                            ProgressMessage.setVisibility(View.VISIBLE);
                        });
                    }

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error("Error: "+e.getMessage(),e);

                        if (e instanceof NotConnectedException) {
                            LOG.error("Error", e);
                            if (mUserStatistics == null) {
                                LOG.debug("No User Statistics");
                            }
                        } else if (e instanceof UnauthorizedException) {
                            LOG.error("Unauthorised",e);
                            if (mUserStatistics == null) {
                                LOG.debug("No User Statistics");
                            }
                        }
                    }

                    @Override
                    public void onNext(UserStatistics userStatistics) {
                        LOG.info("User Statistics loaded.");
                        mUserStatistics = userStatistics;
                        userStatsLoaded = Boolean.TRUE;
                        if (trackStatsLoaded == Boolean.TRUE)
                            setUserStatisticsInCard();
                    }
                }));
    }

    public void getAllTracksFromDB() {
        subscriptions.add(mDAOProvider.getTrackDAO().getTrackIdsWithLimitObservable(1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Track>>() {
                    @Override
                    public void onStart() {
                        LOG.info("onStart() of getAllTracksFromDB");
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
                            }
                        } else if (e instanceof UnauthorizedException) {
                            LOG.error("Unauthorised",e);
                            if (mTrackList.isEmpty()) {
                                LOG.debug("TrackList Empty");
                            }
                        }
                        ProgressMessage.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onNext(List<Track> trackList) {
                        LOG.info("onNext(" + trackList.size() + ") tracks stored on db");

                        for (Track track : trackList) {
                            if (!mTrackList.contains(track)) {
                                mTrackList.add(track);
                            }
                        }
                        Collections.sort(mTrackList);
                        getLastTrackStatistics();
                    }
                }));
    }

    public void getLastTrackStatistics() {

        String trackID = mTrackList.get(0).getRemoteID();
        subscriptions.add(mDAOProvider.getTrackStatisticsDAO().getTrackStatisticsObservable(trackID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<TrackStatistics>() {

                    @Override
                    public void onStart() {
                        LOG.info("onStart of getLastTrackStatistics");
                        mMainThreadWorker.schedule(() -> {

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
                            if (mTrackStatistics == null) {
                                LOG.debug("Last Track Statistics Empty");
                            }
                        } else if (e instanceof UnauthorizedException) {
                            LOG.error("Unauthorised",e);
                            if (mTrackStatistics == null) {
                                LOG.debug("Last Track Statistics Empty");
                            }
                        }
                    }

                    @Override
                    public void onNext(TrackStatistics trackStatistics) {
                        LOG.info("Last Track Statistics Loaded");
                        mTrackStatistics = trackStatistics;
                        setTrackStatisticsInCard();
                    }
                }));

    }

    public void setTrackStatisticsInCard() {
        LOG.info("Setting Track Statistics");

        Phenomenon speed = mTrackStatistics.getStatistic(TrackStatistics.KEY_USER_STAT_SPEED);
        if (speed != null) {
            trackAvgSpeed = (float)speed.getAvgValue();
            LastTrackSpeed.setText(""+TWO_DIGITS_FORMATTER.format(trackAvgSpeed) + " " + speed.getPhenomenonUnit());
        } else {
            trackAvgSpeed = null;
            LastTrackSpeed.setText("NA");
        }

        Phenomenon fuel = mTrackStatistics.getStatistic(TrackStatistics.KEY_USER_STAT_CONSUMPTION);
        if (fuel != null) {
            trackAvgFuel = (float)fuel.getAvgValue();
            LastTrackFuel.setText(""+TWO_DIGITS_FORMATTER.format(trackAvgFuel) + " " + speed.getPhenomenonUnit());
        } else {
            trackAvgFuel = null;
            LastTrackFuel.setText("NA");
        }

        if (userStatsLoaded == Boolean.TRUE)
            setUserStatisticsInCard();
    }

    public void setUserStatisticsInCard() {
        LOG.info("Setting User Statistics");

        Float temp;

        Phenomenon fuel = mUserStatistics.getStatistic(UserStatistics.KEY_USER_STAT_CONSUMPTION);
        if (fuel != null) {
            Float userAvgFuel = (float) fuel.getAvgValue();
            temp = userAvgFuel;
            if (trackAvgFuel != null) {
                temp = (trackAvgFuel - userAvgFuel) * 100f / userAvgFuel;

                if (temp < 0) {
                    temp = -temp;
                    compFuel.setImageResource(R.drawable.arrow_down);
                } else {
                    compFuel.setImageResource(R.drawable.arrow_up);
                }

                LastTrackStatFuel.setText(""+TWO_DIGITS_FORMATTER.format(temp)+" %");
                compFuel.setVisibility(View.VISIBLE);
            } else {
                LastTrackStatFuel.setText(""+TWO_DIGITS_FORMATTER.format(temp)+ " " + fuel.getPhenomenonUnit());
            }
        } else {
            LastTrackStatFuel.setText("NA");
        }

        Phenomenon speed = mUserStatistics.getStatistic(UserStatistics.KEY_USER_STAT_SPEED);
        if (speed != null) {
            Float userAvgSpeed = (float)speed.getAvgValue();
            temp = userAvgSpeed;
            if (trackAvgSpeed != null) {
                temp = (trackAvgSpeed - userAvgSpeed) * 100f / userAvgSpeed;

                if (temp < 0) {
                    temp = -temp;
                    compSpeed.setImageResource(R.drawable.arrow_down);
                } else {
                    compSpeed.setImageResource(R.drawable.arrow_up);
                }

                LastTrackStatSpeed.setText(""+TWO_DIGITS_FORMATTER.format(temp)+" %");
                compSpeed.setVisibility(View.VISIBLE);
            } else {
                LastTrackStatSpeed.setText(""+TWO_DIGITS_FORMATTER.format(temp)+ " " + speed.getPhenomenonUnit());
            }
        } else {
            LastTrackStatSpeed.setText("NA");
        }

        if (trackAvgSpeed == null && trackAvgFuel == null)
            StatInfo.setVisibility(View.INVISIBLE);

        LOG.info("Layouts set.");
        updateView();
    }

    private void updateView() {
        ProgressMessage.setVisibility(View.INVISIBLE);
        if (mTrackList.isEmpty()) {
            LOG.info("No Tracks in DB");
            noTracksCard.setVisibility(View.VISIBLE);
            TracksCard.setVisibility(View.GONE);
        } else {
            TracksCard.setVisibility(View.VISIBLE);

            TrackwDate t = new TrackwDate();
            t.getDateTime(mTrackList.get(0),1);
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMMM dd, yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm a", Locale.getDefault());
            LastTrackDate.setText(dateFormat.format(t.getDateObject()));
            LastTrackTime.setText(timeFormat.format(t.getDateObject()));

            Integer hh = Integer.parseInt(new SimpleDateFormat
                    ("HH").format(t.getDateObject()));
            if (hh < 4 || hh > 19) {
                LastTrackName.setText("Your Night Track");
            } else if (hh >= 4 && hh < 9) {
                LastTrackName.setText("Your Morning Track");
            } else if (hh > 9 && hh < 15) {
                LastTrackName.setText("Your Afternoon Track");
            } else {
                LastTrackName.setText("Your Evening Track");
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
        LOG.info("Item "+position+" clicked.");
        choiceViewModel.setSelectedOption(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }

}
