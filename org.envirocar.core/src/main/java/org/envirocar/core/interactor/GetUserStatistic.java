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

import org.envirocar.core.entity.UserStatistic;
import org.envirocar.core.injection.InjectIOScheduler;
import org.envirocar.core.injection.InjectUIScheduler;
import org.envirocar.core.repository.UserStatisticRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Scheduler;

/**
 * @author dewall
 */
@Singleton
public class GetUserStatistic extends Interactor<UserStatistic, GetUserStatistic.Params> {

    private final UserStatisticRepository repository;

    /**
     * Cosntructor.
     *
     * @param observeOn   the thread to observe on.
     * @param subscribeOn the thread to subscribe on.
     */
    @Inject
    public GetUserStatistic(UserStatisticRepository repository, @InjectIOScheduler Scheduler observeOn, @InjectUIScheduler Scheduler subscribeOn) {
        super(observeOn, subscribeOn);
        this.repository = repository;
    }

    @Override
    protected Observable<UserStatistic> buildObservable(Params params) {
        Preconditions.checkNotNull(params);
        return this.repository.getUserStatistic(params.username);
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
