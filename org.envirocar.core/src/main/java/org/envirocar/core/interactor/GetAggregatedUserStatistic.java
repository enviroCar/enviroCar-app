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
package org.envirocar.core.interactor;

import com.google.common.base.Preconditions;

import org.envirocar.core.EnviroCarDB;
import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.UserStatistic;
import org.envirocar.core.entity.internal.AggregatedUserStatistic;
import org.envirocar.core.injection.InjectIOScheduler;
import org.envirocar.core.injection.InjectUIScheduler;
import org.envirocar.core.repository.UserStatisticRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;

@Singleton
public class GetAggregatedUserStatistic extends Interactor<AggregatedUserStatistic, GetAggregatedUserStatistic.Params> {

    private final UserStatisticRepository repository;
    private final EnviroCarDB enviroCarDB;

    @Inject
    public GetAggregatedUserStatistic(
            @InjectUIScheduler Scheduler observeOn, @InjectIOScheduler Scheduler subscribeOn,
            UserStatisticRepository repository, EnviroCarDB enviroCarDB) {
        super(observeOn, subscribeOn);
        this.repository = repository;
        this.enviroCarDB = enviroCarDB;
    }

    @Override
    protected Observable<AggregatedUserStatistic> buildObservable(GetAggregatedUserStatistic.Params params) {
        Preconditions.checkNotNull(params);
        return Observable.create(emitter -> {
            AggregatedUserStatistic result = new AggregatedUserStatistic();

            if (params.username != null) {
                UserStatistic userStatistic = repository.getUserStatistic(params.username).blockingFirst();
                result.setNumTracks(result.getNumTracks() + userStatistic.getTrackCount());
                result.setTotalDistance(result.getTotalDistance() + userStatistic.getDistance());
                result.setTotalDuration(result.getTotalDuration() + userStatistic.getDuration());
            }

            enviroCarDB.getAllLocalTracks(true)
                    .doOnNext(tracks -> {
                        for (Track track : tracks){
                            result.setNumTracks(result.getNumTracks() + 1);
                            result.setTotalDistance(result.getTotalDistance() + track.getLength());
                            result.setTotalDuration(result.getTotalDuration() + track.getDuration());
                        }
                    }).blockingFirst();

            emitter.onNext(result);
            emitter.onComplete();
        });
    }

    public static final class Params {
        private final String username;

        public Params(String username) {
            this.username = username;
        }

        public String getUsername() {
            return username;
        }
    }
}
