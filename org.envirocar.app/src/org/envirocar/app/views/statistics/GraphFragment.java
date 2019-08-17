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

    // 0: Weekly
    // 1: Monthly
    // 2: Yearly
    protected int tabPosition;

    // 0: Number of tracks
    // 1: Distance
    // 2: Speed
    protected int spinnerChoice;

    // If the spinner choice is speed, we need to call the tracks/:trackid/statistics endpoint
    // to get the average speed of each track so as to create an array with the average speed for
    // each day or month.
    protected int trackIteration;

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

    @BindView(R.id.loading_stats)
    protected TextView loadingStats;

    @BindView(R.id.info_img)
    protected ImageView infoImg;

    @BindView(R.id.arrow_left)
    protected ImageButton arrowLeft;

    @BindView(R.id.arrow_right)
    protected ImageButton arrowRight;

    @BindView(R.id.chart)
    protected lecho.lib.hellocharts.view.LineChartView mChart;

    private LineChartData mChartData;
    protected ArrayList<Float> noOfTracks;
    protected int mYear, mMonth, mDay, mWeek, begOfWeek, endOfWeek;
    protected CompositeSubscription subscriptions = new CompositeSubscription();
    protected Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    private Unbinder unbinder;
    private ChoiceViewModel choiceViewModel;
    private Context context;
    private Boolean isTrackDownloading = false;

    // List of tracks for the range selected
    protected List<Track> mTrackList = new ArrayList<>();
    // Total Remote TrackList for the user
    protected List<Track> persistentTrackList = new ArrayList<>();

    // Used to compute the y value for each point in the graph
    protected ArrayList<Float> values;

    // Holds a collection of pairs of (x,y) for the graph
    // After values is set, this list is made
    protected List<PointValue> pointValues = new ArrayList<>();

    // The names of the X Axis based on tabPosition
    // i.e. Days, Dates or Months
    protected List<String> xAxisNameArray;

    // Holds the names of each point (x) on the X Axis in the graph
    // i.e. if tabPosition is weekly, it holds the days of the week
    protected List<AxisValue> xAxisLabels = new ArrayList<>();

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
            spinnerChoice = item;
            infoImg.setVisibility(View.VISIBLE);
            loadingStats.setVisibility(View.VISIBLE);
            mChart.setVisibility(View.INVISIBLE);
            loadGraph();
        });
    }

    @Nullable
    @Override
    public View onCreateView(@Nullable LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.stat_fragment_graph, container, false);
        tabPosition = getArguments().getInt("pos");
        unbinder = ButterKnife.bind(this, view);
        mDAOProvider = new DAOProvider(context);
        spinnerChoice = 0;
        cleanUpData();
        return view;
    }

    public void loadGraph() {
        cleanUpData();
        // If the tracks are being downloaded, wait
        // Else load the dates
        if (!isTrackDownloading) {
            // If the persistentTrackList has no elements, check if there are any new
            // tracks. Or if the tracks have not been downloaded yet, download them
            if (persistentTrackList.size() == 0)
                getData();
            else
                loadDates();
        }
    }

    /**
     * Resets all data used in the chart
     */
    public void cleanUpData() {
        Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        mWeek = c.get(Calendar.WEEK_OF_YEAR);
        begOfWeek = getWeekStartDate(c.getTime()).getDate();
        endOfWeek = getWeekEndDate(c.getTime()).getDate();

        trackIteration = 0;
        xAxisNameArray = new ArrayList<>();
        xAxisNameArray.add("Days");
        xAxisNameArray.add("Dates");
        xAxisNameArray.add("Months");
        setZeros();
        setLabels();
        setDateSelectorButton(c);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (!subscriptions.isUnsubscribed()) {
            subscriptions.unsubscribe();
        }
        unbinder.unbind();
    }

    /**
     * Set all the y values of the chart to zero
     */
    public void setZeros() {
        pointValues.clear();

        int n;
        if (tabPosition == 0)
            n = 7;
        else if (tabPosition == 1)
            n = 31;
        else
            n = 12;
        for (int i = 0; i < n; ++i)
            pointValues.add(new PointValue(i,0));

        values = new ArrayList<>(Collections.nCopies(n, 0f));
        noOfTracks = new ArrayList<>(Collections.nCopies(n, 0f));
    }

    /**
     * Based on the tabPosition set the labels for the X Axis
     */
    public void setLabels() {
        xAxisLabels.clear();
        List<String> temp;
        String weekdays[] = {"Sun", "Mon", "Tue", "Wed", "Thur", "Fri", "Sat"};
        String months[] = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul",
                "Aug", "Sep", "Oct", "Nov", "Dec"};
        temp = new ArrayList<>();

        if (tabPosition == 0)
            temp = Arrays.asList(weekdays);
        else if (tabPosition == 1)
            for (int i = 1; i <= 31; ++i)
                temp.add(i+"");
        else
            temp = Arrays.asList(months);

        for (int i = 0; i < temp.size(); ++i){
            xAxisLabels.add(new AxisValue(i).setLabel(temp.get(i)));
        }
    }

    public void getData() {
        isTrackDownloading = true;
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
                                LOG.debug("persistentTrackList Empty");
                            }
                        } else if (e instanceof UnauthorizedException) {
                            LOG.error("Unauthorised",e);
                            if (persistentTrackList.isEmpty()) {
                                LOG.debug("persistentTrackList Empty");
                            }
                        }
                    }

                    @Override
                    public void onNext(List<Track> tracks) {
                        for (Track track : tracks) {
                            if (!persistentTrackList.contains(track)) {
                                persistentTrackList.add(track);
                            }
                        }
                        isTrackDownloading = false;
                        loadDates();
                    }
                }));
    }

    /**
     * Set mTrackList to hold all tracks
     * This is done every time the range changes. After setting, the tracks not in
     * the desired range are removed in trimTracksToRange()
     */
    public void setTrackList() {
        for (Track track : persistentTrackList) {
            if (!mTrackList.contains(track)) {
                mTrackList.add(track);
            }
        }
    }


    /**
     * Set the date range needed and then trim mTrackList
     */
    public void loadDates() {
        infoImg.setVisibility(View.VISIBLE);
        loadingStats.setVisibility(View.VISIBLE);
        noStats.setVisibility(View.GONE);
        mChart.setVisibility(View.INVISIBLE);

        setTrackList();

        Calendar cal = Calendar.getInstance();
        Date after = cal.getTime(), before = cal.getTime();
        if (tabPosition == 0) {
            cal.set(mYear,mMonth,mDay);
            after = getWeekStartDate(cal.getTime());
            before = getWeekEndDate(cal.getTime());
        } else if (tabPosition == 1) {
            cal.set(mYear,mMonth,1);
            after = cal.getTime();
            cal.set(mYear,mMonth+1,1);
            before = cal.getTime();
        } else if (tabPosition == 2) {
            cal.set(mYear,0,1);
            after = cal.getTime();
            cal.set(mYear+1,0,1);
            before = cal.getTime();
        }

        trimTracksToRange(after, before);
    }

    public void trimTracksToRange(Date start, Date end) {
        for (int i = 0; i < mTrackList.size(); ++i) {
            TrackwDate t = new TrackwDate();
            t.getDateTime(mTrackList.get(i));
            if (!(t.getDateObject().after(start) && t.getDateObject().before(end))) {
                mTrackList.remove(i);
                i--;
            }
        }
        LOG.info("Tracks in Range:" + mTrackList.size());
        setGraph();
    }

    public void setGraph() {
        if(mTrackList.size() == 0) {
            loadingStats.setVisibility(View.GONE);
            noStats.setVisibility(View.VISIBLE);
            infoImg.setVisibility(View.VISIBLE);
        } else {
            TrackwDate t = new TrackwDate();
            setLabels();
            setZeros();

            if (spinnerChoice != 2) {
                for (int i = 0; i < mTrackList.size(); ++i) {
                    try {
                        Track temp = mTrackList.get(i);
                        t.getDateTime(temp);

                        // Which day / date / month does the data need to be stored in
                        int index;
                        if (tabPosition == 0)
                            // Not 0 based, so need to -1
                            index = t.getDay() - 1;
                        else if (tabPosition == 1)
                            // Not 0 based, so need to -1
                            index = t.getDate() - 1;
                        else
                            index = t.getMonth();

                        if (spinnerChoice == 0)
                            values.set(index, values.get(index) + 1);
                        else if (spinnerChoice == 1)
                            values.set(index, values.get(index) + temp.getLength().floatValue());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                setGraphOptionsAndShow();
            } else {
                trackIteration = 0;
                for (Track track : mTrackList) {
                    String trackID = track.getRemoteID();
                    t.getDateTime(track);
                    getTrackStatistics(trackID, t);
                }
            }
        }
    }

    public void getTrackStatistics(String trackID, TrackwDate t) {
        int index;
        if (tabPosition == 0)
            index = t.getDay() - 1;
        else if (tabPosition == 1)
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
                    }

                    @Override
                    public void onNext(TrackStatistics trackStatistics) {
                        values.set(index, values.get(index) + getTrackStatData(trackStatistics));
                        noOfTracks.set(index, noOfTracks.get(index) + 1);
                        trackIteration++;
                        computeAverageSpeed();
                    }
                }));
    }

    public Float getTrackStatData(TrackStatistics trackStatistics) {
        if (trackStatistics.getStatistic(TrackStatistics.KEY_USER_STAT_SPEED) != null)
            return (float) trackStatistics.getStatistic(TrackStatistics.KEY_USER_STAT_SPEED).getAvgValue();
        else
            return (float) 0;
    }


    public void computeAverageSpeed() {
        // Have the statistics for all the tracks in range been saved?
        // If yes, compute the averages for the values List
        // else, do nothing
        if (trackIteration < mTrackList.size()) {

        } else {
            LOG.info("Stats of all tracks loaded");
            for (int i = 0; i < values.size(); ++i) {
                if (values.get(i) != 0)
                    values.set(i, values.get(i) / noOfTracks.get(i));
            }
            setGraphOptionsAndShow();
        }
    }

    /**
     * After all the data has been downloaded, sorted / computed, set all the
     * needed parameters for the graph
     */
    public void setGraphOptionsAndShow() {
        loadingStats.setVisibility(View.GONE);
        noStats.setVisibility(View.GONE);
        infoImg.setVisibility(View.GONE);
        mChart.setVisibility(View.VISIBLE);
        mChart.cancelDataAnimation();

        Line line = new Line(pointValues);
        line.setCubic(false).setColor(Color.parseColor("#36759B"))
                .setStrokeWidth(2)
                .setPointRadius(3)
                .setPointColor(Color.parseColor("#36759B"));

        List<Line> lines = new ArrayList<>();
        lines.add(line);
        int i = 0;
        for (PointValue value : line.getValues()) {
            value.setTarget(value.getX(), values.get(i));
            i++;
        }

        mChartData = new LineChartData(lines);
        mChartData.setAxisXBottom(new Axis(xAxisLabels).setHasLines(true).setName(xAxisNameArray.get(tabPosition))
                .setTextColor(Color.parseColor("#800065A0")));
        mChartData.setAxisYLeft(new Axis().setTextColor(Color.parseColor("#800065A0")));

        // Set the data in the charts.
        mChart.setLineChartData(mChartData);
        mChart.startDataAnimation(1500);
    }

    @OnClick(R.id.dateButton)
    public void setDate() {
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
        if (tabPosition == 0) {
            String header = begOfWeek + " - " + endOfWeek + " " + new SimpleDateFormat
                    ("MMMM", Locale.getDefault()).format(c.getTime());
            dateButton.setText(header);
        } else if (tabPosition == 1) {
            String header = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(c.getTime());
            dateButton.setText(header);
        } else if (tabPosition == 2) {
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

    /**
     * Go to the previous time range
     */
    @OnClick(R.id.arrow_left)
    public void moveLeft() {
        Calendar c = Calendar.getInstance();
        if (tabPosition == 0) {
            c.set(mYear,mMonth,mDay);
            c.add(Calendar.DAY_OF_YEAR, -7);
            mYear = c.get(Calendar.YEAR);
            mMonth = c.get(Calendar.MONTH);
            mDay = c.get(Calendar.DAY_OF_MONTH);
            mWeek = c.get(Calendar.WEEK_OF_YEAR);
            begOfWeek = getWeekStartDate(c.getTime()).getDate();
            endOfWeek = getWeekEndDate(c.getTime()).getDate();
        } else if(tabPosition == 1) {
            mMonth--;
            c.set(mYear,mMonth,mDay);
        } else {
            mYear--;
            c.set(mYear,mMonth,mDay);
        }
        // If the data being shown is Speed, unsubscribe from previous data
        // being downloaded from the API
        if (spinnerChoice == 2) {
            subscriptions.clear();
        }
        setDateSelectorButton(c);
        loadDates();
    }

    /**
     * Go to the next time range
     */
    @OnClick(R.id.arrow_right)
    public void moveRight() {
        Calendar c = Calendar.getInstance();
        if (tabPosition == 0) {
            c.set(mYear,mMonth,mDay);
            c.add(Calendar.DAY_OF_YEAR, 7);
            mYear = c.get(Calendar.YEAR);
            mMonth = c.get(Calendar.MONTH);
            mDay = c.get(Calendar.DAY_OF_MONTH);
            mWeek = c.get(Calendar.WEEK_OF_YEAR);
            begOfWeek = getWeekStartDate(c.getTime()).getDate();
            endOfWeek = getWeekEndDate(c.getTime()).getDate();
        } else if (tabPosition == 1) {
            mMonth++;
            c.set(mYear,mMonth,mDay);
        } else {
            mYear++;
            c.set(mYear,mMonth,mDay);
        }
        // If the data being shown is Speed, unsubscribe from previous data
        // being downloaded from the API
        if (spinnerChoice == 2) {
            subscriptions.clear();
        }
        setDateSelectorButton(c);
        loadDates();
    }
}
