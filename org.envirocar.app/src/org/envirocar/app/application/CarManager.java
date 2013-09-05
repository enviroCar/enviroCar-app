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
		edit.putFloat(PREF_KEY_CAR_ENGINE_DISPLACEMENT, (float) car.getEngineDisplacement());
		edit.commit();
	}
	
	public void createCar() {
		String fuelType = preferences.getString(PREF_KEY_FUEL_TYPE, "undefined");
		String carManufacturer = preferences.getString(PREF_KEY_CAR_MANUFACTURER, "undefined");
		String carModel = preferences.getString(PREF_KEY_CAR_MODEL, "undefined");
		String sensorId = preferences.getString(PREF_KEY_SENSOR_ID, "undefined");
		int year = this.preferences.getInt(PREF_KEY_CAR_CONSTRUCTION_YEAR, 2000);
		double displacement = preferences.getFloat(PREF_KEY_CAR_ENGINE_DISPLACEMENT, 2.0f);
		FuelType type = null;
		if (fuelType.equalsIgnoreCase(FuelType.GASOLINE.toString())) {
			type = FuelType.GASOLINE;
		} else {
			type = FuelType.DIESEL;
		}
		this.car = new Car(type, carManufacturer, carModel, sensorId, year, displacement);
	}
	
}
