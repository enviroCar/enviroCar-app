/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.R;
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
public class LogbookAdapter extends RecyclerView.Adapter<LogbookAdapter.FuelingViewHolder> {
    private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("#.##");
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();

    private final List<Fueling> fuelings;
    protected final LogbookUiListener listener;
    private Context context;
    /**
     * Constructor.
     *
     * @param objects the arraylist of fuelings.
     */
    public LogbookAdapter(Context context, List<Fueling> objects, final LogbookUiListener listener) {
        this.context = context;
        this.fuelings = objects;
        this.listener = listener;
    }

    @Override
    public FuelingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem= layoutInflater.inflate(R.layout.activity_logbook_listentry_new, parent, false);
        FuelingViewHolder viewHolder = new FuelingViewHolder(listItem);
        return viewHolder;
    }

    @Override
    public int getItemCount() {
        return fuelings.size();
    }

    @Override
    public void onBindViewHolder(FuelingViewHolder holder, int position) {
        final Fueling fueling = fuelings.get(position);
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date(fueling.getTime()));

        holder.dateText.setText(DateUtils.getDateString(
                context, fueling.getTime()));
        holder.totalPrice.setText(String.format("%s €",
                DECIMAL_FORMATTER.format(fueling.getCost())));
        holder.pricePerLiter.setText(String.format("%s l/€",
                DECIMAL_FORMATTER.format(fueling.getCost() / fueling.getVolume())));
        holder.kmAndLiter.setText(String.format("%s km   -   %s l",
                fueling.getMilage(), fueling.getVolume()));

        Car car = fueling.getCar();
        holder.car.setText(String.format("%s %s",
                car.getManufacturer(), car.getModel()));
        holder.carSub.setText(String.format("(%s / %sccm)",
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

        holder.layout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                final Fueling fueling = fuelings.get(position);
                new MaterialDialog.Builder(context)
                        .title(R.string.logbook_dialog_delete_fueling_header)
                        .content(R.string.logbook_dialog_delete_fueling_content)
                        .positiveText(R.string.menu_delete)
                        .negativeText(R.string.cancel)
                        .onPositive((materialDialog, dialogAction) -> listener.deleteFueling(fueling))
                        .show();
                return false;
            }
        });

    }

    static class FuelingViewHolder extends RecyclerView.ViewHolder{
        @BindView(R.id.layout)
        protected ConstraintLayout layout;
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
        @BindView(R.id.activity_logbook_listentry_car_sub)
        protected TextView carSub;
        @BindView(R.id.activity_logbook_listentry_comment_view)
        protected View commentView;
        @BindView(R.id.activity_logbook_listentry_comment)
        protected TextView commentText;
        @BindView(R.id.activity_logbook_listentry_fillup)
        protected View filledUpView;
        @BindView(R.id.activity_logbook_listentry_missedfillup)
        protected View missedFillUpView;

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
