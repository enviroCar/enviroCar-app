/**
 * Copyright (C) 2013 - 2021 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.interactor;

import android.app.Activity;

import org.envirocar.app.handler.TrackUploadHandler;
import org.envirocar.core.EnviroCarDB;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.TrackUploadException;
import org.envirocar.core.injection.InjectIOScheduler;
import org.envirocar.core.injection.InjectUIScheduler;
import org.envirocar.core.interactor.Interactor;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.rx.OptionalOrError;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.ObservableOperator;
import io.reactivex.Scheduler;
import io.reactivex.observers.DisposableObserver;

/**
 * @author dewall
 */
@Singleton
public class UploadAllTracks extends Interactor<UploadAllTracks.Result, Activity> {
    private static final Logger LOG = Logger.getLogger(UploadAllTracks.class);

    private final TrackUploadHandler uploadHandler;
    private final EnviroCarDB enviroCarDB;

    /**
     * Constructor.
     *
     * @param observeOn
     * @param subscribeOn
     * @param uploadHandler
     * @param enviroCarDB
     */
    @Inject
    public UploadAllTracks(@InjectUIScheduler Scheduler observeOn, @InjectIOScheduler Scheduler subscribeOn, TrackUploadHandler uploadHandler, EnviroCarDB enviroCarDB) {
        super(observeOn, subscribeOn);
        this.uploadHandler = uploadHandler;
        this.enviroCarDB = enviroCarDB;
    }

    @Override
    protected Observable<Result> buildObservable(Activity activity) {
        return Observable.create(emitter -> {
            AtomicInteger numberOfTracks = new AtomicInteger();
            Observable.defer(() -> enviroCarDB.getAllLocalTracks())
                    .concatMap(tracks -> {
                        numberOfTracks.set(tracks.size());
                        return uploadHandler.uploadTracksObservable(tracks);
                    })
                    .subscribe(trackOptionalOrError -> {
                        if (trackOptionalOrError.isSuccessful()) {
                            Track track = trackOptionalOrError.getOptional();
                            emitter.onNext(new Result(numberOfTracks.get(), track, true));
                        } else {
                            Track track = ((TrackUploadException) trackOptionalOrError.getE()).getTrack();
                            emitter.onNext(new Result(numberOfTracks.get(), track, false));
                        }
                    }, emitter::onError, emitter::onComplete);
        });
    }

    public static class Result {
        private final int totalNumberOfTracks;
        private final Track track;
        private final boolean successful;

        public Result(int totalNumberOfTracks, Track track, boolean successful) {
            this.totalNumberOfTracks = totalNumberOfTracks;
            this.track = track;
            this.successful = successful;
        }

        public int getTotalNumberOfTracks() {
            return totalNumberOfTracks;
        }

        public Track getTrack() {
            return track;
        }

        public boolean isSuccessful() {
            return successful;
        }
    }
}
