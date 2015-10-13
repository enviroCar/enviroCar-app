package org.envirocar.app;

import android.app.Activity;
import android.content.Context;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.app.activity.DialogUtil;
import org.envirocar.app.exception.NotAcceptedTermsOfUseException;
import org.envirocar.app.exception.NotLoggedInException;
import org.envirocar.app.exception.ServerException;
import org.envirocar.app.exception.TrackAlreadyUploadedException;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.handler.TermsOfUseManager;
import org.envirocar.app.handler.UploadManager;
import org.envirocar.app.handler.UserHandler;
import org.envirocar.app.injection.DAOProvider;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.User;
import org.envirocar.core.events.TrackFinishedEvent;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.DataUpdateFailureException;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.injection.Injector;
import org.envirocar.core.logging.Logger;
import org.envirocar.obd.events.BluetoothServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.exceptions.OnErrorThrowable;
import rx.observables.BlockingObservable;
import rx.schedulers.Schedulers;

/**
 * @author de Wall
 */
public class TrackHandler {
    private static final Logger LOGGER = Logger.getLogger(TrackHandler.class);
    private static final String TRACK_MODE = "trackMode";

    /**
     * Callback interface for uploading a track.
     */
    public interface TrackUploadCallback {

        void onUploadStarted(Track track);

        /**
         * Called if the track has been successfully uploaded.
         *
         * @param track the track to upload.
         */
        void onSuccessfulUpload(Track track);

        /**
         * Called if an error occured during the upload routine.
         *
         * @param track   the track that was intended to be uploaded.
         * @param message the error message to be displayed within snackbar.
         */
        void onError(Track track, String message);
    }

    @Inject
    @InjectApplicationScope
    protected Context mContext;
    @Inject
    protected Bus mBus;
    @Inject
    protected DbAdapter mDBAdapter;
    @Inject
    protected BluetoothHandler mBluetoothHandler;
    @Inject
    protected DAOProvider mDAOProvider;
    @Inject
    protected UserHandler mUserManager;
    @Inject
    protected TermsOfUseManager mTermsOfUseManager;

    private Scheduler.Worker mBackgroundWorker = Schedulers.io().createWorker();

    private BluetoothServiceState mBluetoothServiceState = BluetoothServiceState.SERVICE_STOPPED;


    /**
     * Constructor.
     *
     * @param context the context of the activity's scope.
     */
    public TrackHandler(Context context) {
        // Inject all annotated fields.
        ((Injector) context).injectObjects(this);
    }

    /**
     * Deletes a track and returns true if the track has been successfully deleted.
     *
     * @param trackID the id of the track to delete.
     * @return true if the track has been successfully deleted.
     */
    public boolean deleteLocalTrack(Track.TrackId trackID) {
        Track dbRefTrack = mDBAdapter.getTrack(trackID, true);
        return deleteLocalTrack(dbRefTrack);
    }

    /**
     * Deletes a track and returns true if the track has been successfully deleted.
     *
     * @param trackRef the reference of the track.
     * @return true if the track has been successfully deleted.
     */
    public boolean deleteLocalTrack(Track trackRef) {
        LOGGER.info(String.format("deleteLocalTrack(id = %s)", trackRef.getTrackID().getId()));

        // Only delete the track if the track is a local track.
        if (trackRef.isLocalTrack()) {
            LOGGER.info("deleteLocalTrack(...): Track is a local track.");
            mDBAdapter.deleteTrack(trackRef.getTrackID());
            return true;
        }

        LOGGER.warn("deleteLocalTrack(...): track is no local track. No deletion.");
        return false;
    }

    /**
     * Invokes the deletion of a remote track. Once the remote track has been successfully
     * deleted, this method also deletes the locally stored reference of that track.
     *
     * @param trackRef
     * @return
     * @throws UnauthorizedException
     * @throws NotConnectedException
     */
    public boolean deleteRemoteTrack(Track trackRef) throws UnauthorizedException,
            NotConnectedException {
        LOGGER.info(String.format("deleteRemoteTrack(id = %s)", trackRef.getTrackID().getId()));

        // Check whether this track is a remote track.
        if (!trackRef.isRemoteTrack()) {
            LOGGER.warn("Track reference to upload is no remote track.");
            return false;
        }

        // Delete the track first remote and then the local reference.
        try {
            mDAOProvider.getTrackDAO().deleteTrack(trackRef.getRemoteID());
        } catch (DataUpdateFailureException e) {
            e.printStackTrace();
        }
        mDBAdapter.deleteTrack(trackRef.getTrackID());

        // Successfully deleted the remote track.
        LOGGER.info("deleteRemoteTrack(): Successfully deleted the remote track.");
        return true;
    }

    public boolean deleteAllRemoteTracksLocally() {
        LOGGER.info("deleteAllRemoteTracksLocally()");
        mDBAdapter.deleteAllRemoteTracks();
        return true;
    }

    public Track getTrackByID(long trackId) {
        return getTrackByID(new Track.TrackId(trackId));
    }

    /**
     *
     */
    public Track getTrackByID(Track.TrackId trackId) {
        LOGGER.info(String.format("getTrackByID(%s)", trackId.toString()));
        return mDBAdapter.getTrack(trackId);
    }

    public Observable<Track> uploadAllTracksObservable(){
        return Observable.create(new Observable.OnSubscribe<Track>() {
            @Override
            public void call(Subscriber<? super Track> subscriber) {
                try {
                    assertIsUserLoggedIn();
                    assertHasAcceptedTermsOfUse();
                } catch (NotLoggedInException e) {
                    subscriber.onError(e);
                    subscriber.onCompleted();
                    return;
                } catch (NotAcceptedTermsOfUseException e) {
                    subscriber.onError(e);
                    subscriber.onCompleted();
                    return;
                }


            }
        });

    }


    private boolean assertIsUserLoggedIn() throws NotLoggedInException {
        if(mUserManager.isLoggedIn()){
           return true;
        } else {
            throw new NotLoggedInException("Not Logged In");
        }
    }

    public Observable<Track> uploadAllTracks() {
        return Observable.create(new Observable.OnSubscribe<Track>() {
            @Override
            public void call(Subscriber<? super Track> subscriber) {
                if (!assertIsUserLoggedIn(subscriber)
                        || !assertHasAcceptedTermsOfUse(subscriber)) {
                    return;
                }

                List<Track> allLocalTracks = mDBAdapter.getAllLocalTracks();

                UploadManager uploadManager = new UploadManager(mContext);
                for (Track track : allLocalTracks) {
                    if (!assertIsLocalTrack(track, subscriber)) {
                        LOGGER.warn(String.format("Track with id=%s is no local track",
                                track.getTrackID()));
                        allLocalTracks.remove(track);
                    }
                }

                uploadManager.uploadTracks(allLocalTracks)
                        .subscribe(new Subscriber<Track>() {
                            @Override
                            public void onCompleted() {
                                subscriber.onCompleted();
                            }

                            @Override
                            public void onError(Throwable e) {
                                subscriber.onError(e);
                            }

                            @Override
                            public void onNext(Track track) {
                                subscriber.onNext(track);
                            }
                        });
            }
        });
    }

    private BlockingObservable<Boolean> asserHasAcceptedTermsOfUseObservable(){
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                // First, try to get whether the user has accepted the terms of use.
                final User user = mUserManager.getUser();
                boolean verified = false;
                try {
                    verified = mTermsOfUseManager.verifyTermsUseOfVersion(user.getTermsOfUseVersion());
                } catch (ServerException e) {
                    LOGGER.warn(e.getMessage(), e);
                    String infoText = mContext.getString(R.string.trackviews_server_error);
                    subscriber.onError(new NotAcceptedTermsOfUseException(infoText));
                }
            }
        }).toBlocking();
    }

    private boolean assertHasAcceptedTermsOfUse() throws NotAcceptedTermsOfUseException {
        // First, try to get whether the user has accepted the terms of use.
        final User user = mUserManager.getUser();
        boolean verified = false;
        try {
            verified = mTermsOfUseManager.verifyTermsUseOfVersion(user.getTermsOfUseVersion());
        } catch (ServerException e) {
            LOGGER.warn(e.getMessage(), e);
            String infoText = mContext.getString(R.string.trackviews_server_error);
            throw new NotAcceptedTermsOfUseException(infoText);
        }

        return verified;
    }

    private boolean assertHasAcceptedTermsOfUse(Subscriber<? super Track> subscriber) {
        // First, try to get whether the user has accepted the terms of use.
        final User user = mUserManager.getUser();
        boolean verified = false;
        try {
            verified = mTermsOfUseManager.verifyTermsUseOfVersion(user.getTermsOfUseVersion());
        } catch (ServerException e) {
            LOGGER.warn(e.getMessage(), e);
            String infoText = mContext.getString(R.string.trackviews_server_error);
            subscriber.onError(e);
            return false;
        }

        return verified;
    }

    private boolean assertIsLocalTrack(Track track, Subscriber<? super Track> subscriber){
        // If the track is no local track, then popup a snackbar.
        if (!track.isLocalTrack()) {
            String infoText = String.format(mContext.getString(R.string
                    .trackviews_is_already_uploaded), track.getName());
            LOGGER.info(infoText);
            subscriber.onError(new TrackAlreadyUploadedException(infoText));
            return false;
        }
        return true;
    }

    private boolean assertIsUserLoggedIn(Subscriber<? super Track> subscriber){
        // If the user is not logged in, then skip the upload and popup a snackbar.
        if (!mUserManager.isLoggedIn()) {
            LOGGER.warn("Cannot upload track, because the user is not logged in");
            String infoText = mContext.getString(R.string.trackviews_not_logged_in);
            subscriber.onError(new NotLoggedInException(infoText));
            return false;
        }
        return true;
    }

    private boolean assertHasAcceptedTermsOfUse(TrackUploadCallback callback) {
        // First, try to get whether the user has accepted the terms of use.
        final User user = mUserManager.getUser();
        boolean verified = false;
        try {
            verified = mTermsOfUseManager.verifyTermsUseOfVersion(user.getTermsOfUseVersion());
        } catch (ServerException e) {
            LOGGER.warn(e.getMessage(), e);
            String infoText = mContext.getString(R.string.trackviews_server_error);
            callback.onError(null, infoText);
            return false;
        }

        return verified;
    }

    private boolean assertIsLocalTrack(Track track, TrackUploadCallback callback) {
        // If the track is no local track, then popup a snackbar.
        if (!track.isLocalTrack()) {
            String infoText = String.format(mContext.getString(R.string
                    .trackviews_is_already_uploaded), track.getName());
            LOGGER.info(infoText);
            callback.onError(track, infoText);
            return false;
        }
        return true;
    }

    private boolean assertIsUserLoggedIn(Track track, TrackUploadCallback callback) {
        // If the user is not logged in, then skip the upload and popup a snackbar.
        if (!mUserManager.isLoggedIn()) {
            LOGGER.warn("Cannot upload track, because the user is not logged in");
            String infoText = mContext.getString(R.string.trackviews_not_logged_in);
            callback.onError(track, infoText);
            return false;
        }
        return true;
    }

    // TODO REMOVE THIS ACTIVITY STUFF... unbelievable.. no structure!
    public void uploadTrack(Activity activity, Track track, TrackUploadCallback callback) {
        // If the track is no local track, then popup a snackbar.
        if (!track.isLocalTrack()) {
            String infoText = String.format(mContext.getString(R.string
                    .trackviews_is_already_uploaded), track.getName());
            LOGGER.info(infoText);
            callback.onError(track, infoText);
            return;
        }

        // If the user is not logged in, then skip the upload and popup a snackbar.
        if (!mUserManager.isLoggedIn()) {
            LOGGER.warn("Cannot upload track, because the user is not logged in");
            String infoText = mContext.getString(R.string.trackviews_not_logged_in);
            callback.onError(track, infoText);
            return;
        }

        // First, try to get whether the user has accepted the terms of use.
        final User user = mUserManager.getUser();
        boolean verified = false;
        try {
            verified = mTermsOfUseManager.verifyTermsUseOfVersion(user.getTermsOfUseVersion());
        } catch (ServerException e) {
            LOGGER.warn(e.getMessage(), e);
            String infoText = mContext.getString(R.string.trackviews_server_error);
            callback.onError(track, infoText);
            return;
        }

        // If the user has not accepted the terms of use, then show a dialog where he
        // can accept the terms of use.
        if (!verified) {
            final TermsOfUse current;
            try {
                current = mTermsOfUseManager.getCurrentTermsOfUse();
            } catch (ServerException e) {
                LOGGER.warn("This should never happen!", e);
                callback.onError(track, "Terms Of Use not accepted.");
                return;
            }

            // Create a dialog with which the user can accept the terms of use.
            DialogUtil.createTermsOfUseDialog(current,
                    user.getTermsOfUseVersion() == null, new DialogUtil
                            .PositiveNegativeCallback() {

                        @Override
                        public void negative() {
                            LOGGER.info("User did not accept the ToU.");
                            callback.onError(track, mContext.getString(R.string
                                    .terms_of_use_info));
                        }

                        @Override
                        public void positive() {
                            // If the user accepted the terms of use, then update this and
                            // finally upload the track.
                            mTermsOfUseManager.userAcceptedTermsOfUse(user, current
                                    .getIssuedDate());
                            new UploadManager(activity).uploadSingleTrack(track, callback);
                        }

                    }, activity);

            return;
        } else {
            // Upload the track if everything is right.
            new UploadManager(activity).uploadSingleTrack(track, callback);
        }
    }

    public Observable<Track> fetchRemoteTrackObservable(Track remoteTrack) {
        return Observable.create(new Observable.OnSubscribe<Track>() {
            @Override
            public void call(Subscriber<? super Track> subscriber) {
                try {
                    subscriber.onNext(fetchRemoteTrack(remoteTrack));
                    subscriber.onCompleted();
                } catch (NotConnectedException e) {
                    throw OnErrorThrowable.from(e);
                } catch (DataRetrievalFailureException e) {
                    throw OnErrorThrowable.from(e);
                } catch (UnauthorizedException e) {
                    throw OnErrorThrowable.from(e);
                }
            }
        });
    }

    public Track fetchRemoteTrack(Track remoteTrack) throws NotConnectedException,
            UnauthorizedException, DataRetrievalFailureException {
        try {
            Track downloadedTrack = mDAOProvider.getTrackDAO().getTrackById(remoteTrack
                    .getRemoteID());

            // Deep copy... TODO improve this.
            remoteTrack.setName(downloadedTrack.getName());
            remoteTrack.setDescription(downloadedTrack.getDescription());
            remoteTrack.setMeasurements(new ArrayList<>(downloadedTrack.getMeasurements()));
            remoteTrack.setCar(downloadedTrack.getCar());
            remoteTrack.setTrackStatus(downloadedTrack.getTrackStatus());
            remoteTrack.setMetadata(downloadedTrack.getMetadata());

            remoteTrack.setStartTime(downloadedTrack.getStartTime());
            remoteTrack.setEndTime(downloadedTrack.getEndTime());
        } catch (NoMeasurementsException e) {
            e.printStackTrace();
        }


        mDBAdapter.insertTrack(remoteTrack, true);
        return remoteTrack;
    }

    /**
     * Finishes the current track. On the one hand, the service that handles the connection to
     * the Bluetooth device gets closed and the track in the database gets finished.
     */
    public void finishCurrentTrack() {
        LOGGER.info("stopTrack()");

        // Set the current service state to SERVICE_STOPPING.
        mBus.post(new BluetoothServiceStateChangedEvent(BluetoothServiceState.SERVICE_STOPPING));

        // Schedule a new async task for closing the service, finishing the current track, and
        // finally fireing an event on the event bus.
        mBackgroundWorker.schedule(() -> {
            LOGGER.info("backgroundworker");
            // Stop the background service that is responsible for the OBDConnection.
            mBluetoothHandler.stopOBDConnectionService();

            // Finish the current track.
            final Track track = mDBAdapter.finishCurrentTrack();

            // Fire a new TrackFinishedEvent on the event bus.
            mBus.post(new TrackFinishedEvent(track));
        });
    }

    @Subscribe
    public void onReceiveBluetoothServiceStateChangedEvent(
            BluetoothServiceStateChangedEvent event) {
        LOGGER.info(String.format("onReceiveBluetoothServiceStateChangedEvent: %s",
                event.toString()));
        mBluetoothServiceState = event.mState;
    }

}
