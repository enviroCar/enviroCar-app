package org.envirocar.app.views.carselection;

import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Vehicles;

import java.util.function.Function;

public interface CarSelectionCreation {

    <T> Function<T, Car> createCar(Vehicles vehicle);

    Car.FuelType getFuel(String id);

}
