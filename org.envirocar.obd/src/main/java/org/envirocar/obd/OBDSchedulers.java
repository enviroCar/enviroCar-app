package org.envirocar.obd;

import java.util.concurrent.Executors;

import rx.Scheduler;
import rx.schedulers.Schedulers;

public class OBDSchedulers {

    private static Scheduler instance = Schedulers.from(Executors.newCachedThreadPool());

    public static Scheduler scheduler() {
        return instance;
    }

}
