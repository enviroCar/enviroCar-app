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


import org.envirocar.core.dao.AnnouncementDAO;
import org.envirocar.core.entity.Announcement;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.service.AnnouncementsService;
import org.envirocar.remote.service.EnviroCarService;
import org.envirocar.remote.util.EnvirocarServiceUtils;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.Call;
import retrofit2.Response;

/**
 * The data access object for remote fuelings that are stored at the envirocar remoteService.
 *
 * @author dewall
 */
public class RemoteAnnouncementsDAO extends BaseRemoteDAO<AnnouncementDAO, AnnouncementsService>
        implements AnnouncementDAO {
    private static final Logger LOG = Logger.getLogger(RemoteAnnouncementsDAO.class);

//    /**
//     * Constructor.
//     *
//     * @param cacheDao cache dao for accessing/storing local instances of announcement entities.
//     */
//    public RemoteAnnouncementsDAO(AnnouncementDAO cacheDao) {
//        super(cacheDao);
//    }

    @Inject
    public RemoteAnnouncementsDAO(CacheAnnouncementsDAO cacheDAO, AnnouncementsService service) {
        super(cacheDAO, service);
    }

    @Override
    public List<Announcement> getAllAnnouncements() throws DataRetrievalFailureException,
            NotConnectedException {
        LOG.info("getAllAnnouncements()");

        // Instantiate the announcement remoteService and the upload fueling call
        final AnnouncementsService announcementsService = EnviroCarService.getAnnouncementService();
        Call<List<Announcement>> allAnnouncementsCall = remoteService.getAllAnnouncements();

        try {
            // Execute the call
            Response<List<Announcement>> response = allAnnouncementsCall.execute();

            // assert the response code when it was not successful
            if (!response.isSuccessful()) {
                EnvirocarServiceUtils.assertStatusCode(response);
                return null;
            }

            // Store the announcements into the cache
            if (cacheDao != null) {
                LOG.info("Store the announcments into the cache DAO");
                cacheDao.saveAnnouncements(response.body());
            }

            // return the list of announcements.
            return response.body();
        } catch (IOException e) {
            throw new NotConnectedException(e);
        } catch (Exception e) {
            throw new DataRetrievalFailureException(e);
        }
    }

    @Override
    public Observable<List<Announcement>> getAllAnnouncementsObservable() {
        return Observable.create(emitter -> {
            try {
                List<Announcement> result = getAllAnnouncements();
                emitter.onNext(result);
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    @Override
    public void saveAnnouncements(List<Announcement> announcements) throws NotConnectedException {
        throw new NotConnectedException("No announcement upload allowed/supported!");
    }
}
