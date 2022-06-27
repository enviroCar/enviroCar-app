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
import android.content.DialogInterface;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;


import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.envirocar.app.R;
import org.envirocar.core.entity.Car;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.CarUtils;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.BindView;

/**
 * @author dewall
 */
public class CarSelectionListAdapter extends ArrayAdapter<Car> {
    private static final Logger LOG = Logger.getLogger(CarSelectionListAdapter.class);

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
    private final OnCarListActionCallback mCallback;

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
    public CarSelectionListAdapter(Context context, Car selectedCar, List<Car> values,
                                   OnCarListActionCallback callback) {
        super(context, -1, values);
        this.mContext = context;
        this.mCallback = callback;
        this.mSelectedCar = selectedCar;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // First get the car for which the view needs to be created.
        final Car car = this.getItem(position);

        // Then inflate a new view for the car and create a holder
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        CarViewHolder holder = null;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout
                    .activity_car_selection_layout_carlist_entry, parent, false);
            holder = new CarViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (CarViewHolder) convertView.getTag();
        }

        // set the views
        holder.firstLine.setText(String.format("%s - %s", car.getManufacturer(), car.getModel()));
        holder.secondLine.setText(CarUtils.carAttributesToString(car, getContext()));

        // If this car is the selected car, then set the radio button checked.
        if (mSelectedCar != null && mSelectedCar.equals(car)) {
            mSelectedButton = holder.mRadioButton;
            mSelectedButton.setChecked(true);
        }

        //Start text marquee
        holder.firstLine.setSelected(true);

        final CarViewHolder tmpHolder = holder;
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
        holder.mDeleteButton.setOnClickListener( v -> {
            mCallback.onDeleteCar(car,mSelectedButton);
        });

        // Items of array.car_list_option_items are displayed according to the state of radio button.
        Resources res = getContext().getResources();
        String[] state;
        if (mSelectedCar != null && mSelectedCar.equals(car)) {
            state = res.getStringArray(R.array.car_list_option_item_Delete_car);
        }
        else{
            state = res.getStringArray(R.array.car_list_option_items);
        }

        // Set the onClickListener for a single row.
        convertView.setOnClickListener(v -> new MaterialAlertDialogBuilder(mContext)
                .setTitle(String.format("%s - %s", car.getManufacturer(), car.getModel()))
                .setItems(state, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        switch (i) {
                            case 0:
                                // Call the callback
                                mCallback.onDeleteCar(car, mSelectedButton);
                                break;
                            case 1:
                                if(car.equals(mSelectedCar))
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
                    }
                })
                .show());

        // Return the created view.
        return convertView;
    }

    /**
     * Adds a new {@link Car} to the list and finally invalidates the lsit.
     *
     * @param car the car to add to the list
     */
    protected void addCarItem(Car car) {
        this.add(car);
        if(this.getCount() == 1) {
            this.mSelectedCar = car;
        }
        notifyDataSetChanged();
    }

    /**
     * Removes a {@link Car} from the list and finally invalidates the list.
     *
     * @param car the car to remove from the list.
     */
    protected void removeCarItem(Car car) {
        if (this.getPosition(car) >= 0) {
            this.remove(car);
            notifyDataSetChanged();
        }
    }

    /**
     * Static view holder class that holds all necessary views of a list-row.
     */
    static class CarViewHolder {

        protected final View mCoreView;

        @BindView(R.id.activity_car_selection_layout_carlist_entry_icon)
        protected ImageView iconView;
        @BindView(R.id.activity_car_selection_layout_carlist_entry_firstline)
        protected TextView firstLine;
        @BindView(R.id.activity_car_selection_layout_carlist_entry_secondline)
        protected TextView secondLine;
        @BindView(R.id.activity_car_selection_layout_carlist_entry_radio)
        protected RadioButton mRadioButton;
        @BindView(R.id.activity_car_selection_layout_carlist_delete_icon)
        protected ImageButton mDeleteButton;

        /**
         * Constructor.
         *
         * @param view
         */
        CarViewHolder(View view) {
            this.mCoreView = view;
            ButterKnife.bind(this, view);
        }
    }
}