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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.envirocar.app.R;
import org.envirocar.app.databinding.CarDetailCardLayoutBinding;
import org.envirocar.core.entity.Vehicles;

import java.util.List;

public class CarSelectionAttributeListAdapter extends RecyclerView.Adapter<CarSelectionAttributeListAdapter.CarSelectionViewHolder> {


    private Context context;
    private final List<Vehicles> vehiclesList;
    private OnCarInteractionCallback mCallback;

    public CarSelectionAttributeListAdapter(Context mContext, List<Vehicles> vehicles, OnCarInteractionCallback mCallback) {
        this.context = mContext;
        this.vehiclesList = vehicles;
        this.mCallback = mCallback;
    }

    @NonNull
    @Override
    public CarSelectionAttributeListAdapter.CarSelectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final CarDetailCardLayoutBinding binding = CarDetailCardLayoutBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new CarSelectionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CarSelectionAttributeListAdapter.CarSelectionViewHolder holder, int position) {
        final Vehicles vehicle = vehiclesList.get(position);

        // get fuel type name using power source id
        String fuelType = mCallback.resolveFuelType(vehicle.getPower_source_id());
        View contentView = LayoutInflater.from(context).inflate(
                R.layout.fragment_tracklist_delete_track_dialog, null, false);

        // set the details of vehicles in card view
        holder.hsnTsn.setText(vehicle.getManufacturer_id() + "/" + vehicle.getId());
        holder.manufacturerName.setText(vehicle.getManufacturer());
        holder.vehicleName.setText(vehicle.getCommerical_name());
        holder.constructionYear.setText(vehicle.getAllotment_date());
        holder.fuelType.setText(fuelType);
        if (fuelType.equalsIgnoreCase("electric")) {
            holder.engineView.setVisibility(View.GONE);

        } else
            holder.engineCapacity.setText(vehicle.getEngine_capacity() + " cm\u00B3");
        holder.power.setText(vehicle.getPower() + " KW");
        holder.imageView1.setTag("downTag");
        holder.imageView1.setOnClickListener(view -> {

            if (holder.imageView1.getTag() == "downTag") {
                holder.imageView1.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_keyboard_arrow_up_24));
                holder.imageView1.setTag("upTag");
            } else {
                holder.imageView1.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_keyboard_arrow_down_24));
                holder.imageView1.setTag("downTag");
            }
            if (holder.expandCard.getVisibility() == View.VISIBLE) {
                holder.expandCard.setVisibility(View.GONE);
            } else {
                holder.expandCard.setVisibility(View.VISIBLE);
            }
        });

        holder.carDetailView.setOnClickListener(v -> {

            new MaterialAlertDialogBuilder(context, R.style.MaterialDialog)
                    .setTitle(R.string.create_car_dialog)
                    .setMessage(String.format(holder.manufacturerName.getText() + " " + holder.vehicleName.getText() +
                            " " + holder.fuelType.getText() + " " + holder.engineCapacity.getText() + " "))
                    .setIcon(R.drawable.ic_directions_car_black_24dp)
                    .setPositiveButton(R.string.ok,(dialog, which) -> {
                        mCallback.addAndRegisterCar(vehicle);
                    })
                    .setNegativeButton(R.string.cancel,null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return vehiclesList.size();
    }

    public class CarSelectionViewHolder extends RecyclerView.ViewHolder {

        TextView manufacturerName;
        TextView vehicleName;
        TextView constructionYear;
        TextView fuelType;
        TextView engineCapacity;
        TextView power;
        View carDetailView;
        ImageView imageView1;
        View engineView;
        View expandCard;
        TextView hsnTsn;

        public CarSelectionViewHolder(CarDetailCardLayoutBinding binding) {
            super(binding.getRoot());
            manufacturerName = binding.carLayoutManufacturerName;
            vehicleName = binding.carLayoutVehcileName;
            constructionYear = binding.carLayoutConstructionYear;
            fuelType = binding.carLayoutFuelType;
            engineCapacity = binding.carLayoutEngineCapacity;
            power = binding.carLayoutPower;
            carDetailView = binding.carLayoutCard;
            imageView1 = binding.expandView;
            engineView = binding.carLayoutEngineView;
            expandCard = binding.carLayoutExpandedCard;
            hsnTsn = binding.carLayoutHsnTsn;
        }
    }
}
