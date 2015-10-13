package org.envirocar.app.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.envirocar.app.NotificationHandler;
import org.envirocar.app.TrackHandler;
import org.envirocar.core.entity.Track;
import org.envirocar.core.injection.Injector;
import org.envirocar.core.logging.Logger;

import javax.inject.Inject;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class TrackUploadService extends Service {
    private static final Logger LOG = Logger.getLogger(TrackUploadService.class);

    @Inject
    protected TrackHandler trackHandler;

    @Inject
    protected NotificationHandler notificationHandler;

    @Override
    public void onCreate() {
        LOG.debug("onCreate()");
        super.onCreate();

        // Inject the TrackHandler;
        ((Injector) getApplicationContext()).injectObjects(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOG.info("onStartCommand()");

        trackHandler.uploadAllTracks()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Track>() {

                    @Override
                    public void onCompleted() {
                        LOG.info("Upload of tracks successful");
                        notificationHandler.createNotification("success");
                        try {
                            finalize();
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        notificationHandler.createNotification("Error");
                    }

                    @Override
                    public void onNext(Track track) {

                    }
                });

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
