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


import org.envirocar.core.dao.AnnouncementDAO;
import org.envirocar.core.dao.BaseRemoteDAO;
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

import retrofit.Call;
import retrofit.Response;
import rx.Observable;
import rx.Subscriber;

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
    public RemoteAnnouncementsDAO(CacheAnnouncementsDAO cacheDAO, AnnouncementsService service){
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
            Response<List<Announcement>> allAnnouncementsResponse = allAnnouncementsCall.execute();

            // assert the response code when it was not successful
            if (!allAnnouncementsResponse.isSuccess()) {
                EnvirocarServiceUtils.assertStatusCode(allAnnouncementsResponse.code(),
                        allAnnouncementsResponse.message());
                return null;
            }

            // Store the announcements into the cache
            if (cacheDao != null) {
                LOG.info("Store the announcments into the cache DAO");
                cacheDao.saveAnnouncements(allAnnouncementsResponse.body());
            }

            // return the list of announcements.
            return allAnnouncementsResponse.body();
        } catch (IOException e) {
            throw new NotConnectedException(e);
        } catch (Exception e) {
            throw new DataRetrievalFailureException(e);
        }
    }

    @Override
    public Observable<List<Announcement>> getAllAnnouncementsObservable() {
        return Observable.create(
                new Observable.OnSubscribe<List<Announcement>>() {
                    @Override
                    public void call(Subscriber<? super List<Announcement>> subscriber) {
                        try {
                            List<Announcement> result = getAllAnnouncements();
                            subscriber.onNext(result);
                            subscriber.onCompleted();
                        } catch (Exception e) {
                            subscriber.onError(e);
                        }
                    }
                }
        );
    }

    @Override
    public void saveAnnouncements(List<Announcement> announcements) throws NotConnectedException {
        throw new NotConnectedException("No announcement upload allowed/supported!");
    }
}
