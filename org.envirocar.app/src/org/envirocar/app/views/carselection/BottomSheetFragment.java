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
import org.envirocar.app.databinding.CarAttributesDetailBottomsheetBinding;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Vehicles;





public class BottomSheetFragment extends BottomSheetDialogFragment {

    TextView manufacturer;

    TextView model;

    TextView year;

    TextView fuel;

    TextView power;

    TextView engine;

    View engineLayout;

    Vehicles vehicle;

    BottomSheetFragment(Vehicles vehicles) {
        this.vehicle = vehicles;
    }

    private CarAttributesDetailBottomsheetBinding binding;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        binding = CarAttributesDetailBottomsheetBinding.inflate(inflater,container,false);
        View view = binding.getRoot();
        manufacturer=binding.activityCarDetailsAttrManufacturerValue;
        model=binding.activityCarDetailsAttrCarValue;
        year=binding.activityCarDetailsAttrYearValue;
        fuel=binding.activityCarDetailsAttrFuelValue;
        power=binding.activityCarDetailsAttrPowerValue;
        engine=binding.activityCarDetailsAttrEngineValue;
        engineLayout=binding.bottomSheetEngineLayout;


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

        void cancelSheet(View view) {
        dismiss();
    }

        void proceed(View view) {
        ((CarSelectionActivity)getActivity()).registerCar(vehicle);
        dismiss();
    }
}