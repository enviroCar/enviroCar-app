package org.envirocar.app.test.dao;

import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.envirocar.app.R;
import org.envirocar.core.EnviroCarDB;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.CarImpl;
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
}
