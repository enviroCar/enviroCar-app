package org.envirocar.obd;

import java.util.concurrent.Executors;

import rx.Scheduler;
import rx.schedulers.Schedulers;

public class OBDSchedulers {

    private static final Scheduler IO_SCHEDULER = Schedulers.from(Executors.newSingleThreadExecutor());

    public static Scheduler io() {
        return IO_SCHEDULER;
    }

}
