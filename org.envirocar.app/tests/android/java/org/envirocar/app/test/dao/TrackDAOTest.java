package org.envirocar.app.test.dao;

import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.envirocar.app.R;
import org.envirocar.core.EnviroCarDB;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.CarImpl;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.MeasurementImpl;
import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.TrackImpl;
import org.envirocar.core.entity.TrackTable;
import org.envirocar.core.exception.TrackSerializationException;
import org.envirocar.storage.EnviroCarDBImpl;
import org.envirocar.storage.TrackRoomDatabase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

@RunWith(AndroidJUnit4.class)
public class TrackDAOTest {

    private EnviroCarDB enviroCarDB;
    private TrackRoomDatabase trackRoomDatabase;

    // Room database instance in memory for test
    @Before
    public void initTrackDb() throws Exception {
        this.trackRoomDatabase = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getContext(),
                TrackRoomDatabase.class)
                .allowMainThreadQueries()
                .build();
        this.enviroCarDB = new EnviroCarDBImpl(trackRoomDatabase);
    }

    @After
    public void closeTrackDb() throws Exception {
        trackRoomDatabase.close();
    }

    @Test
    public void trackTest() {

    }

    // create new car
    private Car createCar() {
        Car car = new CarImpl();
        car.setId("5750591ee4b09078f98673d8");
        car.setConstructionYear(2004);
        car.setManufacturer("Opel");
        car.setModel("Vectra C Caravan");
        car.setFuelType(Car.FuelType.GASOLINE);
        car.setEngineDisplacement(2200);

        return car;
    }

    /*  create new track for intial mesasurements
        initially track length is 0.0
        and track name is the current date and time
     */
    @Test
    public void createTrack() throws TrackSerializationException {
        String date = SimpleDateFormat.getDateInstance().format(new Date());
        Car car = createCar();

        Track track = new TrackImpl();
        track.setCar(car);
        track.setName("Track " + date);
        track.setDescription(String.format("Track with Car %s.", car != null ? car.getModel() : "null"));
        track.setLength(0.0);
        track.setStartTime(new Date().getTime());

        //test for insert and fetch the intial track
        Assert.assertNotNull(enviroCarDB);
        enviroCarDB.insertTrack(track);
        List<TrackTable> numTrack = trackRoomDatabase.getTrackDAONew().getAllTracks();
        Assert.assertTrue("Track name", numTrack.get(0).getName().equals("Track " + date));
        Assert.assertTrue("Car Description", numTrack.get(0).getDescription().equals(String.format("Track with Car %s.", car != null ? car.getModel() : "null")));
        Assert.assertTrue("Expected 1 got" + numTrack.size(), numTrack.size() == 1);
        Assert.assertNotNull(numTrack);
    }

    private Measurement getIntialMeasurement() {
        //initial measurement
        Measurement measurement = new MeasurementImpl();
        // data set for intial measurement
        measurement.setLatitude(6.4847174678758375);
        measurement.setLongitude(51.22546715521443);
        measurement.setTime(Long.parseLong("222800"));

        // initially recording just started so speed is 0
        measurement.setProperty(Measurement.PropertyKey.SPEED,0.00);
        measurement.setProperty(Measurement.PropertyKey.THROTTLE_POSITON,6.127659738063812);
        measurement.setProperty(Measurement.PropertyKey.LAMBDA_VOLTAGE,1.452168180985609);
        measurement.setProperty(Measurement.PropertyKey.LAMBDA_VOLTAGE_ER,0.9903972702086321);
        measurement.setProperty(Measurement.PropertyKey.INTAKE_TEMPERATURE,29.99999910593033);
        measurement.setProperty(Measurement.PropertyKey.CONSUMPTION,1.781552855748061);
        measurement.setProperty(Measurement.PropertyKey.GPS_HDOP,1.5);
        measurement.setProperty(Measurement.PropertyKey.MAF,5.4196322499235805);
        measurement.setProperty(Measurement.PropertyKey.INTAKE_PRESSURE,33.99999949336052);
        measurement.setProperty(Measurement.PropertyKey.ENGINE_LOAD,20.09168342647675);
        measurement.setProperty(Measurement.PropertyKey.GPS_ACCURACY,8.00000011920929);
        measurement.setProperty(Measurement.PropertyKey.GPS_SPEED,0.0);
        measurement.setProperty(Measurement.PropertyKey.GPS_PDOP,3.3);
        measurement.setProperty(Measurement.PropertyKey.RPM,782.9921875);
        measurement.setProperty(Measurement.PropertyKey.GPS_VDOP,2.9);
        measurement.setProperty(Measurement.PropertyKey.CO2,4.186649211007944);
        measurement.setProperty(Measurement.PropertyKey.GPS_ALTITUDE,57.48786670887175);

        return measurement;
    }
}
