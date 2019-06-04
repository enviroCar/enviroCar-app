package org.envirocar.app.views.statistics;

import android.graphics.Color;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.main.MainActivityComponent;
import org.envirocar.app.main.MainActivityModule;
import org.envirocar.core.logging.Logger;

import butterknife.BindView;
import butterknife.ButterKnife;

public class StatisticsFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(StatisticsFragment.class);

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

    @BindView(R.id.stat_graph_spinner)
    protected Spinner GraphSpinner;

    //@BindView(R.id.stat_graph)
    //protected LineChartView LineChart;

    @BindView(R.id.stat_tabLayout)
    protected TabLayout tabLayout;

    @BindView(R.id.stat_viewPager)
    protected ViewPager viewPager;


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
        ButterKnife.bind(this, statView);
        /*String labels[] = {"0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9"};
        float values[] = {2f, 3f, 5f, 9f, 1f, 6f, 10f, 2f, 1f };
        LineSet dataset = new LineSet(labels,values);
        dataset.setColor(Color.parseColor("#36759B"));
        dataset.setSmooth(Boolean.TRUE);
        LineChart.addData(dataset);
        Animation animation = new Animation();
        animation.setDuration(2000);
        LineChart.show(animation);*/

        ViewPagerAdapter adapter = new ViewPagerAdapter(getFragmentManager());
        viewPager.setAdapter(adapter);

        tabLayout.setupWithViewPager(viewPager);

        return statView;
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        MainActivityComponent mainActivityComponent =  baseApplicationComponent.plus(new MainActivityModule(getActivity()));
        mainActivityComponent.inject(this);
    }
}
