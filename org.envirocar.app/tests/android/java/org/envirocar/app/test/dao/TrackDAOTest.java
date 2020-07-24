package org.envirocar.app.test.dao;

import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.envirocar.core.EnviroCarDB;
import org.envirocar.storage.EnviroCarDBImpl;
import org.envirocar.storage.TrackRoomDatabase;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TrackDAOTest {

    private EnviroCarDB enviroCarDB;
    private TrackRoomDatabase trackRoomDatabase;
    @Before
    public void initTrackDb() throws Exception {
         trackRoomDatabase = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getContext(),
                TrackRoomDatabase.class)
                .allowMainThreadQueries()
                .build();
        this.enviroCarDB = new EnviroCarDBImpl(trackRoomDatabase);
    }

    @After
    public  void closeTrackDb() throws Exception {
        trackRoomDatabase.close();
    }
}
