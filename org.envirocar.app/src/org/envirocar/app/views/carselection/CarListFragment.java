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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.envirocar.app.R;
import org.envirocar.app.databinding.FragmentCarListBinding;
import org.envirocar.core.entity.Vehicles;

import java.util.List;

public class CarListFragment extends BottomSheetDialogFragment {
    private FragmentCarListBinding binding;

    RecyclerView recyclerView;
    List<Vehicles> vehiclesList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCarListBinding.inflate(inflater, container, false);

        recyclerView = binding.fragmentCarListView;
        binding.fragmentCarListLayoutCancel.setOnClickListener(v -> cancelSheet());

        CarSelectionAttributeListAdapter carListAdapter = new CarSelectionAttributeListAdapter(getContext(), vehiclesList,
                new OnCarInteractionCallback() {

                    @Override
                    public String resolveFuelType(String power_source_id) {
                        return (getContext().getString((((CarSelectionActivity)getActivity()).getFuel(power_source_id)).getStringResource()));
                    }

                    @Override
                    public void addAndRegisterCar(Vehicles vehicle) {
                        ((CarSelectionActivity)getActivity()).registerCar(vehicle);
                    }
                });
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(carListAdapter);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    CarListFragment(List<Vehicles> vehiclesList) {
      this.vehiclesList = vehiclesList;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    void cancelSheet() {
        dismiss();
    }
}
