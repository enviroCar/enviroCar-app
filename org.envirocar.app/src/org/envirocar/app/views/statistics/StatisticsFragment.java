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

    // Views for the LastTrack layout
    @BindView(R.id.tracks_card)
    protected ConstraintLayout tracksCard;

    @BindView(R.id.stat_last_track_header)
    protected TextView LastTrackHeader;

    @BindView(R.id.stat_track_name)
    protected TextView LastTrackName;

    @BindView(R.id.stat_track_time)
    protected TextView LastTrackTime;

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

    @BindView(R.id.stat_info)
    protected TextView StatInfo;

    @BindView(R.id.stat_img_comp_fuel)
    protected ImageView compFuel;

    @BindView(R.id.stat_img_comp_speed)
    protected ImageView compSpeed;

    //Other Card layouts
    @BindView(R.id.stat_card)
    protected CardView cardView;

    @BindView(R.id.loading_card)
    protected ConstraintLayout loadingCard;

    @BindView(R.id.message_card)
    protected ConstraintLayout messageCard;

    @BindView(R.id.message_header)
    protected TextView messageHeader;

    @BindView(R.id.message_desc)
    protected TextView messageDesc;

    @BindView(R.id.no_user_layout)
    protected ConstraintLayout noUserCard;

    // Background images
    @BindView(R.id.header_gradient)
    protected ImageView headerGradient;

    @BindView(R.id.header)
    protected ImageView header;

    // Other Views
    @BindView(R.id.stat_scrollView)
    protected NestedScrollView scrollView;

    @BindView(R.id.stat_graph_spinner)
    protected Spinner GraphSpinner;

    @BindView(R.id.stat_tabLayout)
    protected TabLayout tabLayout;

    @BindView(R.id.stat_viewPager)
    protected ViewPager viewPager;

    protected boolean isUserSignedIn;
    protected boolean tracksLoaded = false;
    protected boolean userStatsLoaded = false;
    protected boolean trackStatsLoaded = false;
    protected Track lastTrack;
    protected UserStatistics mUserStatistics;
    protected TrackStatistics mTrackStatistics;
    protected Float trackAvgSpeed;
    protected Float trackAvgFuel;
    protected ChoiceViewModel choiceViewModel;

    protected CompositeSubscription subscriptions ;
    protected Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    protected Unbinder unbinder;


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

        tracksLoaded = false;
        userStatsLoaded = false;
        trackStatsLoaded = false;
        subscriptions = new CompositeSubscription();

        if (!isUserSignedIn) {
            cardView.setVisibility(View.GONE);
            scrollView.setVisibility(View.GONE);
            header.setVisibility(View.VISIBLE);
            headerGradient.setVisibility(View.GONE);
            noUserCard.setVisibility(View.VISIBLE);
        } else {
            loadingCard.setVisibility(View.VISIBLE);
            headerGradient.setVisibility(View.VISIBLE);
            header.setVisibility(View.GONE);
            noUserCard.setVisibility(View.GONE);
            ViewPagerAdapter adapter = new ViewPagerAdapter(getChildFragmentManager());
            viewPager.setAdapter(adapter);
            tabLayout.setupWithViewPager(viewPager);
            loadData();
        }
        return statView;
    }

    /*
    @Override
    public void onResume() {
        super.onResume();

        if (mUserManager.isLoggedIn()) {
            LOG.info("Statistics Fragment Resumed.");
            noUserCard.setVisibility(View.GONE);
            ViewPagerAdapter adapter = new ViewPagerAdapter(getChildFragmentManager());
            viewPager.setAdapter(adapter);
            tabLayout.setupWithViewPager(viewPager);
            mUserManager.getUser();
            loadData();
        } else {
            noUserCard.setVisibility(View.VISIBLE);
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

    protected void loadData() {
        getStatsOfUser();
        getLastRemoteTrack();
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
                            loadingCard.setVisibility(View.VISIBLE);
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
                        } else if (e instanceof UnauthorizedException) {
                            LOG.error("Unauthorised",e);
                        }

                        messageHeader.setText(R.string.stats_error_header);
                        messageDesc.setText(R.string.stats_error_desc);
                        messageCard.setVisibility(View.VISIBLE);
                        tracksCard.setVisibility(View.GONE);
                        loadingCard.setVisibility(View.GONE);
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

    public void getLastRemoteTrack() {
        subscriptions.add(mDAOProvider.getTrackDAO().getTrackIdsWithLimitObservable(1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Track>>() {
                    @Override
                    public void onStart() {
                        LOG.info("onStart() of getLastRemoteTrack");
                        mMainThreadWorker.schedule(() -> {
                            loadingCard.setVisibility(View.VISIBLE);
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
                        } else if (e instanceof UnauthorizedException) {
                            LOG.error("Unauthorised",e);
                        }

                        messageHeader.setText(R.string.stats_error_header);
                        messageDesc.setText(R.string.stats_error_desc);
                        messageCard.setVisibility(View.VISIBLE);
                        tracksCard.setVisibility(View.GONE);
                        loadingCard.setVisibility(View.GONE);
                    }

                    @Override
                    public void onNext(List<Track> trackList) {
                        if (trackList != null && trackList.size() != 0)
                            lastTrack = trackList.get(0);
                        else
                            lastTrack = null;
                        getLastTrackStatistics();
                    }
                }));
    }

    public void getLastTrackStatistics() {

        String trackID = lastTrack.getRemoteID();
        subscriptions.add(mDAOProvider.getTrackStatisticsDAO().getTrackStatisticsObservable(trackID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<TrackStatistics>() {

                    @Override
                    public void onStart() {
                        LOG.info("onStart of getLastTrackStatistics");
                    }

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error(e.getMessage(), e);

                        if (e instanceof NotConnectedException) {
                            LOG.error("Error", e);
                        } else if (e instanceof UnauthorizedException) {
                            LOG.error("Unauthorised",e);
                        }

                        messageHeader.setText(R.string.stats_error_header);
                        messageDesc.setText(R.string.stats_error_desc);
                        messageCard.setVisibility(View.VISIBLE);
                        tracksCard.setVisibility(View.GONE);
                        loadingCard.setVisibility(View.GONE);
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

        updateView();
    }

    private void updateView() {
        loadingCard.setVisibility(View.INVISIBLE);

        if (lastTrack == null) {
            messageHeader.setText(R.string.stat_no_tracks);
            messageDesc.setText(R.string.stats_no_tracks_desc);
            messageCard.setVisibility(View.VISIBLE);
            tracksCard.setVisibility(View.GONE);
        } else {
            messageCard.setVisibility(View.GONE);
            tracksCard.setVisibility(View.VISIBLE);

            TrackDateUtil t = new TrackDateUtil(lastTrack);
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMMM dd, yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("KK:mm a", Locale.getDefault());
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
