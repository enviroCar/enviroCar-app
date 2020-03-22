/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.logbook;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.envirocar.app.R;
import org.envirocar.app.views.utils.DateUtils;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Fueling;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class LogbookListAdapter extends RecyclerView.Adapter<LogbookListAdapter.FuelingViewHolder> {
    private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("#.##");
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();

    private final List<Fueling> fuelings;

    /**
     * Constructor.
     *
     * @param objects the arraylist of fuelings.
     */
    public LogbookListAdapter(List<Fueling> objects) {
        this.fuelings = objects;
    }

    // Create new views
    @NonNull
    @Override
    public FuelingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.activity_logbook_listentry, parent, false);
        return new FuelingViewHolder(view);
    }

    // Bind the data to the view
    @Override
    public void onBindViewHolder(@NonNull FuelingViewHolder holder, int position) {

        final Fueling fueling = fuelings.get(position);

        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date(fueling.getTime()));

        holder.dateText.setText(DateUtils.getDateString(
                holder.itemView.getContext(), fueling.getTime()));
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
    }

    // Returns the size of data list.
    @Override
    public int getItemCount() {
        return fuelings.size();
    }

    static class FuelingViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.activity_logbook_listentry_date)
        protected TextView dateText;
        @BindView(R.id.activity_logbook_listentry_kmliter)
        protected TextView kmAndLiter;
        @BindView(R.id.activity_logbook_listentry_priceperliter)
        protected TextView pricePerLiter;
        @BindView(R.id.activity_logbook_listentry_totalprice)
        protected TextView totalPrice;

        @BindView(R.id.activity_logbook_listentry_car)
        protected TextView car;
        @BindView(R.id.activity_logbook_listentry_comment_view)
        protected View commentView;
        @BindView(R.id.activity_logbook_listentry_comment)
        protected TextView commentText;
        @BindView(R.id.activity_logbook_listentry_fillup)
        protected View filledUpView;
        @BindView(R.id.activity_logbook_listentry_missedfillup)
        protected View missedFillUpView;
        @BindView(R.id.ll_foreground_logbook)
        protected LinearLayout foregroundView;

        /**
         * Constructor.
         *
         * @param view the core view to inject the subviews from.
         */
        FuelingViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

}
