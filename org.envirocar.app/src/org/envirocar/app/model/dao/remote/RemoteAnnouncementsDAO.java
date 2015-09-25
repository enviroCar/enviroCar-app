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

import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Announcement;
import org.envirocar.app.model.dao.AnnouncementsDAO;
import org.envirocar.app.model.dao.cache.CacheAnnouncementsDAO;
import org.envirocar.app.model.dao.exception.AnnouncementsRetrievalException;
import org.envirocar.app.model.dao.exception.NotConnectedException;
import org.envirocar.app.model.dao.exception.UnauthorizedException;
import org.envirocar.app.model.dao.service.AnnouncementsService;
import org.envirocar.app.model.dao.service.EnviroCarService;
import org.envirocar.app.model.dao.service.utils.EnvirocarServiceUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import retrofit.Call;
import retrofit.Response;

/**
 * The data access object for remote fuelings that are stored at the envirocar service.
 *
 * @author dewall
 */
public class RemoteAnnouncementsDAO extends BaseRemoteDAO implements AnnouncementsDAO {
    private static final Logger LOG = Logger.getLogger(RemoteAnnouncementsDAO.class);

    // announcement cache.
    private CacheAnnouncementsDAO cache;

    /**
     * Constructor.
     *
     * @param cacheAnnouncementsDAO the announcement cache DAO.
     */
    public RemoteAnnouncementsDAO(CacheAnnouncementsDAO cacheAnnouncementsDAO) {
        this.cache = cacheAnnouncementsDAO;
    }

    @Override
    public List<Announcement> getAllAnnouncements() throws AnnouncementsRetrievalException {
        LOG.info("getAllAnnouncements()");

        // Instantiate the announcement service and the upload fueling call
        final AnnouncementsService announcementsService = EnviroCarService.getAnnouncementService();
        Call<List<Announcement>> allAnnouncementsCall = announcementsService.getAllAnnouncements();

        try {
            // Execute the call
            Response<List<Announcement>> allAnnouncementsResponse = allAnnouncementsCall.execute();

            // assert the response code when it was not successful
            if (!allAnnouncementsResponse.isSuccess()) {
                EnvirocarServiceUtils.assertStatusCode(allAnnouncementsResponse.code(),
                        allAnnouncementsResponse.message());
                return null;
            }

            // Store the announcements into the cache
            if (cache != null) {
                LOG.info("Store the announcments into the cache DAO");
                cache.storeAllAnnouncements(allAnnouncementsResponse.raw().body().string());
            }

            // return the list of announcements.
            return allAnnouncementsResponse.body();
        } catch (IOException e) {
            throw new AnnouncementsRetrievalException(e);
        } catch (Exception e) {
            throw new AnnouncementsRetrievalException(e);
        }
    }
}
