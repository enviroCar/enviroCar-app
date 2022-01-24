package org.envirocar.app.services.trackchunks;

import android.content.Context;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.handler.TrackUploadHandler;
import org.envirocar.app.injection.BaseInjectorService;
import org.envirocar.app.interactor.UploadTrack;
import org.envirocar.core.EnviroCarDB;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Track;
import org.envirocar.core.events.recording.RecordingNewMeasurementEvent;
import org.envirocar.core.exception.DataCreationFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.rx.Optional;
import org.envirocar.remote.dao.RemoteTrackDAO;
import org.envirocar.remote.serde.TrackSerde;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TrackchunkUploadService {

    private static final Logger LOG = Logger.getLogger(TrackchunkUploadService.class);

    private final Scheduler.Worker mMainThreadWorker = Schedulers.io().createWorker();

    private EnviroCarDB enviroCarDB;

    public TrackchunkUploadService(){
        LOG.info("TrackchunkUploadService initialized.");
    }

    private Track currentTrack;

    private Bus eventBus;

    private boolean executed = false;

    private TrackUploadHandler trackUploadHandler;

    public TrackchunkUploadService(Context context, EnviroCarDB enviroCarDB, Bus eventBus, TrackUploadHandler trackUploadHandler) {
        this.enviroCarDB = enviroCarDB;
        this.eventBus = eventBus;
        this.trackUploadHandler = trackUploadHandler;
        try {
            this.eventBus.register(this);
        } catch (IllegalArgumentException e){
            LOG.error("TrackchunkUploadService was already registered.", e);
        }
        LOG.info("TrackchunkUploadService initialized.");
        // enviroCarDB.getActiveTrackObservable(true).doOnComplete(() -> {
        //LOG.info("Track measurements: ");
        //});
//        mMainThreadWorker.schedulePeriodically(() -> {
//            enviroCarDB.getAllLocalTracks(true).observeOn(Schedulers.io())
//                    .subscribeOn(AndroidSchedulers.mainThread())
//                    .subscribeWith(getTracksObserver());
//        }, 1000,1000, TimeUnit.MILLISECONDS);
//        final Observer<Track> trackObserver = enviroCarDB.getActiveTrackObservable(false).observeOn(Schedulers.io())
//                .subscribeOn(AndroidSchedulers.mainThread())
//                .subscribeWith(getActiveTrackObserver());
//        mMainThreadWorker.schedule(() -> {
//            final Observer<Track> trackObserver = enviroCarDB.getActiveTrackObservable(false).observeOn(Schedulers.io())
//                    .subscribeOn(AndroidSchedulers.mainThread())
//                    .subscribeWith(getActiveTrackObserver());
//        }, 5000, TimeUnit.MILLISECONDS);
        mMainThreadWorker.schedulePeriodically(() -> {
            final Observer<Track> trackObserver = enviroCarDB.getActiveTrackObservable(false).observeOn(Schedulers.io())
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(getActiveTrackObserver());
        }, 1000,5000, TimeUnit.MILLISECONDS);
    }

    private Observer<Track> getActiveTrackObserver() {
        return new Observer<Track>() {

            @Override
            public void onSubscribe(Disposable d) {
                LOG.info("onSubscribe");
            }

            @Override
            public void onNext(Track track) {

                int measurements = track.getMeasurements().size();

                if(!executed && measurements > 10) {
                    try {
                        trackUploadHandler.uploadTrackChunkStart(track);
                    } catch (Exception e) {
                        LOG.error(e);
                    }
                    executed = true;
                }
                LOG.info("onNext: Track measurements: " + measurements);
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

    private Observer<List<Track>> getTracksObserver() {
        return new Observer<List<Track>>() {

            @Override
            public void onSubscribe(Disposable d) {
                LOG.info("onSubscribe");
            }


            @Override
            public void onNext(List<Track> tracks) {
                LOG.info("onNext: " + tracks.size());
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

    @Subscribe
    public void onReceiveNewMeasurementEvent(RecordingNewMeasurementEvent event) {
        LOG.info("received new measurement");

    }
}
