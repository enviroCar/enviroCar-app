/**
 * Copyright (C) 2013 - 2021 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.others;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.envirocar.app.databinding.ActivityAccountStatisticListEntryBinding;
import org.envirocar.core.entity.Phenomenon;

import java.text.DecimalFormat;
import java.util.List;

/**
 * @author dewall
 */
public class UserStatisticsAdapter extends ArrayAdapter<Phenomenon> {
    private static final DecimalFormat TWO_DIGITS_FORMATTER = new DecimalFormat("#.##");

    private final List<Phenomenon> mValues;

    /**
     * Constructor.
     *
     * @param context the context of the current scope.
     * @param values
     */
    public UserStatisticsAdapter(Context context, List<Phenomenon> values) {
        super(context, -1, values);
        this.mValues = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Phenomenon phenomenon = mValues.get(position);

        ViewHolder viewHolder = null;
        if (convertView == null) {
            final LayoutInflater inflater = LayoutInflater.from(getContext());
            final ActivityAccountStatisticListEntryBinding binding = ActivityAccountStatisticListEntryBinding.inflate(inflater, parent, false);
            convertView = binding.getRoot();
            viewHolder = new ViewHolder(binding);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.mPhenomenonTextView.setText(phenomenon.getPhenomenonName());
        viewHolder.mAvgValue.setText(TWO_DIGITS_FORMATTER.format(phenomenon.getAvgValue())
                + " " + phenomenon.getPhenomenonUnit());
        viewHolder.mMaxValue.setText(TWO_DIGITS_FORMATTER.format(phenomenon.getMaxValue())
                + " " + phenomenon.getPhenomenonUnit());
        viewHolder.mMinValue.setText(TWO_DIGITS_FORMATTER.format(phenomenon.getMinValue())
                + " " + phenomenon.getPhenomenonUnit());

        return convertView;
    }

    static final class ViewHolder {
        TextView mPhenomenonTextView;
        TextView mAvgValue;
        TextView mMaxValue;
        TextView mMinValue;

        ViewHolder(ActivityAccountStatisticListEntryBinding binding) {
            mPhenomenonTextView = binding.activityAccountStatisticsListEntryPhenomenon;
            mAvgValue = binding.activityAccountStatisticsListEntryAvgValue;
            mMaxValue = binding.activityAccountStatisticsListEntryMaxValue;
            mMinValue = binding.activityAccountStatisticsListEntryMinValue;
        }
    }
}
