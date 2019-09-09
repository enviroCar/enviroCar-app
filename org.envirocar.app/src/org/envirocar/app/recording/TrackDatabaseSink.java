package org.envirocar.app.recording;

import android.content.Context;

import com.squareup.otto.Bus;

import org.envirocar.app.R;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.TrackImpl;
import org.envirocar.core.exception.MeasurementSerializationException;
import org.envirocar.core.exception.TrackSerializationException;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.logging.Logger;
import org.envirocar.storage.EnviroCarDB;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableTransformer;
import io.reactivex.observers.DisposableObserver;

/**
 * @author dewall
 */
public class TrackDatabaseSink {
    private static final Logger LOG = Logger.getLogger(TrackDatabaseSink.class);
    private static final DateFormat format = SimpleDateFormat.getDateTimeInstance();

    @Inject
    @InjectApplicationScope
    protected Context context;
    @Inject
    protected CarPreferenceHandler carHandler;
    @Inject
    protected EnviroCarDB enviroCarDB;


    /**
     * @return
     */
    public ObservableTransformer<Measurement, Track> storeInDatabase() {
        return upstream -> Observable.create((ObservableOnSubscribe<Track>) emitter -> upstream.subscribeWith(new DisposableObserver<Measurement>() {
            private Track track;

            @Override
            public void onNext(Measurement measurement) {
                if (isDisposed())
                    return;

                // If not rack exists, then create one.
                if (track == null) {
                    try {
                        track = createNewTrack();
                        emitter.onNext(track);
                    } catch (TrackSerializationException e) {
                        LOG.error("Unable to create track instance", e);
                        dispose();
                    }
                }

                measurement.setTrackId(track.getTrackID());
                track.getMeasurements().add(measurement);

                try {
                    enviroCarDB.insertMeasurement(measurement);
                } catch (MeasurementSerializationException e) {
                    LOG.error(e.getMessage(), e);
                    onError(e);
                    dispose();
                }
            }

            @Override
            public void onError(Throwable e) {
                LOG.error(e);
                finishTrack(track);
            }

            @Override
            public void onComplete() {
                LOG.info("Track has been finished. Finalizing track in database");
                finishTrack(track);
            }
        }));
    }

    private Track createNewTrack() throws TrackSerializationException {
        String date = format.format(new Date());
        Car car = carHandler.getCar();

        Track track = new TrackImpl();
        track.setCar(car);
        track.setName("Track " + date);
        track.setDescription(String.format(context.getString(R.string.default_track_description),
                car != null ? car.getModel() : "null"));

        enviroCarDB.insertTrack(track);
        return track;
    }

    private void finishTrack(Track track) {
        LOG.info(String.format("Finishing current track %s", track.getDescription()));

        track.setTrackStatus(Track.TrackStatus.FINISHED);
        if (track.getMeasurements().size() <= 1) {
            LOG.info("Track had not enough measurements. Deleting track.");
            enviroCarDB.deleteTrack(track);
        } else {
            enviroCarDB.updateTrack(track);
        }
    }

}
