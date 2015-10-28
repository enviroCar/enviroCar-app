package org.envirocar.core.trackprocessing;

import org.envirocar.core.exception.FuelConsumptionException;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.UnsupportedFuelTypeException;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface TrackStatisticsProvider {

    double getDistanceOfTrack();

    double getFuelConsumptionPerHour() throws FuelConsumptionException;

    double getCO2Average() throws FuelConsumptionException;

    double getLiterPerHundredKm() throws FuelConsumptionException, NoMeasurementsException;

    double getGramsPerKm() throws FuelConsumptionException, NoMeasurementsException,
            UnsupportedFuelTypeException;

}
