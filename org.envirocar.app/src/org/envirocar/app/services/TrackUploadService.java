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
package org.envirocar.app.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import org.envirocar.app.R;
import org.envirocar.app.handler.TrackRecordingHandler;
import org.envirocar.app.handler.TrackUploadHandler;
import org.envirocar.app.injection.BaseInjectorService;
import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.views.BaseMainActivity;
import org.envirocar.core.entity.Track;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.EnviroCarDB;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;


/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class TrackUploadService extends BaseInjectorService {
    private static final Logger LOG = Logger.getLogger(TrackUploadService.class);
    private static final int NOTIFICATION_ID = 52;

//    @Inject
//    protected TrackRecordingHandler trackRecordingHandler;
    @Inject
    protected EnviroCarDB enviroCarDB;
    @Inject
    protected TrackUploadHandler trackUploadHandler;

    @Override
    public void onCreate() {
        LOG.debug("onCreate()");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        LOG.info("onStartCommand()");


        // TODO change it to clean u
        List<Track> localTrackList = enviroCarDB.getAllLocalTracks().blockingFirst();
        if (localTrackList.size() > 0) {
            LOG.info(String.format("%s local tracks to upload", localTrackList.size()));

//            setNotification("yeae", "oiad");
            uploadAllLocalTracks();
        } else {
            LOG.info("No local tracks to upload");
//            setNotification("yeae", "oiad");
            try {
                finalize();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }


        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent appComponent) {
        appComponent.inject(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }

    private void uploadAllLocalTracks() {
//        Observable.defer(() -> enviroCarDB.getAllLocalTracks())
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .singleElement()
//                .toObservable()
//                .concatMap(tracks -> getUploadWithNotificationObservable(tracks))
//                .subscribe(new DisposableObserver<Track>() {
//
//                    @Override
//                    public void onStart() {
//                        LOG.info("onStart()");
//                    }
//
//                    @Override
//                    public void onComplete() {
//                        LOG.info("onCompleted()");
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//                        LOG.warn(e.getMessage(), e);
//                    }
//
//                    @Override
//                    public void onNext(Track track) {
//                        LOG.info("Track has been successfully uploaded -> " + track.getRemoteID());
//                    }
//                });
    }

//
//    private Observable<Track> getUploadWithNotificationObservable(final List<Track> tracks) {
//        return Observable.create(new ObservableOnSubscribe<Track>() {
//            private RemoteViews smallView;
//            private RemoteViews bigView;
//            private Notification foregroundNotification;
//            private NotificationManager notificationManager;
//
//            private int numberOfTracks = tracks.size();
//            private int numberOfSuccesses = 0;
//            private int numberOfFailures = 0;
//
//            @Override
//            public void subscribe(ObservableEmitter<Track> emitter) throws Exception {
//                trackUploadHandler.uploadTracksObservable(tracks)
//                        .subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe(new DisposableObserver<Track>() {
//                            @Override
//                            public void onStart() {
//                                foregroundNotification = new NotificationCompat
//                                        .Builder(getApplicationContext())
//                                        .setSmallIcon(R.drawable.ic_cloud_upload_white_24dp)
//                                        .setContentTitle(
//                                                getString(R.string.notification_automatic_track_upload_title))
//                                        .setPriority(Integer.MAX_VALUE)
//                                        .build();
//
//                                smallView = new RemoteViews(getPackageName(),
//                                        R.layout.service_track_upload_handler_notification_small);
//                                bigView = new RemoteViews(getPackageName(),
//                                        R.layout.service_track_upload_handler_notification);
//                                foregroundNotification.bigContentView = bigView;
//
//                                setSmallViewText(
//                                        getString(R.string
//                                                .notification_automatic_track_upload_title),
//                                        getString(R.string.
//                                                notification_slide_down));
//                                setBigViewText(
//                                        getString(R.string.
//                                                notification_automatic_track_upload_title),
//                                        getString(R.string.
//                                                notification_automatic_track_upload_success_sub)
//                                );
//
//                                notificationManager = (NotificationManager) getSystemService(Context
//                                        .NOTIFICATION_SERVICE);
//                                updateProgress();
//                            }
//
//                            @Override
//                            public void onComplete() {
//                                LOG.info("getUploadWithNotificationObservable.onCompleted()");
//                                emitter.onComplete();
//
//                                setSmallViewText(getString(R.string.
//                                                notification_automatic_track_upload_success),
//                                        getString(R.string.notification_automatic_track_upload_success_sub,
//                                                "" + numberOfSuccesses, "" + numberOfTracks));
//
//                                foregroundNotification = new NotificationCompat
//                                        .Builder(getApplicationContext())
//                                        .setSmallIcon(R.drawable.ic_cloud_upload_white_24dp)
//                                        .setContentTitle(
//                                                getString(R.string.notification_automatic_track_upload_title))
//                                        .setContent(smallView)
//                                        .build();
//
//                                notificationManager.notify(100, foregroundNotification);
//                            }
//
//                            @Override
//                            public void onError(Throwable e) {
//                                emitter.onError(e);
//
//                                setSmallViewText(
//                                        getString(R.string.
//                                                notification_automatic_track_upload_error),
//                                        getString(R.string.
//                                                notification_automatic_track_upload_error_sub));
//
//                                foregroundNotification = new NotificationCompat
//                                        .Builder(getApplicationContext())
//                                        .setSmallIcon(R.drawable.ic_error_outline_white_24dp)
//                                        .setContentTitle("Track Upload Error")
//                                        .setContent(smallView)
//                                        .build();
//
//                                notificationManager.notify(100, foregroundNotification);
//                            }
//
//
//                            @Override
//                            public void onNext(Track track) {
//                                if (track == null) {
//                                    LOG.info("track had to less measurements");
//                                    numberOfFailures++;
//                                } else {
//                                    emitter.onNext(track);
//                                    numberOfSuccesses++;
//                                }
//
//                                updateProgress();
//                            }
//
//                            private void updateProgress() {
//                                int totalNumber = numberOfFailures + numberOfSuccesses;
//
//                                bigView.setProgressBar(
//                                        R.id.service_track_upload_handler_notification_progressbar,
//                                        numberOfTracks,
//                                        numberOfFailures + numberOfSuccesses,
//                                        false);
//
//                                bigView.setTextViewText(
//                                        R.id.service_track_upload_handler_notification_total,
//                                        String.format("%s / %s", totalNumber, numberOfTracks));
//
//                                bigView.setTextViewText(
//                                        R.id.service_track_upload_handler_notification_percentage,
//                                        "" + ((numberOfFailures +
//                                                numberOfSuccesses) / numberOfTracks) * 100);
//
//                                notificationManager.notify(100, foregroundNotification);
//                            }
//
//                            private void setSmallViewText(String title, String content) {
//                                smallView.setTextViewText(
//                                        R.id.service_track_upload_handler_notification_small_title,
//                                        title);
//                                smallView.setTextViewText(
//                                        R.id.service_track_upload_handler_notification_small_content,
//                                        content);
//                            }
//
//                            private void setBigViewText(String title, String content) {
//                                bigView.setTextViewText(
//                                        R.id.service_track_upload_handler_notification_text,
//                                        title);
//                                bigView.setTextViewText(
//                                        R.id.service_track_upload_handler_notification_sub_text,
//                                        content);
//                            }
//                        });
//            }
//        });
//    }

    private void setNotification(String title, String notification) {
        RemoteViews bigView = new RemoteViews(getPackageName(), R.layout
                .service_track_upload_handler_notification);

        RemoteViews smallView = new RemoteViews(getPackageName(), R.layout
                .service_track_upload_handler_notification_small);


        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.home_icon);

        Notification forgroundNotification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_cloud_upload_white_24dp)
                .setContentTitle(title)
                .setContent(smallView)
//                .setAutoCancel(false)
                .setPriority(Integer.MAX_VALUE)
//                .setOngoing(true)
//                .setLargeIcon(bm)
                .build();

        forgroundNotification.bigContentView = bigView;

        Intent intent = new Intent(getBaseContext(), BaseMainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(BaseMainActivity.class);
        stackBuilder.addNextIntent(intent);

        PendingIntent resultIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context
                .NOTIFICATION_SERVICE);

        notificationManager.notify(100, forgroundNotification);
    }

}
