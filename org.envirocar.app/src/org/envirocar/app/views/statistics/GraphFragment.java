package org.envirocar.app.views.statistics;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
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
import androidx.lifecycle.ViewModelProviders;

import org.envirocar.app.R;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.TrackDAOHandler;
import org.envirocar.app.handler.UserHandler;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.main.MainActivityComponent;
import org.envirocar.app.main.MainActivityModule;
import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.TrackStatistics;
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
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class GraphFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(GraphFragment.class);

    protected int position;
    protected int choice;
    protected int iteration;

    @Inject
    protected UserHandler mUserManager;

    @Inject
    protected DAOProvider mDAOProvider;

    @Inject
    protected TrackDAOHandler mTrackDAOHandler;

    @Inject
    protected EnviroCarDB mEnvirocarDB;

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

    @BindView(R.id.chart)
    protected lecho.lib.hellocharts.view.LineChartView mChart;

    protected List<String> labels;
    protected List<String> labelsX;
    protected ArrayList<Float> values;
    protected ArrayList<Float> noOfTracks;
    protected int mYear, mMonth, mDay, mWeek, begOfWeek, endOfWeek;
    protected CompositeSubscription subscriptions = new CompositeSubscription();
    protected List<Track> mTrackList = new ArrayList<>();
    protected List<Track> persistentTrackList = new ArrayList<>();
    protected TrackStatistics mTrackStatistics;
    protected Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    private Unbinder unbinder;
    private ChoiceViewModel choiceViewModel;
    private Context context;

    private LineChartData mChartData;
    List<PointValue> valuesHello = new ArrayList<>();
    List<AxisValue> labelsHello = new ArrayList<>();

    public static Fragment getInstance(int position) {
        Bundle bundle = new Bundle();
        bundle.putInt("pos", position);
        GraphFragment graphFragment = new GraphFragment();
        graphFragment.setArguments(bundle);
        return graphFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        MainActivityComponent mainActivityComponent =  baseApplicationComponent.plus(new MainActivityModule(getActivity()));
        mainActivityComponent.inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOG.info("onCreate");
        choiceViewModel = ViewModelProviders.of(this.getActivity()).get(ChoiceViewModel.class);
        choiceViewModel.getSelectedOption().observe(this, item -> {
            LOG.info("choiceViewModel.getSelectedOption()");
            choice = item;
            loadGraph();
        });
    }

    @Nullable
    @Override
    public View onCreateView(@Nullable LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        LOG.info("onCreateView Position: " + position);
        View view = inflater.inflate(R.layout.stat_fragment_graph, container, false);
        position = getArguments().getInt("pos");
        unbinder = ButterKnife.bind(this, view);
        mDAOProvider = new DAOProvider(context);
        choice = 0;
        cleanUpData();
        return view;
    }

    public void loadGraph(){
        cleanUpData();
        if(persistentTrackList.size() == 0)
            getData();
        else
            loadDates();
    }

    public void cleanUpData(){
        Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        mWeek = c.get(Calendar.WEEK_OF_YEAR);
        begOfWeek = getWeekStartDate(c.getTime()).getDate();
        endOfWeek = getWeekEndDate(c.getTime()).getDate();
        iteration = 0;
        setZeros();
        setLabels();
        setDateSelectorButton(c);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    public void setZeros()
    {
        valuesHello.clear();
        int n;
        if(position == 0)
            n=7;
        else if(position == 1)
            n=31;
        else
            n=12;
        for(int i=0; i<n; ++i)
            valuesHello.add(new PointValue(i,0));
        values = new ArrayList<Float>(Collections.nCopies(n, 0f));
        noOfTracks = new ArrayList<Float>(Collections.nCopies(n, 0f));
    }

    public void setLabels()
    {
        labelsHello.clear();
        String weekdays[] = {"Sun", "Mon", "Tue", "Wed", "Thur", "Fri", "Sat"};
        String months[] = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul",
                "Aug", "Sep", "Oct", "Nov", "Dec"};
        labels = new ArrayList<>();
        String x[] = {"Days", "Months", "Years"};
        String y[] = {"Days", "Months", "Years"};
        if(position == 0)
            labels = Arrays.asList(weekdays);
        else if(position == 1)
            for(int i=1;i<=31;++i)
                labels.add(i+"");
        else
            labels = Arrays.asList(months);
        for(int i = 0; i<labels.size(); ++i){
            labelsHello.add(new AxisValue(i).setLabel(labels.get(i)));
        }
        labelsX = Arrays.asList(x);
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

    public void getData()
    {
        subscriptions.add(mDAOProvider.getTrackDAO().getTrackIdsObservable()
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
                            if (persistentTrackList.isEmpty()) {
                                LOG.debug("TrackList Empty");
                            }
                        } else if (e instanceof UnauthorizedException) {
                            LOG.error("Unauthorised",e);
                            if (persistentTrackList.isEmpty()) {
                                LOG.debug("TrackList Empty");
                            }
                        }
                    }

                    @Override
                    public void onNext(List<Track> tracks) {
                        LOG.info("onNext(" + tracks.size() + ") tracks loaded in GraphFragment");
                        for (Track track : tracks) {
                            if (!persistentTrackList.contains(track)) {
                                persistentTrackList.add(track);
                            }
                        }
                        loadDates();
                    }
                }));
    }

    public void setTrackList(){
        for(Track track : persistentTrackList){
            if(!mTrackList.contains(track)){
                mTrackList.add(track);
            }
        }
    }


    public void loadDates(){
        setTrackList();
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
        trimTacksToRange(after, before);
    }

    public void trimTacksToRange(Date start, Date end){
        for(int i = 0; i<mTrackList.size(); ++i){
            TrackwDate t = new TrackwDate();
            t.getDateTime(mTrackList.get(i));
            if(!(t.getDateObject().after(start) && t.getDateObject().before(end)))
            {
                mTrackList.remove(i);
                i--;
            }
        }

        setGraph();
    }

    public void setGraph()
    {
        if(mTrackList.size()==0)
        {
            noStats.setVisibility(View.VISIBLE);
            noStatsImg.setVisibility(View.VISIBLE);
            mChart.setVisibility(View.INVISIBLE);
            LOG.info("mTracklist has zero elements");
        }
        else {
            noStats.setVisibility(View.INVISIBLE);
            noStatsImg.setVisibility(View.INVISIBLE);
            mChart.setVisibility(View.VISIBLE);
            LOG.info(mTrackList.size() + " Tracks in range");

            TrackwDate t = new TrackwDate();
            setLabels();
            setZeros();

            if(choice!=2)
            {
                for (int i = 0; i < mTrackList.size(); ++i) {
                    try {
                        Track temp = mTrackList.get(i);
                        t.getDateTime(temp);
                        int index;
                        if (position == 0)
                            index = t.getDay() - 1;
                        else if (position == 1)
                            index = t.getDate() - 1;
                        else
                            index = t.getMonth();

                        if (choice == 0)
                        {
                            values.set(index, values.get(index) + 1);

                        }
                        else if (choice == 1)
                        {
                            values.set(index, values.get(index) + temp.getLength().floatValue());
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                setGraphOptionsAndShow();
            }
            else
            {
                getTrackStatistics();
            }
        }
    }

    public void getTrackStatistics(){

        String trackID = mTrackList.get(iteration).getRemoteID();
        Track temp = mTrackList.get(iteration);
        TrackwDate t = new TrackwDate();
        t.getDateTime(temp);
        int index;
        if (position == 0)
            index = t.getDay() - 1;
        else if (position == 1)
            index = t.getDate() - 1;
        else
            index = t.getMonth();

        subscriptions.add(mDAOProvider.getTrackStatisticsDAO().getTrackStatisticsObservable(trackID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<TrackStatistics>() {

                    @Override
                    public void onStart() {
                        LOG.info("onStart() of getTrackStatistics with " + trackID +" at index: " + index);
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
                                LOG.debug("TrackStatistics Empty");
                            }
                        } else if (e instanceof UnauthorizedException) {
                            LOG.error("Unauthorised",e);
                            if (mTrackStatistics == null) {
                                LOG.debug("TrackStatistics Empty");
                            }
                        }
                    }

                    @Override
                    public void onNext(TrackStatistics trackStatistics) {
                        mTrackStatistics = trackStatistics;
                        noOfTracks.set(index, noOfTracks.get(index) + 1);

                        LOG.info("Statistics load with " + getTrackStatData());
                        LOG.info("Value of " + index + " = " + values.get(index));
                        LOG.info("No of Tracks at index " + index + " = " + noOfTracks.get(index));
                        iteration++;

                        if(iteration<mTrackList.size())
                            getTrackStatistics();
                        else
                        {
                            for (int i = 0; i < values.size(); ++i) {
                                if (values.get(i) != 0)
                                    values.set(i, values.get(i) / noOfTracks.get(i));
                                }

                            setGraphOptionsAndShow();
                        }
                    }
                }));
    }

    public Float getTrackStatData(){
        return (float) mTrackStatistics.getStatistic(TrackStatistics.KEY_USER_STAT_SPEED).getAvgValue();
    }

    public void setGraphOptionsAndShow()
    {
        mChart.cancelDataAnimation();
        Line line = new Line(valuesHello);
        line.setCubic(false).setColor(Color.parseColor("#36759B"))
                .setStrokeWidth(2)
                .setPointRadius(3)
                .setPointColor(Color.parseColor("#36759B"));

        List<Line> lines = new ArrayList<>();
        lines.add(line);
        int i = 0;
        for(PointValue value : line.getValues()){
            value.setTarget(value.getX(), values.get(i));
            i++;
        }
        mChartData = new LineChartData(lines);
        mChartData.setAxisXBottom(new Axis(labelsHello).setHasLines(true).setName(labelsX.get(position))
                .setTextColor(Color.parseColor("#800065A0")));
        mChartData.setAxisYLeft(new Axis().setTextColor(Color.parseColor("#800065A0")));

        // Set the data in the charts.
        mChart.setLineChartData(mChartData);
        mChart.startDataAnimation(1500);
    }

    @OnClick(R.id.dateButton)
    public void setDate()
    {
        DatePickerDialog datePickerDialog = new DatePickerDialog(context,
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
                        loadDates();
                    }
                }, mYear, mMonth, mDay);
        datePickerDialog.show();
    }

    public void setDateSelectorButton(Calendar c){
        if(position == 0)
        {
            String header = begOfWeek + " - " + endOfWeek + " " + new SimpleDateFormat
                    ("MMMM", Locale.getDefault()).format(c.getTime());
            dateButton.setText(header);
        }
        else if(position == 1)
        {
            String header = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(c.getTime());
            dateButton.setText(header);
        }
        else if(position == 2)
        {
            String header = new SimpleDateFormat("yyyy", Locale.getDefault()).format(c.getTime());
            dateButton.setText(header);
        }
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
        loadDates();
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
        loadDates();
    }

}
