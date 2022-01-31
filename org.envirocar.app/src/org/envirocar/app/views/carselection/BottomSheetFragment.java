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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import org.envirocar.app.databinding.CarAttributesDetailBottomsheetBinding;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Vehicles;


public class BottomSheetFragment extends BottomSheetDialogFragment {

    Vehicles vehicle;

    BottomSheetFragment(Vehicles vehicles) {
        this.vehicle = vehicles;
    }
    private CarAttributesDetailBottomsheetBinding carAttributesDetailBottomsheetBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        carAttributesDetailBottomsheetBinding = CarAttributesDetailBottomsheetBinding.inflate(inflater,container,false);
        View view = carAttributesDetailBottomsheetBinding.getRoot();
        carAttributesDetailBottomsheetBinding.activityCarDetailsAttrManufacturerValue.setText(vehicle.getManufacturer());
        carAttributesDetailBottomsheetBinding.activityCarDetailsAttrCarValue.setText(vehicle.getCommerical_name());
        carAttributesDetailBottomsheetBinding.activityCarDetailsAttrYearValue.setText(vehicle.getAllotment_date());
        Car.FuelType fuelType = ((CarSelectionActivity)getActivity()).getFuel(vehicle.getPower_source_id());
        carAttributesDetailBottomsheetBinding.activityCarDetailsAttrFuelValue.setText(getContext().getString(fuelType.getStringResource()));
        carAttributesDetailBottomsheetBinding.activityCarDetailsAttrPowerValue.setText(vehicle.getPower()+" kW");
        if (fuelType != Car.FuelType.ELECTRIC) {
            carAttributesDetailBottomsheetBinding.activityCarDetailsAttrEngineValue.setText(vehicle.getEngine_capacity() + " cm\u00B3");
        } else {
            carAttributesDetailBottomsheetBinding.bottomSheetEngineLayout.setVisibility(View.GONE);
        }
        return view;
    }

    void cancelSheet() {
        carAttributesDetailBottomsheetBinding.activityCarDetailsCancel.setOnClickListener(v -> dismiss());
    }

    void proceed() {
        carAttributesDetailBottomsheetBinding.activityCarDetailsCreate.setOnClickListener(v -> {
            ((CarSelectionActivity)getActivity()).registerCar(vehicle);
            dismiss();
        });
    }
}
