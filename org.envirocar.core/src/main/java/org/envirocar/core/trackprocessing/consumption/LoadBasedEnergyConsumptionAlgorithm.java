package org.envirocar.core.trackprocessing.consumption;

import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.exception.FuelConsumptionException;
import org.envirocar.core.exception.UnsupportedFuelTypeException;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class LoadBasedEnergyConsumptionAlgorithm implements ConsumptionAlgorithm {
    // External parameters
    private static final double G = 9.81;               // gravitational acceleration in m/s²
    private static final double RHO_AIR = 1.2;           // air mass density in kg/m³
    private static final double RADIUS_EARTH = 6370000;  // in m

    /* Vehicle-specific values */
    private final double cr;              // rolling resistance coefficient (dimensionless)
    private final double cw;              // air drag coefficient (dimensionless)
    private final double height;          // height of the vehicle in m
    private final double width;           // width of the vehicle in m
    private final double mass;            // total mass of the vehicle (including people) in kg
    private final double idlePower;       // power use at idling state in W
    private final double energyDensity;   // energy density of fuel (gasoline/diesel) in kWh/l

    /* Geometric parameters */
    private double theta = 0.0;                 // road gradient angle in °
    private double distanceMeter;         // distance on spherical earth in m
    private double distanceRadians;       // distance on spherical earth in radians
    private double distanceRadiansTemp;   // temporary variable in radians

    /* Resistance and power */
    private double rollingResistance;     // in N
    private double climbingResistance;    // in N
    private double airResistance;         // in N
    private double inertialResistance;    // in N
    private double drivingResistance;     // in N
    private double power;                 // in W
    private double efficiency;            // dimensionless
    private final double efficiencyMin;   // dimensionless
    private final double efficiencyMax;   // dimensionless

    // previous state
    private boolean isFirstValue = true;  // boolean flag for first value
    private double speedPrev;             // in km/h (prev=previous)
    private int dt;                       // temporal sampling in s
    private double acceleration = 0.0;    // in km/h per second
    private long datePrev;
    private double latitudePrev;          // in °
    private double longitudePrev;         // in °
    private double altitudePrev;          // in m

    private final Car.FuelType fuelType;

    /**
     * Constructor
     */
    public LoadBasedEnergyConsumptionAlgorithm(Car.FuelType fuelType) {
        // vehicle specific values (using default values for now)
        this.cr = 0.02;
        this.cw = 0.3;
        this.height = 1.55;
        this.width = 1.7;
        this.mass = 1500;
        this.idlePower = 2000;

        if (fuelType == Car.FuelType.GASOLINE) {
            this.energyDensity = 8.8;
            this.efficiencyMax = 0.4;
        } else {
            this.energyDensity = 9.9;
            this.efficiencyMax = 0.43;
        }
        this.efficiencyMin = 0.1;

        this.fuelType = fuelType;
    }

    /**
     * @param measurement the measurement providing the required parameters
     * @return the estimated fuel consumption for gasoline.
     * @throws FuelConsumptionException
     * @throws UnsupportedFuelTypeException
     */
    @Override
    public double calculateConsumption(Measurement measurement) throws FuelConsumptionException, UnsupportedFuelTypeException {
        double speedNow = measurement.getProperty(Measurement.PropertyKey.SPEED);
        double datetimeNow = measurement.getTime();
        double longitudeNow = measurement.getLongitude();
        double latitudeNow = measurement.getLatitude();
        double altitudeNow = measurement.getProperty(Measurement.PropertyKey.GPS_ALTITUDE);

        if (!isFirstValue) {
            // calculate acceleration
            this.acceleration = (speedNow - speedPrev) / ((datetimeNow - this.datePrev) / 1000);

            // calculate road gradient angle
            this.distanceRadiansTemp =
                    Math.sin(Math.toRadians(latitudeNow))
                            * Math.sin(Math.toRadians(this.latitudePrev))
                            + Math.cos(Math.toRadians(latitudeNow))
                            * Math.cos(Math.toRadians(this.latitudePrev))
                            * Math.cos(Math.toRadians(longitudeNow) - Math.toRadians(this.longitudePrev));

            // check cases where the argument is outside of the domain of definition
            this.distanceRadiansTemp = Math.min(this.distanceRadiansTemp, 1);
            this.distanceRadiansTemp = Math.max(this.distanceRadiansTemp, -1);

            this.distanceRadians = Math.acos(distanceRadiansTemp);
            this.distanceMeter = this.distanceRadians * this.RADIUS_EARTH;

            // atan returns angle between -pi/2 and pi/2
            this.theta = Math.toDegrees(Math.atan((altitudeNow - this.altitudePrev) / this.distanceMeter));

            // Check cases where the angle is unreasonably large or the distance is very small (unstable results)
            // The steepest slope on a road is measured in New Zealand with 19,3° or 35 %
            if (Math.abs(this.theta) > 19 || distanceMeter < 25) {
                this.theta = 0.0;
            }
        }


        // Calculate driving resistance (in N = kg*m/s²)
        this.rollingResistance = mass * G * cr * Math.cos(Math.toRadians(theta));
        this.climbingResistance = mass * G * Math.sin(Math.toRadians(theta));
        this.airResistance = 0.5 * cw * width * height * RHO_AIR * speedNow / 3.6 * speedNow / 3.6;
        this.inertialResistance = mass * acceleration / 3.6;
        this.drivingResistance = rollingResistance + climbingResistance + airResistance + inertialResistance;

        // Calculate power (in W = kg*m²/s³)
        this.power = drivingResistance * speedNow / 3.6;
        this.power = Math.max(power, idlePower);  // Apply idle power consumption

        // Estimate efficiency
        this.efficiency = drivingResistance * (efficiencyMax - efficiencyMin) / (4000) + efficiencyMin - ((efficiencyMax - efficiencyMin) * (-2000)) / (4000); // linearly interpolate between efficiencyMax at 2000 N and efficiencyMin at -2000 N

        // Check if efficiency is outside defined range
        this.efficiency = Math.max(this.efficiency, this.efficiencyMin);
        this.efficiency = Math.min(this.efficiency, this.efficiencyMax);

        // Cache values
        this.speedPrev = speedNow;
        this.datePrev = (long) datetimeNow;
        this.latitudePrev = latitudeNow;
        this.longitudePrev = longitudeNow;
        this.altitudePrev = altitudeNow;
        this.isFirstValue = false;

        // Calculate consumption (in l/h)
        return (power / 1000 / (energyDensity * efficiency));
    }

    @Override
    public double calculateCO2FromConsumption(double consumption) throws FuelConsumptionException {
        switch (this.fuelType) {
            case DIESEL:
                return consumption * DIESEL_CONSUMPTION_TO_CO2_FACTOR;
            case GASOLINE:
                return consumption * GASOLINE_CONSUMPTION_TO_CO2_FACTOR;
            default:
                throw new FuelConsumptionException(String.format("FuelType {} is not supported", this.fuelType.toString()));
        }
    }
}
