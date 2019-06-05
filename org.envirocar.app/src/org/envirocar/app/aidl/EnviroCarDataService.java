package org.envirocar.app.aidl;

import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import com.squareup.otto.Subscribe;

import org.envirocar.aidl.ECMeasurement;
import org.envirocar.aidl.ECRawObdValue;
import org.envirocar.aidl.IECRecordingService;
import org.envirocar.app.events.GPSSpeedChangeEvent;
import org.envirocar.app.injection.BaseInjectorService;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.events.NewMeasurementEvent;
import org.envirocar.core.events.TrackFinishedEvent;
import org.envirocar.core.events.gps.GpsLocationChangedEvent;
import org.envirocar.core.logging.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * @author dewall
 */
public class EnviroCarDataService extends BaseInjectorService {
    private static final Logger LOG = Logger.getLogger(EnviroCarDataService.class);

    private boolean isRecordingTrack;

    private boolean isOBDConnected;

    private String isRecordingSince;

    private ECMeasurement lastMeasurement;

    @Override
    protected void injectDependencies(BaseApplicationComponent appComponent) {
        appComponent.inject(this);
        this.bus.register(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    @Subscribe
    public void onReceiveTrackFinishedEvent(TrackFinishedEvent event) {
        LOG.info("Received Track Finished Event");
    }


    @Subscribe
    public void onReceiveLocationUpdate(GpsLocationChangedEvent event){
        LOG.info("[] Received new gps speed event " + event.mLocation.toString());

        Location m = event.mLocation;

        // Creating a new measurement for testing purposes.
        Map<String, String> properties = new HashMap<>();
        properties.put("Speed", m.getSpeed() + "");
        ECMeasurement res = new ECMeasurement(
                m.getLatitude(),
                m.getLongitude(),
                m.getTime(),
                properties
        );

//        this.lastMeasurement = res;
    }

    @Subscribe
    public void onReceive(GPSSpeedChangeEvent event){
        LOG.info("[] Received new gps speed event " + event.mGPSSpeed);
    }

    @Subscribe
    public void onReceiveNewMeasurementEvent(NewMeasurementEvent event) {
        LOG.info("[] Received new measurement " + event.mMeasurement.toString());

        Measurement m = event.mMeasurement;

        Map<String, String> properties = new HashMap<>();
        Map<Measurement.PropertyKey, Double> allProperties = m.getAllProperties();
        for (Map.Entry<Measurement.PropertyKey, Double> entry : allProperties.entrySet()) {
            properties.put(entry.getKey().toString(), entry.getValue().toString());
        }

        ECMeasurement res = new ECMeasurement(
                m.getLatitude(),
                m.getLongitude(),
                m.getTime(),
                properties
        );

        this.lastMeasurement = res;
    }

    private final IECRecordingService.Stub binder = new IECRecordingService.Stub() {

        @Override
        public boolean isRecordingTrack() throws RemoteException {
            return EnviroCarDataService.this.isRecordingTrack;
        }

        @Override
        public boolean isOBDConnected() throws RemoteException {
            return EnviroCarDataService.this.isOBDConnected;
        }

        @Override
        public String isRecordingSince() throws RemoteException {
            return EnviroCarDataService.this.isRecordingSince;
        }

        @Override
        public ECMeasurement getLatestMeasurement() throws RemoteException {
            return lastMeasurement;
        }

        @Override
        public String[] listSupportedPhenomenons() throws RemoteException {
            return new String[0];
        }

        @Override
        public ECRawObdValue getLatestObdValue(String s) throws RemoteException {
            return null;
        }

    };

}
