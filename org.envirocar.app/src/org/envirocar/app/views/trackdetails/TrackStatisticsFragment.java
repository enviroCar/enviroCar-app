package org.envirocar.app.views.trackdetails;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Fade;
import androidx.transition.TransitionManager;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.R;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.TrackDAOHandler;
import org.envirocar.app.handler.UserHandler;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.main.MainActivityComponent;
import org.envirocar.app.main.MainActivityModule;
import org.envirocar.core.entity.GlobalStatistics;
import org.envirocar.core.entity.TrackStatistics;
import org.envirocar.core.entity.UserStatistics;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.logging.Logger;
import org.envirocar.storage.EnviroCarDB;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class TrackStatisticsFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(TrackStatisticsFragment.class);

    private String[] statChoices = {TrackStatistics.KEY_USER_STAT_CONSUMPTION,
            TrackStatistics.KEY_USER_STAT_SPEED, TrackStatistics.KEY_USER_STAT_CO2,
            TrackStatistics.KEY_USER_STAT_LOAD, TrackStatistics.KEY_USER_STAT_RPM};

    private int[] statImgs = {R.drawable.consumption, R.drawable.speed, R.drawable.ic_co2_emission, R.drawable.load, R.drawable.speed};
    private static final DecimalFormat DECIMAL_FORMATTER_TWO_DIGITS = new DecimalFormat("#.##");

    @Inject
    protected UserHandler mUserManager;

    @Inject
    protected DAOProvider mDAOProvider;

    @Inject
    protected TrackDAOHandler mTrackDAOHandler;

    @Inject
    protected EnviroCarDB mEnvirocarDB;

    @BindView(R.id.layout)
    protected ConstraintLayout layout;
    @BindView(R.id.recyclerView)
    protected RecyclerView recyclerView;
    @BindView(R.id.infoButton)
    protected ImageView infoButton;
    @BindView(R.id.noTrackStats)
    protected TextView noTrackStats;
    @BindView(R.id.loadingStats)
    protected TextView loadingStats;
    @BindView(R.id.errorLoading)
    protected TextView errorLoadingStats;

    private List<TrackStatisticsDataHolder> trackStatisticsDataHolderList = new ArrayList<>();
    private TrackStatisticsAdapter adapter;
    private CompositeSubscription subscription;
    private Unbinder unbinder;
    private Boolean trackStats = false, userStats = false, globalStats = false;
    private String ID;
    private TrackStatistics trackStatistics;
    private GlobalStatistics globalStatistics;
    private UserStatistics userStatistics;
    private CardInterface cardInterface;

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        MainActivityComponent mainActivityComponent =  baseApplicationComponent.plus(new MainActivityModule(getActivity()));
        mainActivityComponent.inject(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            cardInterface = (CardInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement CardInterface");
        }
    }

    @Nullable
    @Override
    public android.view.View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final Bundle args = getArguments();
        ID = args.getString("ID");
        View rootView = inflater.inflate(R.layout.fragment_track_statistics, container, false);
        unbinder = ButterKnife.bind(this, rootView);

        adapter = new TrackStatisticsAdapter(trackStatisticsDataHolderList);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        subscription = new CompositeSubscription();
        loadData();
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                        .customView(R.layout.track_details_dialog, false)
                        .cancelable(true)
                        .show();
            }
        });
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (!subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
        unbinder.unbind();
    }


    public void loadData() {
        TransitionManager.beginDelayedTransition(layout, new Fade());
        loadingStats.setVisibility(View.VISIBLE);
        errorLoadingStats.setVisibility(View.GONE);
        noTrackStats.setVisibility(View.GONE);
        getTrackStatistics();
        getUserStatistics();
        getGlobalStatistics();
    }

    private void getTrackStatistics() {
        String trackID = ID;
        subscription.add(mDAOProvider.getTrackStatisticsDAO().getTrackStatisticsObservable(trackID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<TrackStatistics>() {
                    @Override
                    public void onStart() {
                        LOG.info("onStart() of getTrackStatistics with " + trackID );
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
                        loadingStats.setVisibility(View.INVISIBLE);
                        errorLoadingStats.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onNext(TrackStatistics temp) {
                        LOG.info("Track Statistics loaded.");
                        trackStats = true;
                        trackStatistics = temp;
                        setTrackStatisticsDataHolderList();
                    }
                }));
    }

    private void getUserStatistics() {
        subscription.add(mDAOProvider.getUserStatisticsDAO().getUserStatisticsObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<UserStatistics>() {
                    @Override
                    public void onStart() {
                        LOG.info("onStart() of getUserStatistics");
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
                        loadingStats.setVisibility(View.INVISIBLE);
                        errorLoadingStats.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onNext(UserStatistics temp) {
                        LOG.info("User Statistics loaded.");
                        userStats = true;
                        userStatistics = temp;
                        setTrackStatisticsDataHolderList();
                    }
                }));
    }

    private void getGlobalStatistics() {
        subscription.add(mDAOProvider.getGlobalStatisticsDAO().getGlobalStatisticsObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<GlobalStatistics>() {

                    @Override
                    public void onStart(){
                        LOG.info("onStart() of getGlobalStatistics");
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
                        loadingStats.setVisibility(View.INVISIBLE);
                        errorLoadingStats.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onNext(GlobalStatistics temp) {
                        LOG.info("Global Statistics loaded.");
                        globalStats = true;
                        globalStatistics = temp;
                        setTrackStatisticsDataHolderList();
                    }
                }));
    }

    private void setTrackStatisticsDataHolderList() {
        trackStatisticsDataHolderList.clear();
        if (trackStats && userStats && globalStats) {
            loadingStats.setVisibility(View.INVISIBLE);
            int j = 0;
            for (int i = 0; i < statChoices.length; ++i) {
                if (trackStatistics.getStatistic(statChoices[i]) != null) {
                    TrackStatisticsDataHolder temp = new TrackStatisticsDataHolder();

                    temp.setTrackAvg(Float.valueOf(DECIMAL_FORMATTER_TWO_DIGITS.format(trackStatistics.getStatistic(statChoices[i]).getAvgValue())));
                    temp.setTrackMax(Float.valueOf(DECIMAL_FORMATTER_TWO_DIGITS.format(trackStatistics.getStatistic(statChoices[i]).getMaxValue())));
                    temp.setPhenomena(trackStatistics.getStatistic(statChoices[i]).getPhenomenonName());
                    temp.setUnit(trackStatistics.getStatistic(statChoices[i]).getPhenomenonUnit());
                    temp.setDisplayUserAndGlobalAvg(true);

                    if (userStatistics.getStatistic(statChoices[i]) == null || globalStatistics.getStatistic(statChoices[i]) == null)
                        temp.setDisplayUserAndGlobalAvg(false);

                    if (statChoices[i].equalsIgnoreCase(TrackStatistics.KEY_USER_STAT_LOAD) || statChoices[i].equalsIgnoreCase(TrackStatistics.KEY_USER_STAT_RPM))
                        temp.setDisplayUserAndGlobalAvg(false);

                    if (userStatistics.getStatistic(statChoices[i]) != null)
                        temp.setUserAvg(Float.valueOf(DECIMAL_FORMATTER_TWO_DIGITS.format(userStatistics.getStatistic(statChoices[i]).getAvgValue())));
                    
                    if(globalStatistics.getStatistic(statChoices[i]) != null)
                        temp.setGlobalAvg(Float.valueOf(DECIMAL_FORMATTER_TWO_DIGITS.format(globalStatistics.getStatistic(statChoices[i]).getAvgValue())));

                    temp.setResImg(statImgs[i]);
                    trackStatisticsDataHolderList.add(temp);
                    LOG.debug(trackStatisticsDataHolderList.get(j).getPhenomena());
                    j++;
                }
            }
            if (j == 0)
                noTrackStats.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
            cardInterface.setCardViewHeight(recyclerView.getHeight());
        }
    }

    public interface CardInterface{
        void setCardViewHeight(int height);
    }
}
