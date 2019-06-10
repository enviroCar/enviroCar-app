package org.envirocar.app.views.statistics;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.db.chart.animation.Animation;
import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.google.android.material.snackbar.Snackbar;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class GraphFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(GraphFragment.class);

    protected int position;

    @Inject
    protected UserHandler mUserManager;

    @Inject
    protected DAOProvider mDAOProvider;

    @Inject
    protected TrackDAOHandler mTrackDAOHandler;

    @Inject
    protected EnviroCarDB mEnvirocarDB;

    @BindView(R.id.stat_line_graph)
    protected LineChartView lineChartView;

    @BindView(R.id.dateButton)
    protected Button dateButton;

    @BindView(R.id.no_stats)
    protected TextView noStats;

    protected List<String> labels;
    protected ArrayList<Float> values;
    protected LineSet dataset;
    protected int mYear, mMonth, mDay, mWeek, begOfWeek, endOfWeek;
    protected CompositeSubscription subscriptions = new CompositeSubscription();
    protected List<Track> mTrackList;
    protected Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();

    private Unbinder unbinder;

    public static Fragment getInstance(int position) {
        Bundle bundle = new Bundle();
        bundle.putInt("pos", position);
        GraphFragment graphFragment = new GraphFragment();
        graphFragment.setArguments(bundle);
        return graphFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        position = getArguments().getInt("pos");
    }

    @Nullable
    @Override
    public View onCreateView(@Nullable LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stat_fragment_graph, container, false);
        unbinder = ButterKnife.bind(this, view);
        mDAOProvider = new DAOProvider(getContext());
        return view;
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        MainActivityComponent mainActivityComponent =  baseApplicationComponent.plus(new MainActivityModule(getActivity()));
        mainActivityComponent.inject(this);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Calendar c = Calendar.getInstance();
        mTrackList = new ArrayList<Track>();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        mWeek = c.get(Calendar.WEEK_OF_YEAR);
        c.set(Calendar.DAY_OF_WEEK, 1);
        begOfWeek = c.get(Calendar.DAY_OF_MONTH);
        c.set(Calendar.DAY_OF_WEEK, 7);
        endOfWeek = c.get(Calendar.DAY_OF_MONTH);
        setZeros(7);
        setLabels(0);
        if(position == 0)
        {
            String header = begOfWeek + " - " + endOfWeek + " " + new SimpleDateFormat
                                                            ("MMMM").format(c.getTime());
            dateButton.setText(header);
        }
        else if(position == 1)
        {
            String header = new SimpleDateFormat("MMMM yy").format(c.getTime());
            dateButton.setText(header);
        }
        else if(position == 2)
        {
            String header = new SimpleDateFormat("yyyy").format(c.getTime());
            dateButton.setText(header);
        }
        loadData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    public void loadData(){
        if(position == 0){
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_WEEK, begOfWeek);
            cal.set(Calendar.WEEK_OF_YEAR, mWeek);
            cal.set(Calendar.YEAR, mYear);
            Date after = cal.getTime();
            cal.set(Calendar.DAY_OF_WEEK, endOfWeek);
            cal.set(Calendar.WEEK_OF_YEAR, mWeek);
            cal.set(Calendar.YEAR, mYear);
            Date before = cal.getTime();
            getData(after, before);
        }

    }

    public void setGraph()
    {
        if(mTrackList.size()==0)
        {
            noStats.setVisibility(View.VISIBLE);
            lineChartView.setVisibility(View.INVISIBLE);
        }
        else {
            try {
                showSnackbar(mTrackList.size() + "");
            } catch (Exception e) {
                LOG.error("Error: ", e);
            }
            TrackwDate t = new TrackwDate();
            if (position == 0) {
                setLabels(position);
                setZeros(7);
                for (int i = 0; i < mTrackList.size(); ++i) {
                    try {
                        t.getDateTime(mTrackList.get(i));
                        values.set(t.getDay() - 1, values.get(t.getDay() - 1) + Float.valueOf(mTrackList.get(i).getDuration()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                values.subList(7, values.size()).clear();
                labels.subList(7, values.size()).clear();
            }


            dataset = new LineSet(labels.toArray(new String[0]), convertArrayList());
            dataset.setColor(Color.parseColor("#36759B"));
            dataset.setSmooth(Boolean.TRUE);
            dataset.setThickness(7f);
            float intervals[] = {0f, 0.8f};
            int colors[] = {Color.parseColor("#9EC9E2"), Color.WHITE};
            //dataset.setDashed(intervals);
            dataset.setGradientFill(colors, intervals);
            lineChartView.addData(dataset);
            Animation animation = new Animation();
            animation.setDuration(1000);
            lineChartView.show();
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

    public void getData(Date after, Date before)
    {
        subscriptions.add(mDAOProvider.getTrackDAO().getTrackIdsObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Track>>() {

                    @Override
                    public void onStart() {
                        LOG.info("onStart() tracks in db");
                        mMainThreadWorker.schedule(() -> {
                            //ProgressMessage.setVisibility(View.VISIBLE);
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
                            showSnackbar(R.string.track_list_bg_unauthorized);
                            if (mTrackList.isEmpty()) {
                                LOG.debug("TrackList Empty");
                                showSnackbar(R.string.track_list_bg_unauthorized);
                            }
                        }

                    }

                    @Override
                    public void onNext(List<Track> tracks) {
                        LOG.info("onNext(" + tracks.size() + ") remotely stored tracks");

                        for (Track track : tracks) {
                            if (!mTrackList.contains(track)) {
                                mTrackList.add(track);
                            }
                        }
                        try
                        {
                            showSnackbar(mTrackList.size()+"");
                        }catch (Exception e)
                        {
                            LOG.error("Error: ", e);
                        }
                        setGraph();
                    }
                }));
    }

    @OnClick(R.id.dateButton)
    public void setDate()
    {
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {
                        mYear = year;
                        mMonth = monthOfYear;
                        mDay = dayOfMonth;
                        Calendar c = Calendar.getInstance();
                        c.set(Calendar.YEAR,mYear);
                        c.set(Calendar.MONTH, mMonth);
                        c.set(Calendar.DAY_OF_MONTH, mDay);
                        mWeek = c.get(Calendar.WEEK_OF_YEAR);
                        c.set(Calendar.DAY_OF_WEEK, 1);
                        begOfWeek = c.get(Calendar.DAY_OF_MONTH);
                        c.set(Calendar.DAY_OF_WEEK, 7);
                        endOfWeek = c.get(Calendar.DAY_OF_MONTH);
                        loadData();
                    }
                }, mYear, mMonth, mDay);
        datePickerDialog.show();
    }

    public void setZeros(int n)
    {
        values = new ArrayList<Float>(Collections.nCopies(n, 0f));
    }

    public void setLabels(int choice)
    {
        if(choice == 0)
            labels = Arrays.asList(new  String[]{"Sun", "Mon", "Tue", "Wed", "Thur", "Fri", "Sat"});
        else if(choice == 1)
            for(int i=1;i<=31;++i)
                labels.add(i+"");
        else
            labels = Arrays.asList(new  String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul",
                                                                "Aug", "Sep", "Oct", "Nov", "Dec"});

    }

    public float[] convertArrayList()
    {
        float[] floatArray = new float[values.size()];
        int i = 0;

        for (Float f : values) {
            floatArray[i++] = (f != null ? f : Float.NaN); // Or whatever default you want.
        }

        return floatArray;
    }
}
