package org.envirocar.app.views.statistics;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.transition.Fade;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

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

    @BindView(R.id.graphLayout)
    protected ConstraintLayout graphLayout;

    @BindView(R.id.dateButton)
    protected Button dateButton;

    @BindView(R.id.info_msg)
    protected TextView infoMsg;

    @BindView(R.id.info_img)
    protected ImageView infoImg;

    @BindView(R.id.graph_loading_icon)
    protected ImageView loadingIcon;

    @BindView(R.id.arrow_left)
    protected ImageButton arrowLeft;

    @BindView(R.id.arrow_right)
    protected ImageButton arrowRight;

    @BindView(R.id.chart)
    protected lecho.lib.hellocharts.view.LineChartView mChart;

    private LineChartData mChartData;
    // If spinnerChoice is Speed, this list holds the number of tracks corresponding
    // to each index of values. It is used to calculate the average speed for each index
    protected ArrayList<Float> noOfTracks;
    private Boolean isTrackDownloading = false;
    private ChoiceViewModel choiceViewModel;

    // Holds the current date for the date picker
    protected int mYear, mMonth, mDay;
    // Holds the current date range to show data for
    protected Date startDate, endDate;

    protected CompositeSubscription subscriptions = new CompositeSubscription();
    protected Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    private Unbinder unbinder;
    private Context context;

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
            showMessage(R.string.statistics_loading_data);
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
        Calendar c = Calendar.getInstance();
        setDates(c);
        loadGraph();
        loadingIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadGraph();
            }
        });
        return view;
    }

    public void setDates(Calendar c) {
        if (tabPosition == 0) {
            startDate = getWeekStartDate(c.getTime());
            endDate = getWeekEndDate(c.getTime());
        } else if (tabPosition == 1 ) {
            // Set the start date as the 1st of the current month
            c.set(Calendar.DAY_OF_MONTH, 1);
            startDate = c.getTime();

            // Set the end date as the 1st of the next month
            c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, 1 );
            endDate = c.getTime();
        } else if (tabPosition == 2) {
            // Set the start date as Jan 1st of the current year
            c.set(Calendar.DAY_OF_YEAR, 1);
            startDate = c.getTime();

            // Set the end date as Jan 1st of the next month
            c.set(c.get(Calendar.YEAR) + 1, 0, 1 );
            endDate = c.getTime();
        }

        c.setTime(startDate);
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        setDateSelectorButton();
    }

    public void loadGraph() {
        LOG.info("loadGraph");
        loadingIcon.setVisibility(View.VISIBLE);
        RotateAnimation rotate = new RotateAnimation(0,360,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(1000);
        rotate.setRepeatCount(Animation.INFINITE);
        rotate.setRepeatMode(Animation.INFINITE);
        rotate.setInterpolator(new LinearInterpolator());
        loadingIcon.startAnimation(rotate);

        cleanUpData();
        showMessage(R.string.statistics_loading_data);

        // If the tracks are being downloaded, wait
        // Else load the dates
        if (!isTrackDownloading) {
            // If the persistentTrackList has no elements, check if there are any new
            // tracks. Or if the tracks have not been downloaded yet, download them
            if (persistentTrackList.size() == 0)
                getData();
            else {
                setTrackList();
                trimTracksToRange(startDate, endDate);
            }
        }
    }

    /**
     * Resets all data used in the chart
     */
    public void cleanUpData() {
        trackIteration = 0;
        xAxisNameArray = new ArrayList<>();
        xAxisNameArray.add("Days");
        xAxisNameArray.add("Dates");
        xAxisNameArray.add("Months");
        setZeros();
        setLabels();
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
                    }

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error(e.getMessage(), e);
                        if (e instanceof NotConnectedException) {
                            LOG.error("Error in getData", e);
                        } else if (e instanceof UnauthorizedException) {
                            LOG.error("Unauthorised",e);
                        }
                        isTrackDownloading = false;
                        loadingIcon.clearAnimation();
                        showMessage(R.string.statistics_error_loading_data);
                    }

                    @Override
                    public void onNext(List<Track> tracks) {
                        for (Track track : tracks) {
                            if (!persistentTrackList.contains(track)) {
                                persistentTrackList.add(track);
                            }
                        }
                        isTrackDownloading = false;
                        showMessage(R.string.statistics_loading_data);

                        setTrackList();
                        trimTracksToRange(startDate, endDate);
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

    public void trimTracksToRange(Date start, Date end) {
        for (int i = 0; i < mTrackList.size(); ++i) {
            TrackDateUtil t = new TrackDateUtil(mTrackList.get(i));
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
            loadingIcon.clearAnimation();
            showMessage(R.string.no_stats);
        } else {
            setLabels();
            setZeros();

            if (spinnerChoice != 2) {
                for (int i = 0; i < mTrackList.size(); ++i) {
                    try {
                        Track temp = mTrackList.get(i);
                        TrackDateUtil t = new TrackDateUtil(temp);

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
                    TrackDateUtil t = new TrackDateUtil(track);
                    getTrackStatistics(trackID, t, TrackStatistics.KEY_USER_STAT_SPEED);
                }
            }
        }
    }

    /**
     * Get the average speed for the track and add it to the values List
     * @param trackID for the track to get average speed
     * @param t used to select which index of values to add the speed to
     * @param phenomenon the phenomenon to get data for
     */
    public void getTrackStatistics(String trackID, TrackDateUtil t, String phenomenon) {
        int index;
        if (tabPosition == 0)
            index = t.getDay() - 1;
        else if (tabPosition == 1)
            index = t.getDate() - 1;
        else
            index = t.getMonth();

        subscriptions.add(mDAOProvider.getTrackStatisticsDAO()
                .getTrackStatisticsByPhenomenonObservable(trackID, phenomenon)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<TrackStatistics>() {

                    @Override
                    public void onStart() {
                        LOG.info("onStart() of getTrackStatistics with " + trackID);
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
                        loadingIcon.clearAnimation();
                        showMessage(R.string.statistics_error_loading_data);
                    }

                    @Override
                    public void onNext(TrackStatistics trackStatistics) {
                        values.set(index, values.get(index) + getTrackStatData(trackStatistics, phenomenon));
                        noOfTracks.set(index, noOfTracks.get(index) + 1);
                        trackIteration++;
                        computeAverages();
                    }
                }));
    }

    /**
     *
     * @param trackStatistics
     * @param phenomenon
     * @return the average value of the specified phenomenon from the trackstatistics object
     */
    public Float getTrackStatData(TrackStatistics trackStatistics, String phenomenon) {
        if (trackStatistics.getStatistic(phenomenon) != null)
            return (float) trackStatistics.getStatistic(phenomenon).getAvgValue();
        else
            return (float) 0;
    }

    /**
     * After the statistics for all tracks have been downloaded, compute the average for
     * each index of values
     */
    public void computeAverages() {
        if (trackIteration < mTrackList.size()) {
            // Do nothing..
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
        infoMsg.setVisibility(View.GONE);
        infoImg.setVisibility(View.GONE);
        loadingIcon.clearAnimation();
        loadingIcon.setVisibility(View.GONE);

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
    public void createDateDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(context,
                new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {
                        Calendar c = Calendar.getInstance();
                        c.set(Calendar.YEAR,year);
                        c.set(Calendar.MONTH, monthOfYear);
                        c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        setDates(c);
                        loadGraph();
                    }
                }, mYear, mMonth, mDay);
        datePickerDialog.show();
    }

    public void setDateSelectorButton(){
        Transition dateButtonTransition = new Fade();
        dateButtonTransition.addTarget(dateButton).addTarget(R.id.dateButton);
        TransitionManager.beginDelayedTransition(graphLayout, dateButtonTransition);

        if (tabPosition == 0) {
            Calendar start = Calendar.getInstance(), end = Calendar.getInstance();
            start.setTime(startDate);
            end.setTime(endDate);

            int begOfWeek = start.get(Calendar.DAY_OF_MONTH);
            int endOfWeek = end.get(Calendar.DAY_OF_MONTH);

            String header;
            if(start.get(Calendar.MONTH) == end.get(Calendar.MONTH))
                header = begOfWeek + " - " + endOfWeek + " " + new SimpleDateFormat
                    ("MMMM", Locale.getDefault()).format(startDate);
            else {
                String begMonth = new SimpleDateFormat
                        ("MMM", Locale.getDefault()).format(startDate);
                String endMonth = new SimpleDateFormat
                        ("MMM", Locale.getDefault()).format(endDate);
                header = begOfWeek + " " + begMonth + " - " + endOfWeek + " " + endMonth;
            }

            dateButton.setText(header);
        } else if (tabPosition == 1) {
            String header = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(startDate);
            dateButton.setText(header);
        } else if (tabPosition == 2) {
            String header = new SimpleDateFormat("yyyy", Locale.getDefault()).format(startDate);
            dateButton.setText(header);
        }
    }

    public static Date getWeekStartDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        }
        return calendar.getTime();
    }

    public static Date getWeekEndDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        }
        calendar.add(Calendar.DAY_OF_YEAR, 7);
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
        setDates(c);
        loadGraph();
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
        setDates(c);
        loadGraph();
    }

    public void showMessage(int stringRes){
        infoImg.setVisibility(View.VISIBLE);
        infoMsg.setVisibility(View.VISIBLE);
        infoMsg.setText(stringRes);
        mChart.setVisibility(View.INVISIBLE);
    }
}
