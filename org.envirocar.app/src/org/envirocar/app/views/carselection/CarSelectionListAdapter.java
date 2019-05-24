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
package org.envirocar.app.views.carselection;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.R;
import org.envirocar.core.entity.Car;
import org.envirocar.core.logging.Logger;

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
        void onDeleteCar(Car car);
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
    private final List<Car> mCars;

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
        this.mCars = values;
        this.mCallback = callback;
        this.mSelectedCar = selectedCar;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // First get the car for which the view needs to be created.
        final Car car = mCars.get(position);

        // Then inflate a new view for the car and create a holder
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context
                .LAYOUT_INFLATER_SERVICE);

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
        holder.mFirstLineText.setText(String.format("%s - %s", car.getManufacturer(), car
                .getModel()));
        holder.mYearText.setText(Integer.toString(car.getConstructionYear()));
        holder.mGasolineText.setText(car.getFuelType().toString());
        holder.mEngineText.setText(String.format("%s ccm",
                Integer.toString(car.getEngineDisplacement())));

        // If this car is the selected car, then set the radio button checked.
        if (mSelectedCar != null && mSelectedCar.equals(car)) {
            mSelectedButton = holder.mRadioButton;
            mSelectedButton.setChecked(true);
        }

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

        // Set the onClickListener for a single row.
        convertView.setOnClickListener(v -> new MaterialDialog.Builder(mContext)
                .items(R.array.car_list_option_items)
                .itemsCallback((materialDialog, view, i, charSequence) -> {
                    switch (i) {
                        case 0:
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
                        case 1:
                            // Uncheck the the previously checked radio button and update the
                            // references accordingly.
                            if (car.equals(mSelectedCar)) {
                                mSelectedCar = null;
                                mSelectedButton.setChecked(false);
                                mSelectedButton = null;
                            }

                            // Call the callback
                            mCallback.onDeleteCar(car);
                            break;
                        default:
                            LOG.warn("No action selected!");
                    }
                })
                .show());

        // Return the created view.
        return convertView;
    }

    @Override
    public Car getItem(int position) {
        return mCars.get(position);
    }

    /**
     * Adds a new {@link Car} to the list and finally invalidates the lsit.
     *
     * @param car the car to add to the list
     */
    protected void addCarItem(Car car) {
        this.mCars.add(car);
        notifyDataSetChanged();
    }

    /**
     * Removes a {@link Car} from the list and finally invalidates the list.
     *
     * @param car the car to remove from the list.
     */
    protected void removeCarItem(Car car) {
        if (mCars.contains(car)) {
            mCars.remove(car);
            notifyDataSetChanged();
        }
    }

    /**
     * Static view holder class that holds all necessary views of a list-row.
     */
    static class CarViewHolder {

        protected final View mCoreView;

        @BindView(R.id.activity_car_selection_layout_carlist_entry_icon)
        protected ImageView mIconView;
        @BindView(R.id.activity_car_selection_layout_carlist_entry_firstline)
        protected TextView mFirstLineText;

        @BindView(R.id.activity_car_selection_layout_carlist_entry_engine)
        protected TextView mEngineText;
        @BindView(R.id.activity_car_selection_layout_carlist_entry_gasoline)
        protected TextView mGasolineText;
        @BindView(R.id.activity_car_selection_layout_carlist_entry_year)
        protected TextView mYearText;

        @BindView(R.id.activity_car_selection_layout_carlist_entry_radio)
        protected RadioButton mRadioButton;

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