/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.remote.dao;

import com.google.common.base.Preconditions;
import com.squareup.okhttp.ResponseBody;

import org.envirocar.core.UserManager;
import org.envirocar.core.dao.BaseRemoteDAO;
import org.envirocar.core.dao.TrackDAO;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.DataCreationFailureException;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.DataUpdateFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.ResourceConflictException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.service.EnviroCarService;
import org.envirocar.remote.service.TrackService;
import org.envirocar.remote.util.EnvirocarServiceUtils;

import java.io.IOException;
import java.util.List;

import retrofit.Call;
import retrofit.Response;
import rx.Observable;
import rx.Subscriber;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class RemoteTrackDAO extends BaseRemoteDAO<TrackDAO> implements TrackDAO {
    private static final Logger LOG = Logger.getLogger(RemoteTrackDAO.class);


    /**
     * Constructor.
     */
    public RemoteTrackDAO() {
        super(null, null);
    }

    /**
     * Constructor.
     *
     * @param cacheDao
     * @param userManager
     */
    public RemoteTrackDAO(TrackDAO cacheDao, UserManager userManager) {
        super(cacheDao, userManager);
    }

    @Override
    public Track getTrackById(String id) throws DataRetrievalFailureException,
            NotConnectedException, UnauthorizedException {
        LOG.info(String.format("getTrack(%s)", id));
        final TrackService trackService = EnviroCarService.getTrackService();
        Call<Track> trackCall = trackService.getTrack(userManager.getUser().getUsername(), id);

        try {
            // Execute the request call
            Response<Track> trackResponse = trackCall.execute();

            // If the request call was not successful, then assert the status code and throw an
            // exceptiom
            if (!trackResponse.isSuccess()) {
                LOG.warn(String.format("getTrack was not successful for the following reason: %s",
                        trackResponse.message()));
                EnvirocarServiceUtils.assertStatusCode(
                        trackResponse.code(), trackResponse.message());
            }

            // If it was successful, then return the track.
            LOG.debug("getTrack() was successful");
            return trackResponse.body();
        } catch (ResourceConflictException e) {
            throw new DataRetrievalFailureException(e);
        } catch (IOException e) {
            throw new NotConnectedException(e);
        }
    }

    @Override
    public Observable<Track> getTrackByIdObservable(final String id) {
        return Observable.create(new Observable.OnSubscribe<Track>() {
            @Override
            public void call(Subscriber<? super Track> subscriber) {
                try {
                    Track remoteTracks = getTrackById(id);
                    subscriber.onNext(remoteTracks);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    @Override
    public Integer getUserTrackCount() throws DataRetrievalFailureException,
            NotConnectedException, UnauthorizedException {
        LOG.info("getUserTrackCount()");
        final TrackService trackService = EnviroCarService.getTrackService();
        Call<ResponseBody> allTracksCountCall = trackService.getAllTracksCountOfUser(
                userManager.getUser().getUsername());

        try {
            // Execute the request call.
            Response<ResponseBody> allTracksCountResponse = allTracksCountCall.execute();

            // If the request call was not successful, then assert the status code and throw an
            // exceptiom
            if (!allTracksCountResponse.isSuccess()) {
                EnvirocarServiceUtils.assertStatusCode(allTracksCountResponse.code(),
                        allTracksCountResponse.errorBody().toString());
                return null;
            }

            // Get the page count with a track limit of 1 per page (?limit=1). This corresponds
            // to the number of global tracks and return it.
            int pageCount = EnvirocarServiceUtils.resolvePageCount(allTracksCountResponse);
            LOG.info(String.format("getTotalTrackCount() with a tracksize of %s", "" + pageCount));
            return pageCount;
        } catch (IOException e) {
            throw new NotConnectedException(e);
        } catch (ResourceConflictException e) {
            throw new DataRetrievalFailureException(e);
        }
    }

    @Override
    public Integer getTotalTrackCount() throws NotConnectedException,
            DataRetrievalFailureException {
        LOG.info("getTotalTrackCount()");
        final TrackService trackService = EnviroCarService.getTrackService();
        Call<ResponseBody> allTracksCountCall = trackService.getAllTracksCount();

        try {
            // Execute the request call.
            Response<ResponseBody> allTracksCountResponse = allTracksCountCall.execute();

            // If the request call was not successful, then assert the status code and throw an
            // exceptiom
            if (!allTracksCountResponse.isSuccess()) {
                EnvirocarServiceUtils.assertStatusCode(allTracksCountResponse.code(),
                        allTracksCountResponse.errorBody().toString());
                return null;
            }

            // Get the page count with a track limit of 1 per page (?limit=1). This corresponds
            // to the number of global tracks and return it.
            int pageCount = EnvirocarServiceUtils.resolvePageCount(allTracksCountResponse);
            LOG.info(String.format("getTotalTrackCount() with a tracksize of %s", pageCount));
            return pageCount;
        } catch (IOException e) {
            throw new NotConnectedException(e);
        } catch (Exception e) {
            throw new DataRetrievalFailureException(e);
        }
    }

    @Override
    public String createTrack(Track track) throws DataCreationFailureException,
            NotConnectedException, ResourceConflictException, UnauthorizedException {
        LOG.info("createTrack()");

        // check whether the user is logged in
        if (!userManager.isLoggedIn()) {
            throw new UnauthorizedException("The user is not logged in");
        }

        // Initiate the service and its call
        final TrackService trackService = EnviroCarService.getTrackService();
        Call<ResponseBody> uploadTrackCall =
                trackService.uploadTrack(userManager.getUser().getUsername(), track);

        try {
            Response<ResponseBody> uploadTrackResponse = uploadTrackCall.execute();

            if (!uploadTrackResponse.isSuccess()) {
                LOG.severe("Error while uploading track: " + uploadTrackResponse.message());
                EnvirocarServiceUtils.assertStatusCode(uploadTrackResponse.code(),
                        uploadTrackResponse.message());
            }

            // Resolve the location where the track is stored.
            String location = EnvirocarServiceUtils.resolveRemoteLocation(uploadTrackResponse);
            LOG.info("Uploaded remote location: " + location);

            // Return the location;
            return location.substring(location.lastIndexOf('/') + 1, location.length());
        } catch (IOException e) {
            throw new DataCreationFailureException(e);
        } catch (ResourceConflictException e) {
            throw new NotConnectedException(e);
        }
    }

    @Override
    public List<Track> getTrackIds() throws NotConnectedException, UnauthorizedException {
        return getTrackIds(100);
    }

    public List<Track> getTrackIds(int limit) throws NotConnectedException,
            UnauthorizedException {
        return getTrackIds(limit, 1);
    }

    @Override
    public List<Track> getTrackIds(int limit, int page) throws NotConnectedException,
            UnauthorizedException {
        final TrackService trackService = EnviroCarService.getTrackService();
        Call<List<Track>> remoteTrackCall = trackService.getTrackIds(userManager.getUser()
                .getUsername());

        try {
            // Execute the call
            Response<List<Track>> remoteTracksResponse = remoteTrackCall.execute();

            if (!remoteTracksResponse.isSuccess()) {
                LOG.severe("Error while retrieving the list of remote tracks");
                EnvirocarServiceUtils.assertStatusCode(remoteTracksResponse.code(),
                        remoteTracksResponse.message());
            }

            // Return the list of remotetracks.
            return remoteTracksResponse.body();
        } catch (IOException e) {
            throw new NotConnectedException(e);
        } catch (ResourceConflictException e) {
            throw new NotConnectedException(e);
        }
    }

    @Override
    public Observable<List<Track>> getTrackIdsObservable() {
        return Observable.create(new Observable.OnSubscribe<List<Track>>() {
            @Override
            public void call(Subscriber<? super List<Track>> subscriber) {
                try {
                    List<Track> remoteTrackIds = getTrackIds();
                    subscriber.onNext(remoteTrackIds);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    @Override
    public Observable<List<Track>> getTrackIdsObservable(final int limit, final int page) {
        return Observable.create(
                new Observable.OnSubscribe<List<Track>>() {
                    @Override
                    public void call(Subscriber<? super List<Track>> subscriber) {
                        try {
                            List<Track> remoteTracks = getTrackIds(limit, page);
                            subscriber.onNext(remoteTracks);
                            subscriber.onCompleted();
                        } catch (Exception e) {
                            subscriber.onError(e);
                        }
                    }
                }
        );
    }

    @Override
    public void deleteTrack(String remoteID) throws DataUpdateFailureException,
            NotConnectedException, UnauthorizedException {
        LOG.info(String.format("deleteRemoteTrack(%s)", remoteID));

        // If not logged in, then throw an exception
        if (!userManager.isLoggedIn()) {
            throw new UnauthorizedException("No User logged in.");
        }

        // Init the retrofit service endpoint and the delete call
        final TrackService trackService = EnviroCarService.getTrackService();
        Call<ResponseBody> deleteTrackCall = trackService.deleteTrack(userManager.getUser()
                .getUsername(), remoteID);

        try {
            // Execute the call
            Response<ResponseBody> deleteTrackResponse = deleteTrackCall.execute();

            // Check whether the call was successful or not
            if (!deleteTrackResponse.isSuccess()) {
                LOG.warn(String.format("deleteLocalTrack(): Error while deleting remote track."));
                EnvirocarServiceUtils.assertStatusCode(deleteTrackResponse.code(),
                        deleteTrackResponse.message());
            }
        } catch (IOException e) {
            throw new NotConnectedException(e);
        } catch (ResourceConflictException e) {
            throw new NotConnectedException(e);
        } catch (Exception e){
            LOG.warn("WARNING!!!");
            throw e;
        }
    }

    @Override
    public void deleteTrack(Track track) throws DataUpdateFailureException,
            NotConnectedException, UnauthorizedException {
        Preconditions.checkState(track.getRemoteID() != null, "No RemoteID for this Track.");
        deleteTrack(track.getRemoteID());
    }

}
