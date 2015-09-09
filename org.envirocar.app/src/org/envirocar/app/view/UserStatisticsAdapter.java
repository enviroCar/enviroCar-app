package org.envirocar.app.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.envirocar.app.R;
import org.envirocar.app.model.UserStatistics;

import java.text.DecimalFormat;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * @author dewall
 */
public class UserStatisticsAdapter extends ArrayAdapter<UserStatistics.PhenomenonStatisticHolder> {
    private static final DecimalFormat TWO_DIGITS_FORMATTER = new DecimalFormat("#.##");

    private final List<UserStatistics.PhenomenonStatisticHolder> mValues;

    /**
     * Constructor.
     *
     * @param context the context of the current scope.
     * @param values
     */
    public UserStatisticsAdapter(
            Context context, List<UserStatistics.PhenomenonStatisticHolder> values) {
        super(context, -1, values);
        this.mValues = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        UserStatistics.PhenomenonStatisticHolder phenomenon = mValues.get(position);

        ViewHolder viewHolder = null;
        if(convertView == null){
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(
                    R.layout.activity_account_statistic_list_entry, parent, false);

            // Create a new viewholder and inject the sub-views of the newly inflated convertView.
            viewHolder = new ViewHolder();
            ButterKnife.inject(viewHolder, convertView);

            // Set the viewHolder as tag on the convertView.
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.mPhenomenonTextView.setText(phenomenon.phenomenon);
        viewHolder.mAvgValue.setText(TWO_DIGITS_FORMATTER.format(phenomenon.avg)
                + " " + phenomenon.phenomenonUnit);
        viewHolder.mMaxValue.setText(TWO_DIGITS_FORMATTER.format(phenomenon.max)
                + " " + phenomenon.phenomenonUnit);
        viewHolder.mMinValue.setText(TWO_DIGITS_FORMATTER.format(phenomenon.min)
                + " " + phenomenon.phenomenonUnit);

        return convertView;
    }

    static final class ViewHolder{
        @InjectView(R.id.activity_account_statistics_list_entry_phenomenon)
        TextView mPhenomenonTextView;
        @InjectView(R.id.activity_account_statistics_list_entry_avg_value)
        TextView mAvgValue;
        @InjectView(R.id.activity_account_statistics_list_entry_max_value)
        TextView mMaxValue;
        @InjectView(R.id.activity_account_statistics_list_entry_min_value)
        TextView mMinValue;
    }
}
