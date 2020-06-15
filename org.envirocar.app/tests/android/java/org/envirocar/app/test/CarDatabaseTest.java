package org.envirocar.app.test;

import android.util.Log;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.envirocar.core.entity.Vehicles;
import org.envirocar.storage.EnviroCarVehicleDB;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.observers.DisposableCompletableObserver;

@RunWith(AndroidJUnit4.class)
public class CarDatabaseTest {

    private EnviroCarVehicleDB enviroCarVehicleDB;

    // for synchronous execution of each test
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    //creating room database in memory for test
    @Before
    public void initEnviroCarDb() throws Exception {
        this.enviroCarVehicleDB = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getContext(),
                EnviroCarVehicleDB.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void closeEnviroCarDb() throws Exception {
        enviroCarVehicleDB.close();
    }

    //Test for vehicle database
    @Test
    public void insertVehicleDataTest() throws InterruptedIOException {
        // data of vehicles
        Vehicles vehicles = new Vehicles();
        vehicles.setManufacturer_id("0005");
        vehicles.setId("155");
        vehicles.setCommerical_name("645CI");
        vehicles.setAllotment_date("01.07.2003");
        vehicles.setCategory("1");
        vehicles.setBodywork("200");
        vehicles.setPower_source_id("1");
        vehicles.setPower("245");
        vehicles.setEngine_capacity("4398");
        vehicles.setAxles("2");
        vehicles.setPowered_axles("1");
        vehicles.setSeats("4");
        vehicles.setMaximum_mass("2070");
        List<Vehicles> vehiclesList = new ArrayList<>();
        vehiclesList.add(vehicles);
        //INSERT INTO memory room database
        Completable.fromAction(()->enviroCarVehicleDB.vehicleDAO().insert(vehiclesList)).subscribeWith(new DisposableCompletableObserver() {
            @Override
            public void onComplete() {
                Log.i("insertSuc","successful");
            }

            @Override
            public void onError(Throwable e) {
                Log.i("insertSuc","false");
            }
        });
    }


}
