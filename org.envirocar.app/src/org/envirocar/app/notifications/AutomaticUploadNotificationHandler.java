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
package org.envirocar.app.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.core.app.NotificationCompat;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.handler.ApplicationSettings;
import org.envirocar.app.interactor.UploadTrack;
import org.envirocar.core.InternetAccessProvider;
import org.envirocar.core.entity.Track;
import org.envirocar.core.events.TrackFinishedEvent;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.disposables.Disposable;

/**
 * @author dewall
 */
@Singleton
public class AutomaticUploadNotificationHandler {
    private static final Logger LOG = Logger.getLogger(AutomaticUploadNotificationHandler.class);
    private static final String DELETE_ACTION = "com.envirocar.app.events.handler.automaticupload.delete";
    private static final IntentFilter DELETE_FILTER = new IntentFilter(DELETE_ACTION);

    private enum UploadState {
        UPLOADED,
        UPLOADING,
        ERROR
    }

    private final Context context;
    private final UploadTrack uploadTrack;
    private final InternetAccessProvider accessProvider;
    private final NotificationManager notificationManager;
    private final AutomaticUploadNotification uploadNotification;
    private final Bus bus;

    private Disposable uploadDisposable;

    /**
     * Constructor.
     *
     * @param context
     * @param uploadTrack
     * @param accessProvider
     * @param notificationManager
     */
    @Inject
    public AutomaticUploadNotificationHandler(@InjectApplicationScope Context context, UploadTrack uploadTrack, InternetAccessProvider accessProvider, NotificationManager notificationManager, Bus bus) {
        this.context = context;
        this.uploadTrack = uploadTrack;
        this.accessProvider = accessProvider;
        this.notificationManager = notificationManager;
        this.bus = bus;
        this.uploadNotification = new AutomaticUploadNotification();

        bus.register(this);
    }

    @Subscribe
    public void onTrackFinishedEvent(TrackFinishedEvent e) {
        if (!ApplicationSettings.getAutomaticUploadObservable(context).blockingFirst())
            return;

        LOG.info("Received event %s. Automatic upload is enabled. Trying to upload track");
        if (!accessProvider.isConnected()) {
            LOG.info("No Internet connection available");
            return;
        }

        if (e.mTrack == null || e.mTrack.getMeasurements().size() <= 2) {
            LOG.info("Track has no or too less measurements to upload -> ignoring");
            return;
        }

        // upload the track
        this.uploadNotification.setState(UploadState.UPLOADING, e.mTrack);
        this.uploadDisposable = uploadTrack.execute(new UploadTrack.Params(e.mTrack))
                .subscribe(this::onTrackUploaded, this::onTrackUploadError);
    }

    /**
     * Handler method for onNext()
     *
     * @param track the uploaded track
     */
    private void onTrackUploaded(Track track) {
        LOG.info("Track %s has been automatically uploaded.", track.getName());
        uploadNotification.setState(UploadState.UPLOADED, track);
    }

    /**
     * Handler method for onError()
     *
     * @param e the exception
     */
    private void onTrackUploadError(Throwable e) {
        LOG.error(e);
        uploadNotification.setState(UploadState.ERROR);
    }

    private final class AutomaticUploadNotification implements EnviroCarNotification {
        private final String CHANNEL_ID = "com.envirocar.app.events.handler.automaticupload.notification";
        private final String CHANNEL_NAME = "Automatic Upload Notification";
        private final String CHANNEL_DESCRIPTION = "Notification for the automatic upload of tracks.";
        private final int NOTIFICATION_ID = 284;

        private final String channel;
        private Notification notification;

        /**
         * @author dewall
         */
        AutomaticUploadNotification() {
            this.channel = createChannel(notificationManager, CHANNEL_ID, CHANNEL_NAME, CHANNEL_DESCRIPTION, NotificationManager.IMPORTANCE_LOW);
        }

        public void setState(UploadState state){
            setState(state, null);
        }

        public void setState(UploadState state, Track track) {
            String title = null;
            String text = null;

            switch(state){
                case UPLOADED:
                    title = context.getString(R.string.notification_autoupload_success_title);
                    text = String.format(context.getString(R.string.notification_autoupload_success_sub), track.getName());
                    break;
                case UPLOADING:
                    title = context.getString(R.string.notification_autoupload_uploading_title);
                    text = String.format(context.getString(R.string.notification_autoupload_uploading_sub), track.getName());
                    break;
                case ERROR:
                    title = context.getString(R.string.notification_autoupload_error_title);
                    text = String.format(context.getString(R.string.notification_autoupload_error_title));
                    break;
            }

            this.notification = new NotificationCompat.Builder(context, channel)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(R.drawable.ic_cloud_upload_black_24dp)
                    .build();

            notificationManager.notify(NOTIFICATION_ID, this.notification);
        }

        /**
         * Deletes the notification
         */
        @Override
        public void cancel() {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }
}
