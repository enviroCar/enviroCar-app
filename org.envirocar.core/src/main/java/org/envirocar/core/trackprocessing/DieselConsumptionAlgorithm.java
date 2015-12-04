package org.envirocar.core.trackprocessing;

import com.google.common.base.Preconditions;

import org.envirocar.core.entity.Measurement;
import org.envirocar.core.exception.FuelConsumptionException;
import org.envirocar.core.exception.UnsupportedFuelTypeException;

import static org.envirocar.core.entity.Measurement.PropertyKey.*;

/**
 *
 * This implements the diesel fuel calculations based on the following parameters:<br/>
 *
 * <ul>
 *     <li>Lambda Voltage equivalence ratio</li>
 *     <li>MAF</li>
 *     <li>a set of pre-calculated co-efficients</li>
 * </ul>
 *
 * Lambda voltage equivalence ratio might not be available in some situations (in deed it is capped
 * for some cars), thus a a regression function (using lambda voltage) is used to derive the
 * actual lambda voltage equivalence ratio above the capped values:
 *
 * <pre>lambda ER = x1 / ( x2 - x3 * lambda_voltage )</pre>
 *
 * with actual values:
 *
 * <pre>lambda ER = 0.23478 / ( 0.218911 - 0.18415 * lambda_voltage )</pre>
 *
 */
public class DieselConsumptionAlgorithm implements ConsumptionAlgorithm {

    /**
     * regression function co-efficients
     */
    private static final double CO_EFFICIENT_X1 = 0.23478;
    private static final double CO_EFFICIENT_X2 = 0.218911;
    private static final double CO_EFFICIENT_X3 = 0.18415;

    /**
     * the minimum required air for diesel engines
     */
    private static final double MINIMUM_REQUIRED_AIR = 14.5;

    /**
     * density of diesel fuel
     */
    private static final double FUEL_DENSITY = 0.832;

    @Override
    public double calculateConsumption(Measurement measurement) throws FuelConsumptionException, UnsupportedFuelTypeException {
        Preconditions.checkNotNull(measurement);

        if (!measurement.hasProperty(LAMBDA_VOLTAGE_ER)
                && !measurement.hasProperty(LAMBDA_VOLTAGE)) {
            throw new FuelConsumptionException("No lambda voltage values available");
        }

        /**
         * we assume a consumption of zero if the lambda voltage exceeds 1.1
         */
        Double lambdaV = measurement.getProperty(LAMBDA_VOLTAGE);
        if (lambdaV > 1.1) {
            return 0.0;
        }

        double lambdaER = calculateLambdaVoltagER(measurement.getProperty(LAMBDA_VOLTAGE_ER), lambdaV);
        double maf = resolveMassAirFlow(measurement);

        /**
         * calculate mass fuel flow
         */
        double massFuelFlow =  (maf * 3600) / (lambdaER * MINIMUM_REQUIRED_AIR);

        /**
         * calculate volumetric fuel flow
         */
        return massFuelFlow * FUEL_DENSITY;
    }

    private double resolveMassAirFlow(Measurement measurement) throws FuelConsumptionException {
        if (measurement.hasProperty(MAF)) {
            return measurement.getProperty(MAF);
        }
        else if (measurement.hasProperty(CALCULATED_MAF)) {
            return measurement.getProperty(CALCULATED_MAF);
        }

        throw new FuelConsumptionException("No MAF value available");
    }

    private double calculateLambdaVoltagER(double lambdaER, double lambdaV) throws FuelConsumptionException {
        /**
         * we will use the provided lambda ER if it is less than 1.97 (= the observed capped max)
         */
        if (lambdaER <= 1.97) {
            return lambdaER;
        }

        /**
         * we calculate the lambda ER using the regression function
         */
        double denominator = CO_EFFICIENT_X2 - CO_EFFICIENT_X3 * lambdaV;

        //check if we might get into divided by zero
        if (denominator == 0.0) {
            throw new FuelConsumptionException("Invalid lambda parameters would result in division by zero");
        }

        return CO_EFFICIENT_X1 / denominator;
    }

    @Override
    public double calculateCO2FromConsumption(double consumption) throws FuelConsumptionException {
        return consumption * 2.65; //kg/h
    }
}
