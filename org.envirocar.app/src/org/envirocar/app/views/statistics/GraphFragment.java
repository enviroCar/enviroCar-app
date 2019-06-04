package org.envirocar.app.views.statistics;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.db.chart.animation.Animation;
import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;

import org.envirocar.app.R;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class GraphFragment extends Fragment {

    @Inject
    protected int position;

    @BindView(R.id.stat_line_graph)
    protected LineChartView lineChartView;

    String labels[];
    float values[];
    LineSet dataset;

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

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        labels = new  String[]{"0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9"};
        values = new float[]{2f*(position+1), 3f*(position+1), 5f*(position+1), 9f*(position+1), 1f*(position+1), 6f*(position+1), 10f*(position+1), 2f*(position+1), 1f*(position+1) };
        dataset = new LineSet(labels,values);
        dataset.setColor(Color.parseColor("#36759B"));
        dataset.setSmooth(Boolean.TRUE);
        //dataset.setDotsRadius(8);
        //dataset.setDotsColor()
        dataset.setThickness(7f);
        float intervals[] = {0f,0.8f};
        int colors[] = {Color.parseColor("#9EC9E2"), Color.WHITE};
        //dataset.setDashed(intervals);
        dataset.setGradientFill(colors,intervals);
        lineChartView.addData(dataset);
        Animation animation = new Animation();
        animation.setDuration(1000);
        lineChartView.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
