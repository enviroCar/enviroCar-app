package org.envirocar.app.test;

import android.util.Log;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.envirocar.core.entity.Vehicles;
import org.envirocar.storage.EnviroCarVehicleDB;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableSingleObserver;

@RunWith(AndroidJUnit4.class)
public class CarDatabaseTest {

    // for synchronous execution of each test
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    private EnviroCarVehicleDB enviroCarVehicleDB;

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
        Completable.fromAction(() -> enviroCarVehicleDB.vehicleDAO().insert(vehiclesList)).subscribeWith(new DisposableCompletableObserver() {
            @Override
            public void onComplete() {
                Log.i("insertOperation", "successful");
                fetchVehicleData();
            }

            @Override
            public void onError(Throwable e) {
                Log.i("insertOperation", "failed");
            }
        });
    }

    private void fetchVehicleData() {
        Single<List<Vehicles>> vehicle = enviroCarVehicleDB.vehicleDAO().getManufacturerVehiclesList();
        vehicle.subscribeWith(new DisposableSingleObserver<List<Vehicles>>() {
            @Override
            public void onSuccess(List<Vehicles> vehicles) {
                if (vehicles.size() == 1) {
                    Assert.assertEquals(vehicles.size(),1);
                    checkVehicleDatabase(vehicles.get(0));
                }
                else
                    Log.i("countFetchRow", "failed");
            }

            @Override
            public void onError(Throwable e) {
                throw new AssertionError("database is empty");
            }
        });
    }

    private void checkVehicleDatabase(Vehicles vehicle) {
        //primary key not null check
        Assert.assertNotEquals(vehicle.getManufacturer_id(),null);
        Assert.assertNotEquals(vehicle.getId(),null);

        //check entries
        Assert.assertEquals(vehicle.getManufacturer_id(),"0005");
        Assert.assertEquals(vehicle.getId(),"155");
        Assert.assertEquals(vehicle.getCommerical_name(),"645CI");
        Assert.assertEquals(vehicle.getAllotment_date(),"01.07.2003");
        Assert.assertEquals(vehicle.getCategory(),"1");
        Assert.assertEquals(vehicle.getBodywork(),"200");
        Assert.assertEquals(vehicle.getPower_source_id(),"1");
        Assert.assertEquals(vehicle.getEngine_capacity(),"4398");
        Assert.assertEquals(vehicle.getPower(),"245");
        Assert.assertEquals(vehicle.getAxles(),"2");
        Assert.assertEquals(vehicle.getPowered_axles(),"1");
        Assert.assertEquals(vehicle.getSeats(),"4");
        Assert.assertEquals(vehicle.getMaximum_mass(),"2070");
    }

}
