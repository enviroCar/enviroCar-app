/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.handler.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.PreferenceConstants;
import org.envirocar.core.ContextInternetAccessProvider;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Track;
import org.envirocar.core.events.NewCarTypeSelectedEvent;
import org.envirocar.core.events.NewUserSettingsEvent;
import org.envirocar.core.exception.DataCreationFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.CarUtils;
import org.envirocar.storage.EnviroCarDB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;


/**
 * The manager for cars.
 */
@Singleton
public class CarPreferenceHandler {
    private static final Logger LOG = Logger.getLogger(CarPreferenceHandler.class);
    private static final String PREFERENCE_TAG_DOWNLOADED = "cars_downloaded";

    private final Context mContext;
    private final Bus mBus;
    private final UserHandler mUserManager;
    private final DAOProvider mDAOProvider;
    private final EnviroCarDB mEnviroCarDB;
    private final SharedPreferences mSharedPreferences;

    private Car mSelectedCar;
    private Set<Car> mDeserialzedCars;
    private Set<String> mSerializedCarStrings;
    private Map<String, String> temporaryAlreadyRegisteredCars = new HashMap<>();

    /**
     * Constructor.
     *
     * @param context the context of the activity or application.
     */
    @Inject
    public CarPreferenceHandler(@InjectApplicationScope Context context, Bus bus, UserHandler
            userManager, DAOProvider daoProvider, EnviroCarDB enviroCarDB,
                                SharedPreferences sharedPreferences) {
        this.mContext = context;
        this.mBus = bus;
        this.mUserManager = userManager;
        this.mDAOProvider = daoProvider;
        this.mEnviroCarDB = enviroCarDB;
        this.mSharedPreferences = sharedPreferences;

        // no unregister required because it is applications scoped.
        this.mBus.register(this);

        mSelectedCar = CarUtils.instantiateCar(sharedPreferences.getString(PreferenceConstants
                .PREFERENCE_TAG_CAR, null));

        // Get the serialized car strings of all added cars.
        mSerializedCarStrings = sharedPreferences
                .getStringSet(PreferenceConstants.PREFERENCE_TAG_CARS, new HashSet<>());

        // Instantiate the cars from the set of serialized strings.
        mDeserialzedCars = new HashSet<>();
        for (String serializedCar : mSerializedCarStrings) {
            Car car = CarUtils.instantiateCar(serializedCar);
            if (car == null) {
                mSerializedCarStrings.remove(serializedCar);
                flushCarListState();
            } else {
                mDeserialzedCars.add(CarUtils.instantiateCar(serializedCar));
            }
        }
    }

    public Observable<List<Car>> getAllDeserializedCars() {
        return Observable.create(emitter -> {
            emitter.onNext(getDeserialzedCars());
            emitter.onComplete();
        });
    }

    public Observable<List<Car>> downloadRemoteCarsOfUser() {
        return Observable.just(mUserManager.getUser())
                .flatMap(user -> mDAOProvider.getSensorDAO().getCarsByUserObservable(user))
                .map(cars -> {
                    LOG.info(String.format(
                            "Successfully downloaded %s remote cars. Add these to the preferences.",
                            cars.size()));
                    for (Car car : cars) {
                        addCar(car);
                    }
                    setIsDownloaded(true);
                    return cars;
                });
    }

    public Observable<Car> assertTemporaryCar(Car car) {
        return Observable.just(car)
                .flatMap(car1 -> {
                    LOG.info("assertTemporaryCar() assert whether car is uploaded or the car " +
                            "needs to be registered.");
                    // If the car is already uploaded, then just return car instance.
                    if (CarUtils.isCarUploaded(car1)) {
                        LOG.info("assertTemporaryCar(): car has already been uploaded");
                        return Observable.just(car1);
                    }

                    // the car is already uploaded before but the car has not the right remote id
                    if (temporaryAlreadyRegisteredCars.containsKey(car1.getId())) {
                        LOG.info("assertTemporaryCar(): car has already been uploaded");
                        car1.setId(temporaryAlreadyRegisteredCars.get(car1.getId()));
                        return Observable.just(car1);
                    }

                    LOG.info("assertTemporaryCar(): car is not uploaded. Trying to register.");
                    // create a new car instance.
                    return registerCar(car1);
                });
    }

    private Observable<Car> registerCar(Car car) {
        LOG.info(String.format("registerCarBeforeUpload(%s)", car.toString()));
        String oldID = car.getId();
        return mDAOProvider.getSensorDAO()
                // Create a new remote car and update the car remote id.
                .createCarObservable(car)
                // update all IDs of tracks that have this car as a reference
                .flatMap(updCar -> updateCarIDsOfTracksObservable(oldID, updCar))
                // sum all tracks to a list of tracks.
                .toList()
                // Just set the current car reference to the updated one and return it.
                .map(tracks -> {
                    LOG.info("kommta hier an?");
                    if (!temporaryAlreadyRegisteredCars.containsKey(oldID))
                        temporaryAlreadyRegisteredCars.put(oldID, car.getId());
                    if (getCar().getId().equals(oldID))
                        setCar(car);
                    return car;
                })
                .toObservable();
    }

    private Observable<Track> updateCarIDsOfTracksObservable(String oldID, Car car) {
        return mEnviroCarDB.getAllTracksByCar(oldID, true)
                .singleOrError()
                .toObservable()
                .flatMap(tracks -> Observable.fromIterable(tracks))
                .map(track -> {
                    LOG.info("Track has been updated! -> [" + track.toString() + "]");
                    track.setCar(car);
                    return track;
                })
                .concatMap(track -> mEnviroCarDB.updateTrackObservable(track));
    }

    /**
     * Adds the car to the set of cars in the shared preferences.
     *
     * @param car the car to add to the shared preferences.
     * @return true if the car has been successfully added.
     */
    public boolean addCar(Car car) {
        LOG.info(String.format("addCar(%s %s)", car.getManufacturer(), car.getModel()));
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
        LOG.info(String.format("removeCar(%s %s)", car.getManufacturer(), car.getModel()));

        // If the cartype equals the selected car, then set it to null and fire an event on the
        // event bus.
        if (mSelectedCar != null && mSelectedCar.equals(car)) {
            LOG.info(String.format("%s %s equals the selected car type.",
                    car.getManufacturer(), car.getModel()));
            mSelectedCar = null;

            flushSelectedCarState();
            mBus.post(new NewCarTypeSelectedEvent(null));
        }


        // Return false when the car is not contained in the set.
        if (!mDeserialzedCars.contains(car))
            return false;

        // Remove from both sets.
        mDeserialzedCars.remove(car);

        // Finally flush the state to shared preferences
        flushCarListState();
        return true;
    }

    /**
     * Returns true if there already are some cars created.
     *
     * @return true if there are some cars.
     */
    public boolean hasCars() {
        return !(mSerializedCarStrings == null || mSerializedCarStrings.isEmpty());
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
        LOG.info(String.format("setCar(%s %s)", c.getManufacturer(), c.getModel()));
        if (c == null || (mSelectedCar != null && this.mSelectedCar.equals(c))) {
            LOG.info("setCar(): car is null or the same as already set");
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
        List<Car> carList = new ArrayList<>(mDeserialzedCars);
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
            if (car.getFuelType() == null ||
                    car.getManufacturer() == null ||
                    car.getModel() == null ||
                    car.getConstructionYear() == 0 ||
                    car.getEngineDisplacement() == 0)
                throw new Exception("Empty value!");
            if (car.getManufacturer().isEmpty() || car.getModel().isEmpty()) {
                throw new Exception("Empty value!");
            }

        } catch (Exception e) {
            //TODO i18n
            Toast.makeText(mContext, "Not all values were defined.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (new ContextInternetAccessProvider(mContext).isConnected() &&
                mUserManager.isLoggedIn()) {
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        String sensorId = mDAOProvider.getSensorDAO().createCar(car);

                        //put the sensor id into shared preferences
                        car.setId(sensorId);

                    } catch (final NotConnectedException e1) {
                        LOG.warn(e1.getMessage());
                    } catch (final UnauthorizedException e1) {
                        LOG.warn(e1.getMessage());
                    } catch (DataCreationFailureException e1) {
                        LOG.warn(e1.getMessage());
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

    @Subscribe
    public void onReceiveNewUserSettingsEvent(NewUserSettingsEvent event) {
        LOG.info("Received NewUserSettingsEvent: " + event.toString());
        if (!event.mIsLoggedIn) {
            setIsDownloaded(false);
            LOG.info("Downloaded setted to false");
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
        LOG.info("flushCarListState()");

        // Recreate serialized car strings.
        mSerializedCarStrings.clear();
        for (Car car : mDeserialzedCars) {
            mSerializedCarStrings.add(CarUtils.serializeCar(car));
        }

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
            LOG.info("flushCarListState(): Successfully inserted into shared preferences");
        else
            LOG.severe("flushCarListState(): Error on insert.");
    }

    /**
     * Stores and updates the currently selected car in the shared preferences of the application.
     */
    private void flushSelectedCarState() {
        LOG.info("flushSelectedCarState()");

        // Delete the entry of the selected car and its hash code.
        boolean deleteSuccess = removeSelectedCarState();

        if (deleteSuccess)
            LOG.info("flushSelectedCarState(): Successfully deleted from the shared " +
                    "preferences");
        else
            LOG.severe("flushSelectedCarState(): Error on delete.");

        if (mSelectedCar != null) {
            // Set the new selected car type and hashcode.
            boolean insertSuccess = PreferenceManager.getDefaultSharedPreferences(mContext)
                    .edit()
                    .putString(PreferenceConstants.PREFERENCE_TAG_CAR,
                            CarUtils.serializeCar(mSelectedCar))
                    .putInt(PreferenceConstants.CAR_HASH_CODE, mSelectedCar.hashCode())
                    .commit();

            if (insertSuccess)
                LOG.info("flushSelectedCarState(): Successfully inserted into shared preferences");
            else
                LOG.severe("flushSelectedCarState(): Error on insert.");
        }
    }

    public void setIsDownloaded(boolean isDownloaded) {
        LOG.info(String.format("setIsDownloaded() to [%s]", isDownloaded));
        mSharedPreferences.edit().remove(PREFERENCE_TAG_DOWNLOADED).commit();
        if (isDownloaded) {
            mSharedPreferences.edit().putBoolean(PREFERENCE_TAG_DOWNLOADED, isDownloaded).commit();
        }
    }

    public boolean isDownloaded() {
        return mSharedPreferences.getBoolean(PREFERENCE_TAG_DOWNLOADED, false);
    }

    private boolean removeSelectedCarState() {
        // Delete the entry of the selected car and its hash code.
        return mSharedPreferences.edit()
                .remove(PreferenceConstants.PREFERENCE_TAG_CAR)
                .remove(PreferenceConstants.CAR_HASH_CODE)
                .commit();
    }

    @Produce
    public NewCarTypeSelectedEvent produceCarTypeSelectedEvent() {
        return new NewCarTypeSelectedEvent(getCar());
    }

}
