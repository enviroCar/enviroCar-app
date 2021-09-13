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
import org.envirocar.core.entity.Car;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.BindView;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class LogbookCarSpinnerAdapter extends ArrayAdapter<Car> {

    private final List<Car> cars;

    /**
     * Constructor.
     *
     * @param context the context of the current scope.
     * @param objects
     */
    public LogbookCarSpinnerAdapter(Context context, List<Car> objects) {
        super(context, R.layout.activity_logbook_car_spinner_entry, R.id
                .activity_logbook_car_spinner_entry_firstline, objects);
        this.cars = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final Car car = cars.get(position);

        CarSpinnerEntryHolder holder = null;
        if (convertView == null) {
            // Then inflate a new view for the car and create a holder
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context
                    .LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.activity_logbook_car_spinner_entry,
                    parent, false);
            holder = new CarSpinnerEntryHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (CarSpinnerEntryHolder) convertView.getTag();
        }

        holder.title.setText(car.getManufacturer() + " " + car.getModel());
        holder.secondLine.setText(car.getConstructionYear() +
                "     " + car.getFuelType() +
                "     " + car.getEngineDisplacement() + " ccm");

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    static class CarSpinnerEntryHolder {

        @BindView(R.id.activity_logbook_car_spinner_entry_firstline)
        protected TextView title;
        @BindView(R.id.activity_logbook_car_spinner_entry_secondline)
        protected TextView secondLine;


        /**
         * Constructor.
         *
         * @param view the parent view of an entry in the car spinner.
         */
        CarSpinnerEntryHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
