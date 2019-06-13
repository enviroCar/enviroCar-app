package org.envirocar.app.views.statistics;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.icu.text.DecimalFormat;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.db.chart.animation.Animation;
import com.db.chart.model.LineSet;
import com.db.chart.renderer.AxisRenderer;
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
import org.envirocar.core.trackprocessing.TrackStatisticsProvider;
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

public class GraphFragment extends BaseInjectorFragment implements StatisticsFragment.SpinnerEventListener {
    private static final Logger LOG = Logger.getLogger(GraphFragment.class);

    protected int position;
    protected int choice;

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

    @BindView(R.id.no_stats_img)
    protected ImageView noStatsImg;

    @BindView(R.id.arrow_left)
    protected ImageButton arrowLeft;

    @BindView(R.id.arrow_right)
    protected ImageButton arrowRight;

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
        LOG.info("Graph Created");
        super.onViewCreated(view, savedInstanceState);
        Calendar c = Calendar.getInstance();
        mTrackList = new ArrayList<Track>();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        mWeek = c.get(Calendar.WEEK_OF_YEAR);
        begOfWeek = getWeekStartDate(c.getTime()).getDate();
        endOfWeek = getWeekEndDate(c.getTime()).getDate();
        choice = 0;
        setZeros();
        setLabels();
        setDateSelectorButton(c);
        loadData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    public void loadData(){
        Calendar cal = Calendar.getInstance();
        Date after = cal.getTime(), before = cal.getTime();
        if(position == 0){
            cal.set(mYear,mMonth,mDay);
            after = getWeekStartDate(cal.getTime());
            before = getWeekEndDate(cal.getTime());

        }
        else if(position ==1)
        {
            cal.set(mYear,mMonth,1);
            after = cal.getTime();
            cal.set(mYear,mMonth+1,1);
            before = cal.getTime();
        }
        else if(position ==2)
        {
            cal.set(mYear,0,1);
            after = cal.getTime();
            cal.set(mYear+1,0,1);
            before = cal.getTime();
        }
        getData(after, before);
    }

    public void setGraph()
    {
        if(mTrackList.size()==0)
        {
            noStats.setVisibility(View.VISIBLE);
            noStatsImg.setVisibility(View.VISIBLE);
            lineChartView.setVisibility(View.INVISIBLE);
            LOG.info("mTracklist has zero elements");
        }
        else {
            noStats.setVisibility(View.INVISIBLE);
            noStatsImg.setVisibility(View.INVISIBLE);
            lineChartView.setVisibility(View.VISIBLE);
            LOG.info(mTrackList.size() + " Tracks in range");

            TrackwDate t = new TrackwDate();
            setLabels();
            setZeros();

            for (int i = 0; i < mTrackList.size(); ++i) {
                try {
                    Track temp = mTrackList.get(i);
                    t.getDateTime(temp);
                    int index;
                    if(position == 0)
                        index = t.getDay() - 1;
                    else if(position == 1)
                        index = t.getDate() - 1;
                    else
                        index = t.getMonth();
                    if(choice == 0)
                        values.set(index, values.get(index) + 1 );
                    else if(choice == 1)
                        values.set(index, values.get(index) + temp.getLength() );

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            dataset = new LineSet(labels.toArray(new String[0]), convertArrayList());
            dataset.setColor(Color.parseColor("#8036759B"));
            dataset.setDotsRadius(10f);
            dataset.setDotsColor(Color.WHITE);
            dataset.setDotsStrokeThickness(4f);
            dataset.setDotsStrokeColor(Color.parseColor("#36759B"));
            dataset.setThickness(5f);
            float intervals[] = {0f, 0.9f};
            int colors[] = {Color.parseColor("#9EC9E2"), Color.TRANSPARENT};
            dataset.setGradientFill(colors, intervals);
            lineChartView.reset();
            lineChartView.addData(dataset);
            Animation animation = new Animation();
            animation.setDuration(1500);
            lineChartView.setXAxis(Boolean.FALSE);
            lineChartView.setYAxis(Boolean.FALSE);
            lineChartView.setLabelsColor(Color.parseColor("#9EC9E2"));
            if(position == 1)
                lineChartView.setXLabels(AxisRenderer.LabelPosition.NONE);
            lineChartView.setAxisLabelsSpacing(30);
            lineChartView.show(animation);
        }
    }

    public void getData(Date after, Date before)
    {
        subscriptions.add(mDAOProvider.getTrackDAO().getTrackinPeriodObservable(after, before)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Track>>() {

                    @Override
                    public void onStart() {
                        LOG.info("onStart() of getData");
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
                            if (mTrackList.isEmpty()) {
                                LOG.debug("TrackList Empty");
                            }
                        } else if (e instanceof UnauthorizedException) {
                            LOG.error("Unauthorised",e);
                            if (mTrackList.isEmpty()) {
                                LOG.debug("TrackList Empty");
                            }
                        }
                    }

                    @Override
                    public void onNext(List<Track> tracks) {
                        LOG.info("onNext(" + tracks.size() + ") tracks loaded in GraphFragment");
                        mTrackList.clear();
                        for (Track track : tracks) {
                                if (!mTrackList.contains(track)) {
                                    mTrackList.add(track);
                                }
                            }
                        setGraph();
                    }
                }));
    }

    @Override
    public void itemClick(int dataChoice){
        LOG.info(dataChoice + " received");
        choice = dataChoice;
        loadData();
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
                        begOfWeek = getWeekStartDate(c.getTime()).getDate();
                        endOfWeek = getWeekEndDate(c.getTime()).getDate();
                        LOG.info("mWeek: "+mWeek+" mYear: "+mYear+" mMonth: "+mMonth+" mDay: "+mDay);
                        setDateSelectorButton(c);
                        loadData();
                    }
                }, mYear, mMonth, mDay);
        datePickerDialog.show();
    }

    public void setZeros()
    {
        int n;
        if(position == 0)
            n=7;
        else if(position == 1)
            n=31;
        else
            n=12;
        values = new ArrayList<Float>(Collections.nCopies(n, 0f));
    }

    public void setLabels()
    {
        String weekdays[] = {"Sun", "Mon", "Tue", "Wed", "Thur", "Fri", "Sat"};
        String months[] = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul",
                "Aug", "Sep", "Oct", "Nov", "Dec"};
        labels = new ArrayList<>();
        if(position == 0)
            labels = Arrays.asList(weekdays);
        else if(position == 1)
            for(int i=1;i<=31;++i)
                labels.add(i+"");
        else
            labels = Arrays.asList(months);

    }

    public float[] convertArrayList()
    {
        float[] floatArray = new float[values.size()];
        int i = 0;

        for (Float f : values) {
            floatArray[i++] = (f != null ? f : Float.NaN);
        }

        return floatArray;
    }

    public static Date getWeekStartDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            calendar.add(Calendar.DATE, -1);
        }
        return calendar.getTime();
    }

    public static Date getWeekEndDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            calendar.add(Calendar.DATE, 1);
        }
        calendar.add(Calendar.DATE, -1);
        return calendar.getTime();
    }

    public void setDateSelectorButton(Calendar c){
        if(position == 0)
        {
            String header = begOfWeek + " - " + endOfWeek + " " + new SimpleDateFormat
                    ("MMMM").format(c.getTime());
            dateButton.setText(header);
        }
        else if(position == 1)
        {
            String header = new SimpleDateFormat("MMMM yyyy").format(c.getTime());
            dateButton.setText(header);
        }
        else if(position == 2)
        {
            String header = new SimpleDateFormat("yyyy").format(c.getTime());
            dateButton.setText(header);
        }
    }

    @OnClick(R.id.arrow_left)
    public void moveLeft(){
        Calendar c = Calendar.getInstance();
        if(position == 0)
        {
            c.set(mYear,mMonth,mDay);
            c.add(Calendar.DAY_OF_YEAR, -7);
            mYear = c.get(Calendar.YEAR);
            mMonth = c.get(Calendar.MONTH);
            mDay = c.get(Calendar.DAY_OF_MONTH);
            mWeek = c.get(Calendar.WEEK_OF_YEAR);
            begOfWeek = getWeekStartDate(c.getTime()).getDate();
            endOfWeek = getWeekEndDate(c.getTime()).getDate();
        }
        else if(position == 1)
        {
            mMonth--;
            c.set(mYear,mMonth,mDay);
        }
        else
        {
            mYear--;
            c.set(mYear,mMonth,mDay);
        }

        setDateSelectorButton(c);
        loadData();
    }

    @OnClick(R.id.arrow_right)
    public void moveRight(){
        Calendar c = Calendar.getInstance();
        if(position == 0)
        {
            c.set(mYear,mMonth,mDay);
            c.add(Calendar.DAY_OF_YEAR, 7);
            mYear = c.get(Calendar.YEAR);
            mMonth = c.get(Calendar.MONTH);
            mDay = c.get(Calendar.DAY_OF_MONTH);
            mWeek = c.get(Calendar.WEEK_OF_YEAR);
            begOfWeek = getWeekStartDate(c.getTime()).getDate();
            endOfWeek = getWeekEndDate(c.getTime()).getDate();
        }
        else if(position == 1)
        {
            mMonth++;
            c.set(mYear,mMonth,mDay);
        }
        else
        {
            mYear++;
            c.set(mYear,mMonth,mDay);
        }
        setDateSelectorButton(c);
        loadData();
    }


}
