package org.envirocar.app.recording.provider;

import android.content.Context;

import com.squareup.otto.Bus;

import org.envirocar.app.R;
import org.envirocar.app.handler.preferences.CarPreferenceHandler;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.TrackImpl;
import org.envirocar.core.events.recording.RecordingNewMeasurementEvent;
import org.envirocar.core.exception.MeasurementSerializationException;
import org.envirocar.core.exception.TrackSerializationException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.LocationUtils;
import org.envirocar.core.EnviroCarDB;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableTransformer;

/**
 * @author dewall
 */
public class TrackDatabaseSink {
    private static final Logger LOG = Logger.getLogger(TrackDatabaseSink.class);
    private static final DateFormat format = SimpleDateFormat.getDateTimeInstance();

    private final Context context;
    private final CarPreferenceHandler carHandler;
    private final EnviroCarDB enviroCarDB;
    private final Bus eventBus;
    private Track track;

    /**
     * Constructor.
     *
     * @param context
     * @param carHandler
     * @param enviroCarDB
     */
    public TrackDatabaseSink(Context context, CarPreferenceHandler carHandler, EnviroCarDB enviroCarDB, Bus eventBus) {
        this.context = context;
        this.carHandler = carHandler;
        this.enviroCarDB = enviroCarDB;
        this.eventBus = eventBus;
    }

    /**
     * @return
     */
    public ObservableTransformer<Measurement, Track> storeInDatabase() {
        return upstream -> upstream.flatMap(measurement -> Observable.create((ObservableOnSubscribe<Track>) emitter -> {
            LOG.info("Storing new measurement into database");

            // If not rack exists, then create one.
            if (track == null) {
                try {
                    track = createNewTrack(measurement.getTime());
                    emitter.onNext(track);
                } catch (TrackSerializationException e) {
                    LOG.error("Unable to create track instance", e);
                    emitter.onError(e);
                }
            }

            try {
                // inserting measurement
                measurement.setTrackId(track.getTrackID());
                enviroCarDB.insertMeasurement(measurement);

                // updating track information
                track.setEndTime(measurement.getTime());

                // update distance
                int numOfTracks = track.getMeasurements().size();
                if (numOfTracks > 0) {
                    Measurement lastMeasurement = track.getMeasurements().get(numOfTracks - 1);
                    double distanceToLast = LocationUtils.getDistance(lastMeasurement, measurement);
                    track.setLength(track.getLength() + distanceToLast);
                }

                // update track in databse
                track.getMeasurements().add(measurement);
                enviroCarDB.updateTrack(track);
                eventBus.post(new RecordingNewMeasurementEvent(measurement));
                LOG.info("Measurement stored");
            } catch (MeasurementSerializationException e) {
                LOG.error(e.getMessage(), e);
                emitter.onError(e);
            }
        }))
                .doOnDispose(() -> finishTrack(track))
                .doOnComplete(() -> finishTrack(track));
    }

    private Track createNewTrack(long startTime) throws TrackSerializationException {
        String date = format.format(new Date());
        Car car = carHandler.getCar();

        Track track = new TrackImpl();
        track.setCar(car);
        track.setName("Track " + date);
        track.setDescription(String.format(context.getString(R.string.default_track_description), car != null ? car.getModel() : "null"));
        track.setLength(0.0);
        track.setStartTime(startTime);

        enviroCarDB.insertTrack(track);
        return track;
    }

    private void finishTrack(Track track) {
        if (track == null)
            return;
        LOG.info(String.format("Finishing current track %s", track.getDescription()));

        if (track.getMeasurements().size() <= 1) {
            LOG.info("Track had not enough measurements. Deleting track.");
            enviroCarDB.deleteTrack(track);
        } else {
            track.setTrackStatus(Track.TrackStatus.FINISHED);
            enviroCarDB.updateTrack(track);
        }
        this.track = null;
    }

}
