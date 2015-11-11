/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.app;

import android.content.Context;
import android.location.Location;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.MeasurementImpl;
import org.envirocar.core.events.NewMeasurementEvent;
import org.envirocar.obd.MeasurementListener;
import org.envirocar.obd.commands.response.entity.LambdaProbeCurrentResponse;
import org.envirocar.obd.commands.response.entity.LambdaProbeVoltageResponse;
import org.envirocar.obd.events.BluetoothServiceStateChangedEvent;
import org.envirocar.core.events.gps.GpsDOP;
import org.envirocar.core.events.gps.GpsLocationChangedEvent;
import org.envirocar.core.exception.FuelConsumptionException;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.UnsupportedFuelTypeException;
import org.envirocar.core.injection.Injector;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.trackprocessing.AbstractCalculatedMAFAlgorithm;
import org.envirocar.core.trackprocessing.AbstractConsumptionAlgorithm;
import org.envirocar.core.trackprocessing.BasicConsumptionAlgorithm;
import org.envirocar.core.trackprocessing.CalculatedMAFWithStaticVolumetricEfficiency;
import org.envirocar.obd.events.Co2Event;
import org.envirocar.obd.events.ConsumptionEvent;
import org.envirocar.obd.service.BluetoothServiceState;

import javax.inject.Inject;

public class Collector {
    private static final Logger LOG = Logger.getLogger(Collector.class);

    public static final int DEFAULT_SAMPLING_RATE_DELTA = 5000;
    private Measurement measurement = new MeasurementImpl();
    private MeasurementListener callback;
    private Car car;
    private AbstractCalculatedMAFAlgorithm mafAlgorithm;
    private AbstractConsumptionAlgorithm consumptionAlgorithm;
    private boolean fuelTypeNotSupportedLogged;
    private long samplingRateDelta = 5000;

    @Inject
    protected Bus mBus;

    private boolean mIsRegisteredOnTheBus;

    /**
     * Constructor.
     *
     * @param context the context of the current scope.
     * @param l
     * @param car
     */
    public Collector(Context context, MeasurementListener l, Car car) {
        this(context, l, car, DEFAULT_SAMPLING_RATE_DELTA);
    }

    /**
     * Constructor.
     *
     * @param context       the context of the current scope.
     * @param l
     * @param car
     * @param samplingDelta
     */
    public Collector(Context context, MeasurementListener l, Car car, int samplingDelta) {
        // First, inject all annotated fields.
        ((Injector) context).injectObjects(this);

        // then register on the bus.
        this.mBus.register(this);

        this.callback = l;
        this.car = car;

        this.samplingRateDelta = samplingDelta;

        this.mafAlgorithm = new CalculatedMAFWithStaticVolumetricEfficiency(this.car);
        LOG.info("Using MAF Algorithm " + this.mafAlgorithm.getClass());
        this.consumptionAlgorithm = new BasicConsumptionAlgorithm(this.car.getFuelType());
        LOG.info("Using Consumption Algorithm " + this.consumptionAlgorithm.getClass());

        resetMeasurement();
    }


    private void resetMeasurement() {
        measurement.reset();
    }

    public void newLocation(Location l) {
        this.measurement.setLatitude(l.getLatitude());
        this.measurement.setLongitude(l.getLongitude());

        if (l.hasAccuracy() && l.getAccuracy() != 0.0f) {
            this.measurement.setProperty(Measurement.PropertyKey.GPS_ACCURACY, (double) l
                    .getAccuracy());
        }
        if (l.hasBearing()) {
            this.measurement.setProperty(Measurement.PropertyKey.GPS_BEARING, (double) l
                    .getBearing());
        }
        if (l.hasAltitude()) {
            this.measurement.setProperty(Measurement.PropertyKey.GPS_ALTITUDE, l.getAltitude());
        }
        if (l.hasSpeed()) {
            this.measurement.setProperty(Measurement.PropertyKey.GPS_SPEED,
                    meterPerSecondToKilometerPerHour(
                    (double) l.getSpeed()));
        }

        checkStateAndPush();
    }

    private Double meterPerSecondToKilometerPerHour(double speed) {
        return speed * (36.0 / 10.0);
    }

    public void newSpeed(int s) {
        this.measurement.setProperty(Measurement.PropertyKey.SPEED, Double.valueOf(s));
        //		checkStateAndPush();
    }

    public void newMAF(double m) {
        this.measurement.setProperty(Measurement.PropertyKey.MAF, m);
        //		checkStateAndPush();
        fireConsumptionEvent();
    }

    public void newRPM(int r) {
        this.measurement.setProperty(Measurement.PropertyKey.RPM, Double.valueOf(r));
        checkAndCreateCalculatedMAF();
        //		checkStateAndPush();
    }

    /**
     * method checks if the current measurement has everything available for
     * calculating the MAF, and then calculates it.
     */
    private void checkAndCreateCalculatedMAF() {
        if (this.measurement.getProperty(Measurement.PropertyKey.RPM) != null &&
                this.measurement.getProperty(Measurement.PropertyKey.INTAKE_PRESSURE) != null &&
                this.measurement.getProperty(Measurement.PropertyKey.INTAKE_TEMPERATURE) != null) {
            try {
                this.measurement.setProperty(Measurement.PropertyKey.CALCULATED_MAF, this
                        .mafAlgorithm
                        .calculateMAF(this.measurement));
                fireConsumptionEvent();
            } catch (NoMeasurementsException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    private void fireConsumptionEvent() {
        try {
            double consumption = this.consumptionAlgorithm.calculateConsumption(measurement);
            double co2 = this.consumptionAlgorithm.calculateCO2FromConsumption(consumption);

            // fire the events.
            mBus.post(new Co2Event(co2));
            mBus.post(new ConsumptionEvent(consumption));

        } catch (FuelConsumptionException e) {
            LOG.warn(e.getMessage());
        } catch (UnsupportedFuelTypeException e) {
            if (!fuelTypeNotSupportedLogged) {
                LOG.warn(e.getMessage());
                fuelTypeNotSupportedLogged = true;
            }
        }

    }

    public void newIntakeTemperature(int i) {
        this.measurement.setProperty(Measurement.PropertyKey.INTAKE_TEMPERATURE, Double.valueOf(i));
        checkAndCreateCalculatedMAF();
        //		checkStateAndPush();
    }

    public void newIntakePressure(int p) {
        this.measurement.setProperty(Measurement.PropertyKey.INTAKE_PRESSURE, Double.valueOf(p));
        checkAndCreateCalculatedMAF();
        //		checkStateAndPush();
    }

    public void newTPS(int tps) {
        this.measurement.setProperty(Measurement.PropertyKey.THROTTLE_POSITON, Double.valueOf(tps));
    }

    public void newEngineLoad(double load) {
        this.measurement.setProperty(Measurement.PropertyKey.ENGINE_LOAD, load);
    }

    public void newDop(GpsDOP dop) {
        if (dop.hasPdop()) {
            this.measurement.setProperty(Measurement.PropertyKey.GPS_PDOP, dop.getPdop());
        }

        if (dop.hasHdop()) {
            this.measurement.setProperty(Measurement.PropertyKey.GPS_HDOP, dop.getHdop());
        }

        if (dop.hasVdop()) {
            this.measurement.setProperty(Measurement.PropertyKey.GPS_VDOP, dop.getVdop());
        }
    }

    /**
     * currently, this method is only called when a location update
     * was received. as the update rate of the GPS receiver is
     * lower (1 Hz probably) then the update rate of the OBD adapter
     * (revised one) this provides smaller time deltas. A previous location
     * update could be <= 1 second. Following this approach the delta
     * is the maximum of the OBD adapter update rate.
     */
    private synchronized void checkStateAndPush() {
        LOG.warn("checkStateAndPush()");
        if (measurement == null) return;
        if (checkReady(measurement)) {
            try {
                double consumption = this.consumptionAlgorithm.calculateConsumption(measurement);
                double co2 = this.consumptionAlgorithm.calculateCO2FromConsumption(consumption);
                this.measurement.setProperty(Measurement.PropertyKey.CONSUMPTION, consumption);
                this.measurement.setProperty(Measurement.PropertyKey.CO2, co2);
            } catch (FuelConsumptionException e) {
                LOG.warn(e.getMessage());
            } catch (UnsupportedFuelTypeException e) {
                if (!fuelTypeNotSupportedLogged) {
                    LOG.warn(e.getMessage());
                    fuelTypeNotSupportedLogged = true;
                }
            }

			/*
             * update the time as the latest values represent
			 * this measurement
			 */
            measurement.setTime(System.currentTimeMillis());
            LOG.warn("Try to insert Measurement.");
            insertMeasurement(measurement);

            mBus.post(new NewMeasurementEvent(measurement.carbonCopy()));
            resetMeasurement();
        }
    }


    private boolean checkReady(Measurement m) {
        if (m.getLatitude() == 0.0 || m.getLongitude() == 0.0) return false;

        if (System.currentTimeMillis() - m.getTime() < samplingRateDelta) return false;

        if (!m.hasProperty(Measurement.PropertyKey.SPEED) || m.getProperty(Measurement.PropertyKey.SPEED) == null)
            return false;

		/*
         * emulate the legacy behavior: insert measurement despite data might be missing
		 */
        //		if (m.getSpeed() == 0) return false;
        //
        //		if (m.getCO2() == 0.0) return false;
        //
        //		if (m.getConsumption() == 0.0) return false;
        //
        //		if (m.getCalculatedMaf() == 0.0 || m.getMaf() == 0.0) return false;
        //
        //		if (m.getRpm() == 0) return false;
        //
        //		if (m.getIntakePressure() == 0) return false;
        //
        //		if (m.getIntakeTemperature() == 0) return false;

        return true;
    }

    private void insertMeasurement(Measurement m) {
        LOG.warn("Insert measurement");
        callback.insertMeasurement(m.carbonCopy());
    }

    public void newFuelSystemStatus(boolean loop, int status) {
        this.measurement.setProperty(Measurement.PropertyKey.FUEL_SYSTEM_LOOP, loop ? 1d : 0d);
        this.measurement.setProperty(Measurement.PropertyKey.FUEL_SYSTEM_STATUS_CODE, (double) status);
    }

    public void newLambdaProbeValue(LambdaProbeVoltageResponse command) {
        this.measurement.setProperty(Measurement.PropertyKey.LAMBDA_VOLTAGE, command.getVoltage());
        this.measurement.setProperty(Measurement.PropertyKey.LAMBDA_VOLTAGE_ER, command
                .getEquivalenceRatio());
    }

    public void newLambdaProbeValue(LambdaProbeCurrentResponse command) {
        this.measurement.setProperty(Measurement.PropertyKey.LAMBDA_CURRENT, command.getCurrent());
        this.measurement.setProperty(Measurement.PropertyKey.LAMBDA_CURRENT_ER, command
                .getEquivalenceRatio());
    }

    public void newShortTermTrimBank1(Number numberResult) {
        this.measurement.setProperty(Measurement.PropertyKey.SHORT_TERM_TRIM_1, numberResult.doubleValue());
    }

    public void newLongTermTrimBank1(Number numberResult) {
        this.measurement.setProperty(Measurement.PropertyKey.LONG_TERM_TRIM_1, numberResult.doubleValue());
    }


    @Subscribe
    public void onReceiveLocationChangedEvent(GpsLocationChangedEvent event) {
        LOG.warn(String.format("Received event: %s", event.toString()));
        newLocation(event.mLocation);
    }

    @Subscribe
    public void onReceiveBluetoothServiceStateChangedEvent(
            BluetoothServiceStateChangedEvent event) {
        LOG.warn(String.format("Received event: %s", event.toString()));

        // Fix. Whenever the OBDConnection stopps, then unregister from the event bus so that
        // this collector can be garbage collected and no new ghost measurement is created.
        if (event.mState == BluetoothServiceState.SERVICE_STOPPING) {
            mBus.unregister(this);
        }
    }

}
