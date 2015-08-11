/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.app.application;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.squareup.otto.Bus;

import org.envirocar.app.R;
import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.activity.preference.CarSelectionPreference;
import org.envirocar.app.events.NewCarTypeSelectedEvent;
import org.envirocar.app.injection.InjectApplicationScope;
import org.envirocar.app.injection.Injector;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.dao.DAOProvider;
import org.envirocar.app.model.dao.exception.NotConnectedException;
import org.envirocar.app.model.dao.exception.UnauthorizedException;
import org.envirocar.app.util.CarUtils;
import org.envirocar.app.view.preferences.PreferenceConstants;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

/**
 * The manager for cars.
 */
public class CarPreferenceHandler {
    private static final Logger LOGGER = Logger.getLogger(CarPreferenceHandler.class);

    // TODO DELETE.
    public static final String PREF_KEY_CAR_MODEL = "carmodel";
    public static final String PREF_KEY_CAR_MANUFACTURER = "manufacturer";
    public static final String PREF_KEY_CAR_CONSTRUCTION_YEAR = "constructionyear";
    public static final String PREF_KEY_FUEL_TYPE = "fueltype";
    public static final String PREF_KEY_SENSOR_ID = "sensorid";
    public static final String PREF_KEY_CAR_ENGINE_DISPLACEMENT = "pref_engine_displacement";

    @Inject
    @InjectApplicationScope
    protected Context mContext;
    @Inject
    protected Bus mBus;
    @Inject
    protected UserManager mUserManager;
    @Inject
    protected DAOProvider mDAOProvider;

    private Car mSelectedCar;
    private Set<Car> mDeserialzedCars;
    private Set<String> mSerializedCarStrings;

    /**
     * Constructor.
     *
     * @param context the context of the activity or application.
     */
    public CarPreferenceHandler(Context context) {
        // Inject all annotated fields.
        ((Injector) context).injectObjects(this);

        // get the default PreferenceManager
        final SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(context);

        mSelectedCar = CarSelectionPreference.instantiateCar(
                preferences.getString(SettingsActivity.CAR, null));

        // Get the serialized car strings of all added cars.
        mSerializedCarStrings = preferences
                .getStringSet(PreferenceConstants.PREFERENCE_TAG_CARS, Sets.newHashSet());

        // Instantiate the cars from the set of serialized strings.
        mDeserialzedCars = Sets.newHashSet();
        for (String serializedCar : mSerializedCarStrings) {
            mDeserialzedCars.add(CarUtils.instantiateCar(serializedCar));
        }
    }

    /**
     * Adds the car to the set of cars in the shared preferences.
     *
     * @param car the car to add to the shared preferences.
     * @return true if the car has been successfully added.
     */
    public boolean addCar(Car car) {
        LOGGER.info(String.format("addCar(%s %s)", car.getManufacturer(), car.getModel()));
        // Serialize the car.
        String serializedCar = CarUtils.serializeCar(car);

        // if the car is not already part of the list, add it
        if (mDeserialzedCars.contains(car) || mSerializedCarStrings.contains(serializedCar))
            return false;
        mSerializedCarStrings.add(serializedCar);
        mDeserialzedCars.add(car);

        // Finally flush the state to shared preferences
        flushCarListState();
        return true;
    }

    /**
     * Removes the car from the set of cars in the shared preferences.
     *
     * @param car the car to remove from the shared preferences.
     * @return true if the car has been successfully deleted.
     */
    public boolean removeCar(Car car) {
        LOGGER.info(String.format("removeCar(%s %s)", car.getManufacturer(), car.getModel()));

        // If the cartype equals the selected car, then set it to null and fire an event on the
        // event bus.
        if (mSelectedCar != null && mSelectedCar.equals(car)) {
            LOGGER.info(String.format("%s %s equals the selected car type.",
                    car.getManufacturer(), car.getModel()));
            mSelectedCar = null;

            flushSelectedCarState();
            mBus.post(new NewCarTypeSelectedEvent(null));
        }

        // Get the serialized car representation
        String serializedCar = CarUtils.serializeCar(car);

        // Return false when the car is not contained in the set.
        if (!mDeserialzedCars.contains(car) && !mSerializedCarStrings.contains(serializedCar))
            return false;

        // Remove from both sets.
        mSerializedCarStrings.remove(serializedCar);
        mDeserialzedCars.remove(car);

        // Finally flush the state to shared preferences
        flushCarListState();
        return true;
    }

    /**
     * Returns the instance of the current Car
     *
     * @return instance of the selected car.
     */
    public Car getCar() {
        return mSelectedCar;
    }

    /**
     * @param c
     */
    public void setCar(Car c) {
        LOGGER.info(String.format("setCar(%s %s)", c.getManufacturer(), c.getModel()));
        if (c == null || (mSelectedCar != null && this.mSelectedCar.equals(c))) {
            LOGGER.info("setCar(): car is null or the same as already set");
            return;
        }

        // Set the selected car and flush the car state.
        this.mSelectedCar = c;
        flushSelectedCarState();

        // Post a new event holding the new setted car type.
        mBus.post(new NewCarTypeSelectedEvent(mSelectedCar));
    }

    /**
     * Getter method for the list of deserialized cars in sorted order.
     *
     * @return list of sorted cars. (Sorted by manufacturer and model)
     */
    public List<Car> getDeserialzedCars() {
        List<Car> carList = Lists.newArrayList(mDeserialzedCars);
        Collections.sort(carList, new Comparator<Car>() {
            @Override
            public int compare(Car lhs, Car rhs) {
                int res = lhs.getManufacturer().compareTo(rhs.getManufacturer());
                if (res == 0)
                    res = lhs.getModel().compareTo(rhs.getModel());
                return res;
            }
        });
        return carList;
    }

    /**
     * Registers a new car at the server.
     *
     * @param car the car to register
     */
    public void registerCarAtServer(final Car car) {
        try {
            if (car.getFuelType() == null || car.getManufacturer() == null || car.getModel() ==
                    null || car.getConstructionYear() == 0 || car.getEngineDisplacement() == 0)
                throw new Exception("Empty value!");
            if (car.getManufacturer().isEmpty() || car.getModel().isEmpty()) {
                throw new Exception("Empty value!");
            }

        } catch (Exception e) {
            //TODO i18n
            Toast.makeText(mContext, "Not all values were defined.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (new ContextInternetAccessProvider(mContext).isConnected()
                && mUserManager.isLoggedIn()) {
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        String sensorId = mDAOProvider.getSensorDAO().saveSensor(car);

                        //put the sensor id into shared preferences
                        car.setId(sensorId);

                    } catch (final NotConnectedException e1) {
                        LOGGER.warn(e1.getMessage());
                    } catch (final UnauthorizedException e1) {
                        LOGGER.warn(e1.getMessage());
                    }

                    return null;
                }

            }.execute();
        } else {
            String uuid = UUID.randomUUID().toString();
            String sensorId = Car.TEMPORARY_SENSOR_ID.concat(uuid.substring(0, uuid.length() -
                    Car.TEMPORARY_SENSOR_ID.length()));
            car.setId(sensorId);
        }
    }


    /**
     * Getter method for the serialized car strings.
     *
     * @return the serialized car strings
     */
    public Set<String> getSerializedCars() {
        return mSerializedCarStrings;
    }

    /**
     * Stores and updates the current set of serialized car strings in the shared preferences of
     * the application.
     */
    private void flushCarListState() {
        LOGGER.info("flushCarListState()");

        // First, delete the entry set of serialized car strings. Very important here to note is
        // that there has to be a commit happen before setting the next string set.
        boolean deleteSuccess = PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                .remove(PreferenceConstants.PREFERENCE_TAG_CARS)
                .commit();

        // then set the new string set.
        boolean insertSuccess = PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                .putStringSet(PreferenceConstants.PREFERENCE_TAG_CARS, mSerializedCarStrings)
                .commit();

        if (deleteSuccess && insertSuccess)
            LOGGER.info("flushCarListState(): Successfully inserted into shared preferences");
        else
            LOGGER.severe("flushCarListState(): Error on insert.");
    }

    /**
     * Stores and updates the currently selected car in the shared preferences of the application.
     */
    private void flushSelectedCarState() {
        LOGGER.info("flushSelectedCarState()");

        // Delete the entry of the selected car and its hash code.
        boolean deleteSuccess = PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                .remove(PreferenceConstants.PREFERENCE_TAG_CAR)
                .remove(PreferenceConstants.CAR_HASH_CODE)
                .commit();

        if (deleteSuccess)
            LOGGER.info("flushSelectedCarState(): Successfully deleted from the shared " +
                    "preferences");
        else
            LOGGER.severe("flushSelectedCarState(): Error on delete.");

        if (mSelectedCar != null) {
            // Set the new selected car type and hashcode.
            boolean insertSuccess = PreferenceManager.getDefaultSharedPreferences(mContext)
                    .edit()
                    .putString(PreferenceConstants.PREFERENCE_TAG_CAR,
                            CarUtils.serializeCar(mSelectedCar))
                    .putInt(PreferenceConstants.CAR_HASH_CODE, mSelectedCar.hashCode())
                    .commit();

            if (insertSuccess)
                LOGGER.info("flushSelectedCarState(): Successfully inserted into shared " +
                        "preferences");
            else
                LOGGER.severe("flushSelectedCarState(): Error on insert.");
        }
    }

}
