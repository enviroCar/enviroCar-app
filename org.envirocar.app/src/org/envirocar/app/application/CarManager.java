package org.envirocar.app.application;

import org.envirocar.app.model.Car;
import org.envirocar.app.model.Car.FuelType;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class CarManager {
	
	public static final String PREF_KEY_CAR_MODEL = "carmodel";
	public static final String PREF_KEY_CAR_MANUFACTURER = "manufacturer";
	public static final String PREF_KEY_CAR_CONSTRUCTION_YEAR = "constructionyear";
	public static final String PREF_KEY_FUEL_TYPE = "fueltype";
	public static final String PREF_KEY_SENSOR_ID = "sensorid";
	public static final String PREF_KEY_CAR_ENGINE_DISPLACEMENT = "pref_engine_displacement";
	
	private static CarManager instance = null;
	
	private Car car;
	
	private SharedPreferences preferences;

	public CarManager(SharedPreferences prefs) {
		this.preferences = prefs;
		createCar();
	}
	
	public static synchronized CarManager instance() {
		if (instance == null) {
			// Initialize first
		}
		return instance;
	}
	
	public static void init (SharedPreferences preferences) {
		if (instance == null) {
			instance = new CarManager(preferences);
		}
	}
	
	public Car getCar() {
		return car;
	}
	
	public boolean isCarSet() {
		return (car != null) ? true : false; 
	}
	
	public void setCat(Car car) {
		this.car = car;
		Editor edit = this.preferences.edit();
		edit.putString(PREF_KEY_FUEL_TYPE, car.getFuelType().toString());
		edit.putInt(PREF_KEY_CAR_CONSTRUCTION_YEAR, car.getConstructionYear());
		edit.putString(PREF_KEY_CAR_MANUFACTURER, car.getManufacturer());
		edit.putString(PREF_KEY_CAR_MODEL, car.getModel());
		edit.putString(PREF_KEY_SENSOR_ID, car.getId());
		edit.putString(PREF_KEY_CAR_ENGINE_DISPLACEMENT, Double.toString(car.getEngineDisplacement()));
		edit.commit();
	}
	
	public void createCar() {
		String fuelType = preferences.getString(PREF_KEY_FUEL_TYPE, null);
		String carManufacturer = preferences.getString(PREF_KEY_CAR_MANUFACTURER, null);
		String carModel = preferences.getString(PREF_KEY_CAR_MODEL, null);
		String sensorId = preferences.getString(PREF_KEY_SENSOR_ID, null);
		
		/*
		 * this is not a real car, must be reloaded from server
		 */
		if (fuelType == null || carManufacturer == null || carModel == null || sensorId == null)
			return;
		
		int year = this.preferences.getInt(PREF_KEY_CAR_CONSTRUCTION_YEAR, 2000);
		double displacement = Float.valueOf(preferences.getString(PREF_KEY_CAR_ENGINE_DISPLACEMENT, "2.0f"));
		FuelType type = null;
		if (fuelType.equalsIgnoreCase(FuelType.GASOLINE.toString())) {
			type = FuelType.GASOLINE;
		} else {
			type = FuelType.DIESEL;
		}
		this.car = new Car(type, carManufacturer, carModel, sensorId, year, displacement);
	}
	
}
