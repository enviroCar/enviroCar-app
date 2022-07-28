package org.envirocar.app.services.trackchunks;

import android.content.Context;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.OnLifecycleEvent;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.hwangjr.rxbus.Bus;
import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.annotation.Subscribe;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.events.TrackchunkEndUploadedEvent;
import org.envirocar.app.events.TrackchunkUploadEvent;
import org.envirocar.app.handler.ApplicationSettings;
import org.envirocar.app.handler.TrackDAOHandler;
import org.envirocar.app.handler.TrackUploadHandler;
import org.envirocar.app.injection.BaseInjectorService;
import org.envirocar.app.injection.ScopedBaseInjectorService;
import org.envirocar.app.interactor.UploadTrack;
import org.envirocar.core.EnviroCarDB;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.events.TrackFinishedEvent;
import org.envirocar.core.events.recording.RecordingNewMeasurementEvent;
import org.envirocar.core.exception.DataCreationFailureException;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.Util;
import org.envirocar.core.utils.rx.Optional;
import org.envirocar.remote.dao.RemoteTrackDAO;
import org.envirocar.remote.serde.MeasurementSerde;
import org.envirocar.remote.serde.TrackSerde;
import org.json.JSONException;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TrackchunkUploadService extends BaseInjectorService {

    private static final Logger LOG = Logger.getLogger(TrackchunkUploadService.class);

    private static final int MEASUREMENT_THRESHOLD = 10;

    private final Scheduler.Worker mMainThreadWorker = Schedulers.io().createWorker();

    private EnviroCarDB enviroCarDB;

    private Car car;

    private List<Measurement> measurements;

    public TrackchunkUploadService(){
        LOG.info("TrackchunkUploadService initialized.");
    }

    private Track currentTrack;

    private Bus eventBus;

    private boolean executed = false;

    private TrackUploadHandler trackUploadHandler;

    private MeasurementSerde measurementSerde;

    private TrackDAOHandler trackDAOHandler;

    private boolean isEnabled = false;

    public TrackchunkUploadService(Context context, EnviroCarDB enviroCarDB, Bus eventBus, TrackUploadHandler trackUploadHandler, TrackDAOHandler trackDAOHandler) {
        isEnabled = ApplicationSettings.isTrackchunkUploadEnabled(context);
        this.enviroCarDB = enviroCarDB;
        this.eventBus = eventBus;
        this.trackUploadHandler = trackUploadHandler;
        this.trackDAOHandler = trackDAOHandler;
        measurementSerde = new MeasurementSerde();
        measurements = new ArrayList<>();
        if(isEnabled){
            try {
                this.eventBus.register(this);
            } catch (IllegalArgumentException e){
                LOG.error("TrackchunkUploadService was already registered.", e);
            }
        }
        LOG.info("TrackchunkUploadService initialized. Enabled: " + isEnabled);
    }

    private Observer<Track> getActiveTrackObserver() {
        return new Observer<Track>() {

            @Override
            public void onSubscribe(Disposable d) {
                LOG.info("onSubscribe");
            }

            @Override
            public void onNext(Track track) {
                TrackchunkUploadService.this.setCar(track.getCar());
                if(!executed) {
                    executed = true;
                    try {
                        currentTrack = trackUploadHandler.uploadTrackChunkStart(track);
                        LOG.info("Track remote id: " + currentTrack.getRemoteID());
                    } catch (Exception e) {
                        LOG.error(e);
                        TrackchunkUploadService.this.eventBus.unregister(TrackchunkUploadService.this);
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                LOG.info("onError: " + e.getMessage());
            }

            @Override
            public void onComplete() {
                LOG.info( "onComplete");
            }
        };
    }

    private void setCar(Car car) {
        this.car = car;
        LOG.info("received car: " + car);
    }

    @Subscribe
    public void onReceiveNewMeasurementEvent(RecordingNewMeasurementEvent event) {
        if(!isEnabled){
            return;
        }
        if(!executed) {
            final Observer<Track> trackObserver = enviroCarDB.getActiveTrackObservable(false).observeOn(Schedulers.io())
                    .subscribeOn(Schedulers.io())
                    .subscribeWith(getActiveTrackObserver());
        }
        measurements.add(event.mMeasurement);
        LOG.info("received new measurement" + this);
        if(measurements.size() > MEASUREMENT_THRESHOLD) {
           List<Measurement> measurementsCopy = new ArrayList<>(measurements.size() + 1);
            measurementsCopy.addAll(measurements);
            JsonArray trackFeatures = createMeasurementJson(measurementsCopy);
            LOG.info("trackFeatures" + trackFeatures);
            try {
                trackUploadHandler.uploadTrackChunk(currentTrack.getRemoteID(), trackFeatures);
            } catch (Exception e){
                LOG.error("Could not upload track chunk", e);
                this.eventBus.post(new TrackchunkUploadEvent(TrackchunkUploadEvent.FAILED));
                return;
            }
            this.eventBus.post(new TrackchunkUploadEvent(TrackchunkUploadEvent.SUCCESSFUL));
            measurements.clear();
        }
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent appComponent) {
        appComponent.inject(this);
    }

    private List<Measurement> getMeasurements() {
        return this.measurements;
    }

    private Car getCar() {
        return this.car;
    }

    private JsonArray createMeasurementJson(List<Measurement> measurements) {
        // serialize the array of features.
        JsonArray trackFeatures = new JsonArray();
        if (measurements == null || measurements.isEmpty()) {
            LOG.severe("Track did not contain any non obfuscated measurements.");
            return null;
        }

        try {
            for (Measurement measurement : measurements) {
                JsonElement measurementJson = createMeasurementProperties(
                        measurement, getCar());
                trackFeatures.add(measurementJson);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return trackFeatures;
    }

    private JsonElement createMeasurementProperties(Measurement src, Car car) throws
            JSONException {
        // Create the Geometry json object
        JsonObject geometryJsonObject = new JsonObject();
        geometryJsonObject.addProperty(Track.KEY_TRACK_TYPE, "Point");

        // Create the coordinates of the geometry json object
        JsonArray coordinatesArray = new JsonArray();
        coordinatesArray.add(new JsonPrimitive(src.getLongitude()));
        coordinatesArray.add(new JsonPrimitive(src.getLatitude()));
        geometryJsonObject.add(Track.KEY_TRACK_FEATURES_GEOMETRY_COORDINATES,
                coordinatesArray);

        // Create measurement properties.
        JsonObject propertiesJson = new JsonObject();
        propertiesJson.addProperty(Track.KEY_TRACK_FEATURES_PROPERTIES_TIME,
                Util.longToIsoDate(src.getTime()));
        propertiesJson.addProperty("sensor", car.getId());

        // Add all measured phenomenons to this measurement.
        JsonObject phenomenons = createPhenomenons(src, car.getFuelType() == Car.FuelType.DIESEL);
        if (phenomenons != null) {
            propertiesJson.add(Track.KEY_TRACK_FEATURES_PROPERTIES_PHENOMENONS, phenomenons);
        }

        // Create the final json Measurement
        JsonObject result = new JsonObject();
        result.addProperty("type", "Feature");
        result.add(Track.KEY_TRACK_FEATURES_GEOMETRY, geometryJsonObject);
        result.add(Track.KEY_TRACK_FEATURES_PROPERTIES, propertiesJson);

        return result;
    }

    private JsonObject createPhenomenons(Measurement measurement, boolean isDiesel) throws
            JSONException {
        if (measurement.getAllProperties().isEmpty()) {
            return null;
        }

        JsonObject result = new JsonObject();
        Map<Measurement.PropertyKey, Double> props = measurement.getAllProperties();
        for (Measurement.PropertyKey key : props.keySet()) {
            if (TrackSerde.supportedPhenomenons.contains(key)) {
                if (isDiesel && (key == Measurement.PropertyKey.CO2 || key == Measurement.PropertyKey.CONSUMPTION)) {
                    // DO NOTHING TODO delete when necessary
                } else {
                    result.add(key.toString(), TrackSerde.createValue(props.get(key)));
                }
            }
        }
        return result;
    }

    @Subscribe
    public void onReceiveTrackFinishedEvent(final TrackFinishedEvent event) {
        if(!isEnabled){
            return;
        }
        LOG.info(String.format("onReceiveTrackFinishedEvent(): event=%s", event.toString()));
        mMainThreadWorker.schedule(() -> {
            try {
                trackUploadHandler.uploadTrackChunkEnd(currentTrack);
            } catch (NotConnectedException | UnauthorizedException e) {
                LOG.error("Could not finish track.", e);
                //TODO handle unfinished track
                return;
            }
            LOG.info("Delete local track.");
            enviroCarDB.deleteTrack(currentTrack);
            this.eventBus.post(new TrackchunkEndUploadedEvent());
        },5000, TimeUnit.MILLISECONDS);
        try {
            this.eventBus.unregister(this);
        } catch (IllegalArgumentException e){
            LOG.error("TrackchunkUploadService not unregistered.", e);
        }
    }
}
