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
package org.envirocar.app.views.logbook;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.envirocar.app.R;
import org.envirocar.app.databinding.ActivityLogbookListentryBinding;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Fueling;
import org.envirocar.app.views.utils.DateUtils;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.BindView;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class LogbookListAdapter extends ArrayAdapter<Fueling> {
    private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("#.##");
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();

    private final List<Fueling> fuelings;

    /**
     * Constructor.
     *
     * @param context the context of the current scope.
     * @param objects the arraylist of fuelings.
     */
    public LogbookListAdapter(Context context, List<Fueling> objects) {
        super(context, -1, objects);
        this.fuelings = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final Fueling fueling = fuelings.get(position);

        // Then inflate a new view for the car and create a holder
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context
                .LAYOUT_INFLATER_SERVICE);

        FuelingViewHolder holder = null;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.activity_logbook_listentry, parent, false);
            ActivityLogbookListentryBinding binding = ActivityLogbookListentryBinding.bind(convertView);
            holder = new FuelingViewHolder(binding);
            convertView.setTag(holder);
        } else {
            holder = (FuelingViewHolder) convertView.getTag();
        }

        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date(fueling.getTime()));

        holder.dateText.setText(DateUtils.getDateString(
                getContext(), fueling.getTime()));
        holder.totalPrice.setText(String.format("%s €",
                DECIMAL_FORMATTER.format(fueling.getCost())));
        holder.pricePerLiter.setText(String.format("%s l/€",
                DECIMAL_FORMATTER.format(fueling.getCost() / fueling.getVolume())));
        holder.kmAndLiter.setText(String.format("%s km   -   %s l",
                fueling.getMilage(), fueling.getVolume()));

        Car car = fueling.getCar();
        holder.car.setText(String.format("%s %s (%s / %sccm)",
                car.getManufacturer(), car.getModel(),
                car.getConstructionYear(), car.getEngineDisplacement()));

        String comment = fueling.getComment();
        if (comment == null || comment.isEmpty()) {
            holder.commentView.setVisibility(View.GONE);
        } else {
            holder.commentText.setText(comment);
            holder.commentView.setVisibility(View.VISIBLE);
        }

        holder.missedFillUpView.setVisibility(fueling.isMissedFuelStop() ?
                View.VISIBLE : View.GONE);
        holder.filledUpView.setVisibility(fueling.isPartialFueling() ?
                View.VISIBLE : View.GONE);

        return convertView;
    }

    @Override
    public Fueling getItem(int position) {
        return this.fuelings.get(position);
    }

    static class FuelingViewHolder {
        protected TextView dateText;
        protected TextView kmAndLiter;
        protected TextView pricePerLiter;
        protected TextView totalPrice;

        protected TextView car;
        protected View commentView;
        protected TextView commentText;
        protected View filledUpView;
        protected View missedFillUpView;

        /**
         * Constructor.
         *
         * @param binding the binding of the view.
         */
        FuelingViewHolder(ActivityLogbookListentryBinding binding) {
            dateText = binding.activityLogbookListentryDate;
            kmAndLiter = binding.activityLogbookListentryKmliter;
            pricePerLiter = binding.activityLogbookListentryPriceperliter;
            totalPrice = binding.activityLogbookListentryTotalprice;
            car = binding.activityLogbookListentryCar;
            commentView = binding.activityLogbookListentryCommentView;
            commentText = binding.activityLogbookListentryComment;
            filledUpView = binding.activityLogbookListentryFillup;
            missedFillUpView = binding.activityLogbookListentryMissedfillup;
        }
    }
}
