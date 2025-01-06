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
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.envirocar.app.R;
import org.envirocar.app.databinding.ActivityCarSelectionLayoutCarlistEntryBinding;
import org.envirocar.core.entity.Car;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.CarUtils;

import java.util.List;

public class CarSelectionRecyclerAdapter extends RecyclerView.Adapter<CarSelectionRecyclerAdapter.CarSelectionViewHolder>{

    private static final Logger LOG = Logger.getLogger(CarSelectionRecyclerAdapter.class);
    /**
     * Simple callback interface for the action types of the car list entries.
     */
    public interface OnCarListActionCallback {
        /**
         * Called whenever a car has been selected to be the used car.
         *
         * @param car the selected car
         */
        void onSelectCar(Car car);

        /**
         * Called whenever a car should be deleted.
         *
         * @param car the selected car.
         */
        void onDeleteCar(Car car, RadioButton mSelectedButton);
    }

    /**
     * Context of the current scope.
     */
    private final Context mContext;

    /**
     * Callback
     */
    private final CarSelectionRecyclerAdapter.OnCarListActionCallback mCallback;
    private final List<Car> mCarList;
    private Car mSelectedCar;
    private RadioButton mSelectedButton;

    /**
     * Constructor.
     *
     * @param context     the context of the current scope.
     * @param selectedCar the car for which the radio button gets checked.
     * @param values      the values to show in the list.
     * @param callback    the callback for list actions
     */
    public CarSelectionRecyclerAdapter(Context context, Car selectedCar, List<Car> values, CarSelectionRecyclerAdapter.OnCarListActionCallback callback) {
        this.mContext = context;
        this.mCallback = callback;
        this.mSelectedCar = selectedCar;
        this.mCarList = values;
    }

    @NonNull
    @Override
    public CarSelectionRecyclerAdapter.CarSelectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CarSelectionViewHolder(ActivityCarSelectionLayoutCarlistEntryBinding.inflate(LayoutInflater.from(mContext)));
    }

    @Override
    public void onBindViewHolder(@NonNull CarSelectionRecyclerAdapter.CarSelectionViewHolder holder, int position) {
        // First get the car for which the view needs to be created.
        final Car car = this.mCarList.get(position);
        LOG.info(String.format("getView() for car: %s", car));

        // set the views
        holder.firstLine.setText(String.format("%s - %s", car.getManufacturer(), car.getModel()));
        holder.secondLine.setText(CarUtils.carAttributesToString(car, mContext));

        if ((mSelectedCar == null && position == 0) || (mSelectedCar != null && mSelectedCar.equals(car))) {
            // if there is no selection at all, use the first one
            // OR
            // If this car is the selected car, then set the radio button checked.
            LOG.debug(String.format("Settings selected state for car view: %d, %s", position, car));
            mSelectedButton = holder.mRadioButton;
            mSelectedButton.setChecked(true);
            holder.firstLine.setSelected(true);
        } else {
            holder.firstLine.setSelected(false);
            holder.mRadioButton.setChecked(false);
        }

        final CarSelectionRecyclerAdapter.CarSelectionViewHolder tmpHolder = holder;
        // set the onClickListener of the radio button.
        holder.mRadioButton.setOnClickListener(v -> {
            if (mSelectedCar == null) {
                mSelectedCar = car;
                mSelectedButton = tmpHolder.mRadioButton;
            } else if (!mSelectedCar.equals(car)) {
                mSelectedCar = car;
                if (mSelectedButton != null)
                    mSelectedButton.setChecked(false);
                mSelectedButton = tmpHolder.mRadioButton;
            }
            tmpHolder.mRadioButton.setChecked(true);
            mCallback.onSelectCar(mSelectedCar);
        });

        // set the onClickListener of the delete button.
        holder.mDeleteButton.setOnClickListener(v -> {
            mCallback.onDeleteCar(car, mSelectedButton);
        });

        // Items of array.car_list_option_items are displayed according to the state of radio button.
        Resources res = mContext.getResources();
        String[] state;
        if (mSelectedCar != null && mSelectedCar.equals(car)) {
            state = res.getStringArray(R.array.car_list_option_item_Delete_car);
        } else {
            state = res.getStringArray(R.array.car_list_option_items);
        }

        // Set the onClickListener for a single row.
        holder.itemView.setOnClickListener(v -> new MaterialAlertDialogBuilder(mContext)
                .setTitle(String.format("%s - %s", car.getManufacturer(), car.getModel()))
                .setItems(state, (dialog, i) -> {
                    switch (i) {
                        case 0:
                            // Call the callback
                            mCallback.onDeleteCar(car, mSelectedButton);
                            break;
                        case 1:
                            if (car.equals(mSelectedCar))
                                return;

                            // Uncheck the currently checked car.
                            if (mSelectedButton != null) {
                                mSelectedButton.setChecked(false);
                            }

                            // Set the new car as selected car type.
                            mSelectedCar = car;
                            mSelectedButton = tmpHolder.mRadioButton;
                            mSelectedButton.setChecked(true);

                            // Call the callback in order to react accordingly.
                            mCallback.onSelectCar(car);
                            break;
                        default:
                            LOG.warn("No action selected!");
                    }
                })
                .show());
    }

    @Override
    public int getItemCount() {
        return mCarList.size();
    }

    /**
     * Adds a new {@link Car} to the list and finally invalidates the lsit.
     *
     * @param car the car to add to the list
     */
    protected void addCarItem(Car car) {
        this.mCarList.add(car);
        notifyDataSetChanged();
    }

    /**
     * Removes a {@link Car} from the list and finally invalidates the list.
     *
     * @param car the car to remove from the list.
     */
    protected void removeCarItem(Car car) {
        if (this.mCarList.contains(car)) {
            notifyItemRemoved(mCarList.indexOf(car));
            this.mCarList.remove(car);
        }
    }

    protected int getPosition(Car car) {
        return this.mCarList.indexOf(car);
    }

    public void clear() {
        this.mCarList.clear();
        notifyDataSetChanged();
    }

    /**
     * Static view holder class that holds all necessary views of a list-row.
     */

    public static class CarSelectionViewHolder extends RecyclerView.ViewHolder {

        protected final View mCoreView;

        protected ImageView iconView;
        protected TextView firstLine;
        protected TextView secondLine;
        protected RadioButton mRadioButton;
        protected ImageButton mDeleteButton;

        /**
         * Constructor.
         *
         * @param binding the binding of the view.
         */
        CarSelectionViewHolder(ActivityCarSelectionLayoutCarlistEntryBinding binding) {
            super(binding.getRoot());
            mCoreView = binding.getRoot();
            iconView = binding.activityCarSelectionLayoutCarlistEntryIcon;
            firstLine = binding.activityCarSelectionLayoutCarlistEntryFirstline;
            secondLine = binding.activityCarSelectionLayoutCarlistEntrySecondline;
            mRadioButton = binding.activityCarSelectionLayoutCarlistEntryRadio;
            mDeleteButton = binding.activityCarSelectionLayoutCarlistDeleteIcon;
        }
    }
}