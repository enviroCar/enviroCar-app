/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import org.envirocar.app.BaseMainActivity;
import org.envirocar.app.R;
import org.envirocar.app.handler.TrackHandler;
import org.envirocar.app.exception.NotAcceptedTermsOfUseException;
import org.envirocar.core.entity.Track;
import org.envirocar.core.injection.Injector;
import org.envirocar.core.logging.Logger;
import org.envirocar.storage.EnviroCarDB;

import java.util.List;

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
    private static final int NOTIFICATION_ID = 52;

    @Inject
    protected TrackHandler trackHandler;
    @Inject
    protected EnviroCarDB enviroCarDB;

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


        // TODO change it to clean u
        List<Track> localTrackList = enviroCarDB.getAllLocalTracks().first().toBlocking().first();
        if (localTrackList.size() > 0) {
            LOG.info(String.format("%s local tracks to upload", localTrackList.size()));

            trackHandler.uploadAllTracks()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<Track>() {

                        @Override
                        public void onStart() {
                            LOG.info("uploadAllTracks.onStart()");
                            setNotification("enviroCar - Automatic Track Upload", "Uploading all " +
                                    "the local tracks.");
                        }

                        @Override
                        public void onCompleted() {
                            LOG.info("Upload of tracks successful");
                            setNotification("enviroCar - Automatic Track Upload", "Successfully " +
                                    "uploaded all local tracks.");
                            try {
                                finalize();
                            } catch (Throwable throwable) {
                                throwable.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            LOG.error(e.getMessage(), e);
                            if (e instanceof NotAcceptedTermsOfUseException) {
                                setNotification("Error while uploading", "Can't automatically " +
                                        "upload " +
                                        "the tracks. You have not accepted the terms of use.");
                            } else {
                                setNotification("Error while uploading", "Unknown reason");
                            }
                        }

                        @Override
                        public void onNext(Track track) {

                        }
                    });
        } else {
            LOG.info("No local tracks to upload");
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setNotification(String title, String notification) {
        // Prepare the intent
        Intent intent = new Intent(getBaseContext(), BaseMainActivity.class);
        PendingIntent pintent = PendingIntent.getActivity(getBaseContext(), 0, intent, 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getBaseContext())
                        .setSmallIcon(R.drawable.ic_cloud_upload_black_24dp)
                        .setContentTitle(title)
                        .setContentText(notification)
                        .setContentIntent(pintent)
                        .setTicker(notification);

        NotificationManager mNotificationManager =
                (NotificationManager) getBaseContext().getSystemService(Context
                        .NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
