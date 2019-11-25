package org.envirocar.app.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.core.app.NotificationCompat;

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

import io.reactivex.disposables.Disposable;

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
    public AutomaticUploadNotificationHandler(@InjectApplicationScope Context context, UploadTrack uploadTrack, InternetAccessProvider accessProvider, NotificationManager notificationManager) {
        this.context = context;
        this.uploadTrack = uploadTrack;
        this.accessProvider = accessProvider;
        this.notificationManager = notificationManager;
        this.uploadNotification = new AutomaticUploadNotification();
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
        LOG.info("Track %s has been automatically uploaded.", track.getDescription());
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

        public class OnDismissReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {

            }
        }

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

            Intent onCancelIntent = new Intent(context, OnDismissReceiver.class);

            String title = null;
            String text = null;

            switch(state){
                case UPLOADED:
                    title = "Track Uploaded";
                    text = String.format("%s has been successfully uploaded", track.getDescription());
                    break;
                case UPLOADING:
                    title = "Uploading Track";
                    text = String.format("Uploading %s", track.getDescription());
                    break;
                case ERROR:
                    title = "Uploading Error";
                    text = "Some error occured while uploading. Please try it manually.";
                    break;
            }

            this.notification = new NotificationCompat.Builder(context, channel)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(R.drawable.ic_cloud_upload_black_24dp)
//                    .setDeleteIntent()
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
