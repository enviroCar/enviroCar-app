package org.envirocar.core.trackprocessing;

import org.envirocar.core.entity.Measurement;
import org.envirocar.core.exception.FuelConsumptionException;
import org.envirocar.core.exception.MeasurementsException;
import org.envirocar.core.exception.UnsupportedFuelTypeException;

import java.util.List;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface TrackStatisticsProvider {

    double getDistanceOfTrack();

    double getFuelConsumptionPerHour() throws FuelConsumptionException;

    double getCO2Average() throws FuelConsumptionException;

    double getLiterPerHundredKm() throws FuelConsumptionException, MeasurementsException;

    double getGramsPerKm() throws FuelConsumptionException, MeasurementsException, UnsupportedFuelTypeException;

}
