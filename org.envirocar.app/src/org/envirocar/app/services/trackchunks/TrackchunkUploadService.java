package org.envirocar.app.services.trackchunks;

import android.content.Context;
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
import org.envirocar.core.EnviroCarDB;
import org.envirocar.core.entity.Track;
import org.envirocar.core.events.recording.RecordingNewMeasurementEvent;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.rx.Optional;

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

    public TrackchunkUploadService(Context context, EnviroCarDB enviroCarDB) {
        this.enviroCarDB = enviroCarDB;
        LOG.info("TrackchunkUploadService initialized.");
        // enviroCarDB.getActiveTrackObservable(true).doOnComplete(() -> {
        //LOG.info("Track measurements: ");
        //});
        mMainThreadWorker.schedulePeriodically(() -> {
            enviroCarDB.getAllLocalTracks(true).observeOn(Schedulers.io())
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(getTracksObserver());
        }, 1000,1000, TimeUnit.MILLISECONDS);
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
}
