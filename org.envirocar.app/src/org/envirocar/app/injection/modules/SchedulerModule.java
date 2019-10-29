package org.envirocar.app.injection.modules;

import org.envirocar.core.injection.InjectComputationScheduler;
import org.envirocar.core.injection.InjectIOScheduler;
import org.envirocar.core.injection.InjectUIScheduler;

import dagger.Module;
import dagger.Provides;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

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
