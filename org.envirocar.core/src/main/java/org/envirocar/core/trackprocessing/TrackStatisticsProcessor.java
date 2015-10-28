package org.envirocar.core.trackprocessing;

import android.location.Location;

import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.exception.FuelConsumptionException;
import org.envirocar.core.exception.UnsupportedFuelTypeException;
import org.envirocar.core.logging.Logger;

import java.util.List;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class TrackStatisticsProcessor {
    private static final Logger LOG = Logger.getLogger(TrackStatisticsProcessor.class);

    protected AbstractConsumptionAlgorithm consumptionAlgorithm;

    /**
     * Constructor.
     *
     * @param fuelType the fuel type of the corresponding car.
     */
    public TrackStatisticsProcessor(Car.FuelType fuelType) {
        consumptionAlgorithm = new BasicConsumptionAlgorithm(fuelType);
    }

    public double computeDistanceOfTrack(List<Measurement> measurements) {
        double distance = 0.0;

        // Return a distance of one if only one measurement exists.
        if (measurements.size() <= 1) {
            return distance;
        }

        Measurement m1 = measurements.get(0);
        Measurement m2;
        float[] distanceArray = new float[1];

        for (int i = 1; i < measurements.size(); i++) {
            m2 = measurements.get(i);
            Location.distanceBetween(m1.getLatitude(), m1.getLongitude(), m2
                    .getLatitude(), m2.getLongitude(), distanceArray);
            distance += distanceArray[0];

            m1 = m2;
            distanceArray[0] = 0;
        }

        return distance / 1000.0d;
    }

    public double getCO2Average(List<Measurement> measurements) throws FuelConsumptionException {
        double co2Avg = 0.0;

        for (Measurement measurement : measurements) {
            Double property = measurement.getProperty(Measurement.PropertyKey.CONSUMPTION);

            if (property != null) {
                co2Avg += consumptionAlgorithm.calculateCO2FromConsumption(property);
            }
        }
        co2Avg /= measurements.size();

        return co2Avg;
    }

    public double getFuelConsumptionPerHour(List<Measurement> measurements) throws
            FuelConsumptionException {
        double consumption = 0.0;

        int consideredCount = 0;
        for (Measurement measurement : measurements) {
            try {
                consumption += consumptionAlgorithm.calculateConsumption(measurement);
                consideredCount++;
            } catch (UnsupportedFuelTypeException e) {
                LOG.debug(e.getMessage());
                //                throw new FuelConsumptionException(e);
            }
        }

        LOG.info(String.format("%s of %s measurements used for consumption/hour calculation",
                consideredCount, measurements.size()));

        return consumption / consideredCount;
    }

    public double getLiterPerHundredKm(double consumptionPerHour, double durationInMillis,
                                       double lengthOfTrack) {
        return consumptionPerHour * durationInMillis / (1000 * 60 * 60) / lengthOfTrack * 100;
    }

    public double getGramsPerKm(double literPerHundredKm, Car.FuelType fuelType) throws
            UnsupportedFuelTypeException {
        if (fuelType.equals(Car.FuelType.GASOLINE)) {
            return literPerHundredKm * 23.3;
        } else if (fuelType.equals(Car.FuelType.DIESEL)) {
            return literPerHundredKm * 26.4;
        } else {
            throw new UnsupportedFuelTypeException(fuelType);
        }
    }
}
