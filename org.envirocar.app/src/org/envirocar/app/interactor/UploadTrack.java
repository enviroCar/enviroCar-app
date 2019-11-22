package org.envirocar.app.interactor;

import android.app.Activity;

import org.envirocar.app.handler.TrackUploadHandler;
import org.envirocar.core.entity.Track;
import org.envirocar.core.injection.InjectIOScheduler;
import org.envirocar.core.injection.InjectUIScheduler;
import org.envirocar.core.interactor.Interactor;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Scheduler;

/**
 * @author dewall
 */
@Singleton
public class UploadTrack extends Interactor<Track, UploadTrack.Params> {

    private final TrackUploadHandler trackUploadHandler;

    /**
     * Cosntructor.
     *
     * @param observeOn          the thread to observe on.
     * @param subscribeOn        the thread to subscribe on.
     * @param trackUploadHandler
     */
    @Inject
    public UploadTrack(@InjectUIScheduler Scheduler observeOn, @InjectIOScheduler Scheduler subscribeOn, TrackUploadHandler trackUploadHandler) {
        super(observeOn, subscribeOn);
        this.trackUploadHandler = trackUploadHandler;
    }

    @Override
    protected Observable<Track> buildObservable(UploadTrack.Params params) {
        return trackUploadHandler.uploadTrackObservable(params.track, params.activity);
    }

    public static class Params {
        private final Track track;
        private final Activity activity;

        public Params(Track track) {
            this(track, null);
        }

        public Params(Track track, Activity activity) {
            this.track = track;
            this.activity = activity;
        }

        public Track getTrack() {
            return track;
        }

        public Activity getActivity() {
            return activity;
        }
    }
}
