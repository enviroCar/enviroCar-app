package org.envirocar.app.views.carselection;

import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Vehicles;

import java.util.function.Function;

public interface CarSelectionCreation {

    Car createCar(Vehicles vehicle);

    Car.FuelType getFuel(String id);

    void registerCar(Vehicles vehicle);

}
