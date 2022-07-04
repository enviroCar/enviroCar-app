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
import org.envirocar.core.entity.Track;
import org.envirocar.core.injection.InjectIOScheduler;
import org.envirocar.core.injection.InjectUIScheduler;
import org.envirocar.core.interactor.Interactor;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;

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
