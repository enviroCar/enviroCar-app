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
package org.envirocar.app.model.dao.remote;

import com.squareup.okhttp.ResponseBody;

import org.envirocar.app.json.TrackWithoutMeasurementsException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.dao.TrackDAO;
import org.envirocar.app.model.dao.exception.NotConnectedException;
import org.envirocar.app.model.dao.exception.ResourceConflictException;
import org.envirocar.app.model.dao.exception.TrackRetrievalException;
import org.envirocar.app.model.dao.exception.TrackSerializationException;
import org.envirocar.app.model.dao.exception.UnauthorizedException;
import org.envirocar.app.model.dao.service.EnviroCarService;
import org.envirocar.app.model.dao.service.TrackService;
import org.envirocar.app.model.dao.service.utils.EnvirocarServiceUtils;
import org.envirocar.app.storage.Track;

import java.io.IOException;
import java.util.List;

import retrofit.Call;
import retrofit.Response;

public class RemoteTrackDAO extends BaseRemoteDAO implements TrackDAO, AuthenticatedDAO {
    private static final Logger LOG = Logger.getLogger(RemoteTrackDAO.class);

    @Override
    public Track getTrack(String id) throws NotConnectedException {
        LOG.info(String.format("getTrack(%s)", id));
        final TrackService trackService = EnviroCarService.getTrackService();
        Call<Track> trackCall = trackService.getTrack(mUserManager.getUser().getUsername(), id);

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
        } catch (Exception e) {
            throw new NotConnectedException(e);
        }
    }

    @Override
    public Integer getUserTrackCount() throws NotConnectedException, TrackRetrievalException {
        LOG.info("getUserTrackCount()");
        final TrackService trackService = EnviroCarService.getTrackService();
        Call<ResponseBody> allTracksCountCall = trackService.getAllTracksCountOfUser(
                mUserManager.getUser().getUsername());

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
        } catch (Exception e) {
            throw new TrackRetrievalException(e);
        }
    }

    @Override
    public Integer getTotalTrackCount() throws NotConnectedException, TrackRetrievalException {
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
            throw new TrackRetrievalException(e);
        }
    }

    @Override
    public List<String> getTrackIds() throws NotConnectedException, UnauthorizedException {
        return getTrackIds(100);
    }

    @Override
    public List<String> getTrackIds(int limit) throws NotConnectedException, UnauthorizedException {
        return getTrackIds(limit, 1);
    }

    @Override
    public List<String> getTrackIds(int limit, int page) throws NotConnectedException,
            UnauthorizedException {
        //        final TrackService trackService = EnviroCarService.getTrackService();
        //        trackService.getTrack(mUserManager.getUser().getUsername(), )
        // TODO implement this

        //        User user = mUserManager.getUser();
        //        HttpGet get = new HttpGet(String.format("%s/users/%s/tracks?limit=%d&page=%d",
        //                ConstantsEnvirocar.BASE_URL, user.getUsername(), limit, page));
        //
        //        InputStream response;
        //        try {
        //            response = super.retrieveHttpContent(get);
        //        } catch (IOException e1) {
        //            throw new NotConnectedException(e1);
        //        }
        //
        //        List<String> result;
        //        try {
        //            result = new TrackDecoder().getResourceIds(response);
        //        } catch (ParseException e) {
        //            throw new NotConnectedException(e);
        //        } catch (IOException e) {
        //            throw new NotConnectedException(e);
        //        } catch (JSONException e) {
        //            throw new NotConnectedException(e);
        //        }
        //
        //        return result;
        return null;
    }

    @Override
    public String storeTrack(Track track, boolean obfuscate) throws NotConnectedException,
            TrackSerializationException, TrackRetrievalException,
            TrackWithoutMeasurementsException, UnauthorizedException {
        LOG.info("storeTrack()");

        // check whether the user is logged in
        if (!mUserManager.isLoggedIn()) {
            throw new UnauthorizedException("The user is not logged in");
        }

        // Initiate the service and its call
        final TrackService trackService = EnviroCarService.getTrackService();
        Call<ResponseBody> uploadTrackCall =
                trackService.uploadTrack(mUserManager.getUser().getUsername(),
                        EnvirocarServiceUtils.getNonObfuscatedMeasurements(track, obfuscate));

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
            return location;
        } catch (IOException e) {
            throw new TrackSerializationException(e);
        } catch (ResourceConflictException e) {
            throw new NotConnectedException(e);
        }
    }

    @Override
    public void deleteTrack(String remoteID) throws UnauthorizedException, NotConnectedException {
        LOG.info(String.format("deleteTrack(%s)", remoteID));

        // If not logged in, then throw an exception
        if (!mUserManager.isLoggedIn()) {
            throw new UnauthorizedException("No User logged in.");
        }

        // Init the retrofit service endpoint and the delete call
        final TrackService trackService = EnviroCarService.getTrackService();
        Call<ResponseBody> deleteTrackCall = trackService.deleteTrack(mUserManager.getUser()
                .getUsername(), remoteID);

        try {
            // Execute the call
            Response<ResponseBody> deleteTrackResponse = deleteTrackCall.execute();

            // Check whether the call was successful or not
            if (!deleteTrackResponse.isSuccess()) {
                LOG.warn(String.format("deleteTrack(): Error while deleting remote track."));
                EnvirocarServiceUtils.assertStatusCode(deleteTrackResponse.code(),
                        deleteTrackResponse.message());
            }
        } catch (IOException e) {
            throw new NotConnectedException(e);
        } catch (ResourceConflictException e) {
            throw new NotConnectedException(e);
        }
    }
}
