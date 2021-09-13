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
package org.envirocar.app.views.carselection;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.envirocar.app.R;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Vehicles;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class BottomSheetFragment extends BottomSheetDialogFragment {

    @BindView(R.id.activity_car_details_attr_manufacturer_value)
    TextView manufacturer;
    @BindView(R.id.activity_car_details_attr_car_value)
    TextView model;
    @BindView(R.id.activity_car_details_attr_year_value)
    TextView year;
    @BindView(R.id.activity_car_details_attr_fuel_value)
    TextView fuel;
    @BindView(R.id.activity_car_details_attr_power_value)
    TextView power;
    @BindView(R.id.activity_car_details_attr_engine_value)
    TextView engine;
    @BindView(R.id.bottomSheetEngineLayout)
    View engineLayout;

    Vehicles vehicle;

    BottomSheetFragment(Vehicles vehicles) {
        this.vehicle = vehicles;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.car_attributes_detail_bottomsheet, container,false);
        ButterKnife.bind(this, view);
        manufacturer.setText(vehicle.getManufacturer());
        model.setText(vehicle.getCommerical_name());
        year.setText(vehicle.getAllotment_date());
        Car.FuelType fuelType = ((CarSelectionActivity)getActivity()).getFuel(vehicle.getPower_source_id());
        fuel.setText(getContext().getString(fuelType.getStringResource()));
        power.setText(vehicle.getPower()+" kW");
        if (fuelType != Car.FuelType.ELECTRIC) {
            engine.setText(vehicle.getEngine_capacity() + " cm\u00B3");
        } else {
            engineLayout.setVisibility(View.GONE);
        }
        return view;
    }

    @OnClick(R.id.activity_car_details_cancel)
    void cancelSheet() {
        dismiss();
    }

    @OnClick(R.id.activity_car_details_create)
    void proceed() {
        ((CarSelectionActivity)getActivity()).registerCar(vehicle);
        dismiss();
    }
}
