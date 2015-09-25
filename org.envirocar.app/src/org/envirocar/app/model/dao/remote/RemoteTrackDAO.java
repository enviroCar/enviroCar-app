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

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.envirocar.app.ConstantsEnvirocar;
import org.envirocar.app.json.StreamTrackEncoder;
import org.envirocar.app.json.TrackDecoder;
import org.envirocar.app.json.TrackWithoutMeasurementsException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.User;
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
import org.envirocar.app.util.FileWithMetadata;
import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import retrofit.Call;
import retrofit.Response;

public class RemoteTrackDAO extends BaseRemoteDAO implements TrackDAO, AuthenticatedDAO {
    private static final Logger LOG = Logger.getLogger(RemoteTrackDAO.class);

    @Override
    public void deleteTrack(String remoteID) throws UnauthorizedException, NotConnectedException {
        User user = mUserManager.getUser();

        if (user == null) {
            throw new UnauthorizedException("No User logged in.");
        }

        HttpDelete request = new HttpDelete(ConstantsEnvirocar.BASE_URL + "/users/" +
                user.getUsername() + "/tracks/" + remoteID);

        super.executeContentRequest(request);
    }

    @Override
    public String storeTrack(Track track, boolean obfuscate) throws NotConnectedException,
            TrackSerializationException, TrackRetrievalException,
            TrackWithoutMeasurementsException {
        try {
            File f = mTemporaryFileManager.createTemporaryFile();
            FileWithMetadata content = new StreamTrackEncoder().createTrackJsonAsFile(track,
                    obfuscate, f, true);

            User user = mUserManager.getUser();
            HttpPost post = new HttpPost(String.format("%s/users/%s/tracks", ConstantsEnvirocar
                            .BASE_URL,
                    user.getUsername()));

            HttpResponse response = executePayloadRequest(post, content);

            return new TrackDecoder().resolveLocation(response);
        } catch (JSONException e) {
            throw new TrackSerializationException(e);
        } catch (ResourceConflictException e) {
            throw new NotConnectedException(e);
        } catch (UnauthorizedException e) {
            throw new TrackRetrievalException(e);
        } catch (FileNotFoundException e) {
            throw new TrackSerializationException(e);
        } catch (IOException e) {
            throw new TrackSerializationException(e);
        }
    }


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


        User user = mUserManager.getUser();
        HttpGet get = new HttpGet(String.format("%s/users/%s/tracks?limit=%d&page=%d",
                ConstantsEnvirocar.BASE_URL, user.getUsername(), limit, page));

        InputStream response;
        try {
            response = super.retrieveHttpContent(get);
        } catch (IOException e1) {
            throw new NotConnectedException(e1);
        }

        List<String> result;
        try {
            result = new TrackDecoder().getResourceIds(response);
        } catch (ParseException e) {
            throw new NotConnectedException(e);
        } catch (IOException e) {
            throw new NotConnectedException(e);
        } catch (JSONException e) {
            throw new NotConnectedException(e);
        }

        return result;
    }


}
