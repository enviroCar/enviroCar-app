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
package org.envirocar.app.injection.modules;

import org.envirocar.core.injection.InjectComputationScheduler;
import org.envirocar.core.injection.InjectIOScheduler;
import org.envirocar.core.injection.InjectUIScheduler;

import dagger.Module;
import dagger.Provides;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

@Module
public class SchedulerModule {

    @Provides
    @InjectComputationScheduler
    public Scheduler provideComputationScheduler() {
        return Schedulers.computation();
    }

    @Provides
    @InjectIOScheduler
    public Scheduler provideIOScheduler(){
        return Schedulers.io();
    }

    @Provides
    @InjectUIScheduler
    public Scheduler provideUIScheduler(){
        return AndroidSchedulers.mainThread();
    }

}
