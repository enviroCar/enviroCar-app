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
package org.envirocar.remote.dao;

import com.google.common.base.Preconditions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.envirocar.core.UserManager;
import org.envirocar.core.dao.TrackDAO;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.*;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.service.EnviroCarService;
import org.envirocar.remote.service.TrackService;
import org.envirocar.remote.util.EnvirocarServiceUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Singleton
public class RemoteTrackDAO extends BaseRemoteDAO<TrackDAO, TrackService> implements TrackDAO {
    private static final Logger LOG = Logger.getLogger(RemoteTrackDAO.class);

    /**
     * Constructor.
     *
     * @param cacheDao
     * @param service
     * @param userManager
     */
    @Inject
    public RemoteTrackDAO(CacheTrackDAO cacheDao, TrackService service, UserManager userManager) {
        super(cacheDao, service, userManager);
    }

    @Override
    public Track getTrackById(String id) throws DataRetrievalFailureException,
            NotConnectedException, UnauthorizedException {
        LOG.info(String.format("getTrack(%s)", id));
        final TrackService trackService = EnviroCarService.getTrackService();
        Call<Track> trackCall = trackService.getTrack(userManager.getUser().getUsername(), id);

        try {
            // Execute the request call
            Response<Track> trackResponse = executeCall(trackCall);

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
        return Observable.create(emitter -> {
            if (emitter.isDisposed())
                return;
            try {
                Track remoteTracks = getTrackById(id);
                emitter.onNext(remoteTracks);
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
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
            Response<ResponseBody> response = allTracksCountCall.execute();

            // If the request call was not successful, then assert the status code and throw an
            // exceptiom
            if (!response.isSuccessful()) {
                EnvirocarServiceUtils.assertStatusCode(response.code(),
                        response.errorBody().toString(), response.body().string());
                return null;
            }

            // Get the page count with a track limit of 1 per page (?limit=1). This corresponds
            // to the number of global tracks and return it.
            int pageCount = EnvirocarServiceUtils.resolvePageCount(response);
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
            Response<ResponseBody> response = allTracksCountCall.execute();

            // If the request call was not successful, then assert the status code and throw an
            // exceptiom
            if (!response.isSuccessful()) {
                EnvirocarServiceUtils.assertStatusCode(response);
                return null;
            }

            // Get the page count with a track limit of 1 per page (?limit=1). This corresponds
            // to the number of global tracks and return it.
            int pageCount = EnvirocarServiceUtils.resolvePageCount(response);
            LOG.info(String.format("getTotalTrackCount() with a tracksize of %s", pageCount));
            return pageCount;
        } catch (IOException e) {
            throw new NotConnectedException(e);
        } catch (Exception e) {
            throw new DataRetrievalFailureException(e);
        }
    }

    @Override
    public Track createTrack(Track track) throws DataCreationFailureException,
            NotConnectedException, UnauthorizedException {
        LOG.info("createTrack()");

        // check whether the getUserStatistic is logged in
        if (!userManager.isLoggedIn()) {
            throw new UnauthorizedException("The getUserStatistic is not logged in");
        }

        // Initiate the remoteService and its call
        final TrackService trackService = EnviroCarService.getTrackService();
        Call<ResponseBody> uploadTrackCall =
                trackService.uploadTrack(userManager.getUser().getUsername(), track);

        try {
            Response<ResponseBody> response = uploadTrackCall.execute();

            if (!response.isSuccessful()) {
                LOG.severe("Error while uploading track: " + response.message());
                LOG.severe(response.errorBody().string());
                EnvirocarServiceUtils.assertStatusCode(response.code(),
                        response.message(), response.errorBody().string());
            }

            // Resolve the location where the track is stored.
            String location = EnvirocarServiceUtils.resolveRemoteLocation(response);
            LOG.info("Uploaded remote location: " + location);

            // Set the remoteID ...
            track.setRemoteID(location.substring(location.lastIndexOf('/') + 1, location.length()));
            // ... and return the track;
            return track;
        } catch (IOException e) {
            throw new DataCreationFailureException(e);
        } catch (ResourceConflictException e) {
            throw new NotConnectedException(e);
        }
    }

    @Override
    public Observable<Track> createTrackObservable(Track track) {
        return Observable.create(emitter -> {
            if (emitter.isDisposed())
                return;
            LOG.info("call: creating remote track.");
            try {
                emitter.onNext(createTrack(track));
            } catch (DataCreationFailureException |
                    NotConnectedException |
                    UnauthorizedException e) {
                LOG.error(e.getMessage(), e);
                emitter.onError(e);
            }
            emitter.onComplete();
        });
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
        Call<List<Track>> remoteTrackCall = remoteService.getTrackIdsWithLimit(userManager.getUser()
                .getUsername(), limit);

        try {
            // Execute the call
            Response<List<Track>> remoteTracksResponse = remoteTrackCall.execute();

            if (!remoteTracksResponse.isSuccessful()) {
                LOG.severe("Error while retrieving the list of remote tracks");
                EnvirocarServiceUtils.assertStatusCode(remoteTracksResponse);
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
        return Observable.create(emitter -> {
            if (emitter.isDisposed())
                return;
            try {
                List<Track> remoteTrackIds = getTrackIds();
                emitter.onNext(remoteTrackIds);
                emitter.onComplete();
            } catch (Exception e) {
                if (!emitter.isDisposed())
                    emitter.onError(e);
            }
        });
    }

    @Override
    public Observable<List<Track>> getTrackIdsObservable(final int limit, final int page) {
        return Observable.create(emitter -> {
            if (emitter.isDisposed())
                return;
            try {
                List<Track> remoteTracks = getTrackIds(limit, page);
                emitter.onNext(remoteTracks);
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    @Override
    public List<Track> getTrackIdsWithLimit(int limit) throws NotConnectedException,
            UnauthorizedException {
        final TrackService trackService = EnviroCarService.getTrackService();
        Call<List<Track>> remoteTrackCall = trackService.getTrackIdsWithLimit(userManager.getUser()
                .getUsername(), limit);

        try {
            Response<List<Track>> response = remoteTrackCall.execute();

            if (!response.isSuccessful()) {
                LOG.severe("Error while retrieving the list of remote tracks with limit " + limit);
                EnvirocarServiceUtils.assertStatusCode(response);
            }

            return response.body();
        } catch (IOException e) {
            throw new NotConnectedException(e);
        } catch (ResourceConflictException e) {
            throw new NotConnectedException(e);
        }
    }

    @Override
    public Observable<List<Track>> getTrackIdsWithLimitObservable(final int limit) {
        return Observable.create(emitter -> {
            if (emitter.isDisposed())
                return;
            try {
                List<Track> remoteTracks = getTrackIdsWithLimit(limit);
                emitter.onNext(remoteTracks);
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    @Override
    public void deleteTrack(Track track) throws
            NotConnectedException, UnauthorizedException {
        Preconditions.checkState(track.getRemoteID() != null, "No RemoteID for this Track.");
        Preconditions.checkState(track.isRemoteTrack(), "Track is not a remote track. Track " +
                "cannot be deleted");
        String remoteID = track.getRemoteID();
        LOG.info(String.format("deleteRemoteTrack(%s)", remoteID));

        // If not logged in, then throw an exception
        if (!userManager.isLoggedIn()) {
            throw new UnauthorizedException("No User logged in.");
        }

        // Init the retrofit remoteService endpoint and the delete call
        final TrackService trackService = EnviroCarService.getTrackService();
        Call<ResponseBody> deleteTrackCall = trackService.deleteTrack(userManager.getUser()
                .getUsername(), remoteID);

        try {
            // Execute the call
            Response<ResponseBody> response = deleteTrackCall.execute();

            // Check whether the call was successful or not
            if (!response.isSuccessful()) {
                LOG.warn(String.format("deleteLocalTrack(): Error while deleting remote track."));
                EnvirocarServiceUtils.assertStatusCode(response);
            }
        } catch (IOException e) {
            throw new NotConnectedException(e);
        } catch (ResourceConflictException e) {
            throw new NotConnectedException(e);
        } catch (Exception e) {
            LOG.warn("WARNING!!!");
            throw e;
        }
    }

    @Override
    public void updateTrack(String remoteID, JsonArray trackFeatures) throws DataUpdateFailureException, NotConnectedException, UnauthorizedException {
        Preconditions.checkState(remoteID != null, "No RemoteID for this Track.");
        LOG.info(String.format("updateTrack(%s)", remoteID));

        // If not logged in, then throw an exception
        if (!userManager.isLoggedIn()) {
            throw new UnauthorizedException("No User logged in.");
        }

        JsonObject featureCollection = new JsonObject();
        featureCollection.addProperty("type", "FeatureCollection");
        featureCollection.add("properties", new JsonObject());
        featureCollection.add("features", trackFeatures);

        // Init the retrofit remoteService endpoint and the update call
        final TrackService trackService = EnviroCarService.getTrackService();
        Call<ResponseBody> updateTrackCall = trackService.updateTrack(userManager.getUser()
                .getUsername(), remoteID, featureCollection);

        try {
            // Execute the call
            Response<ResponseBody> response = updateTrackCall.execute();

            // Check whether the call was successful or not
            if (!response.isSuccessful()) {
                LOG.warn(String.format("updateTrack(): Error while updating remote track."));
                EnvirocarServiceUtils.assertStatusCode(response);
            }
        } catch (IOException e) {
            throw new NotConnectedException(e);
        } catch (ResourceConflictException e) {
            throw new NotConnectedException(e);
        } catch (Exception e) {
            LOG.warn("WARNING!!!");
            throw e;
        }

    }

    @Override
    public void finishTrack(Track track) throws UnauthorizedException, NotConnectedException {
        LOG.info("finishTrack()");

        // check whether the getUserStatistic is logged in
        if (!userManager.isLoggedIn()) {
            throw new UnauthorizedException("The getUserStatistic is not logged in");
        }

        track.setTrackStatus(Track.TrackStatus.FINISHED);

        //remove measurements
        track.setMeasurements(new ArrayList<>());

        // Initiate the remoteService and its call
        final TrackService trackService = EnviroCarService.getTrackService();
        Call<ResponseBody> finishTrackCall =
                trackService.finishTrack(userManager.getUser().getUsername(), track.getRemoteID(), track);

        try {
            Response<ResponseBody> response = finishTrackCall.execute();

            if (!response.isSuccessful()) {
                LOG.severe("Error while finishing track: " + response.message());
                LOG.severe(response.errorBody().string());
                EnvirocarServiceUtils.assertStatusCode(response.code(),
                        response.message(), response.errorBody().string());
            }
        }catch (IOException e) {
            throw new NotConnectedException(e);
        } catch (ResourceConflictException e) {
            throw new NotConnectedException(e);
        } catch (Exception e) {
            LOG.warn("WARNING!!!");
            throw e;
        }
    }

}
