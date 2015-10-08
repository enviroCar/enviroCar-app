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
import org.envirocar.core.entity.Announcement;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;

import java.io.IOException;
import java.util.List;

import rx.Observable;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class CacheAnnouncementsDAO extends AbstractCacheDAO implements AnnouncementDAO {
    public static final String CACHE_FILE_NAME = "announcements";

    @Override
    public List<Announcement> getAllAnnouncements() throws DataRetrievalFailureException, NotConnectedException {
        return null;
    }

    @Override
    public Observable<List<Announcement>> getAllAnnouncementsObservable() {
        return null;
    }

    @Override
    public void saveAnnouncements(List<Announcement> announcements) throws NotConnectedException,
            IOException {

    }


    //    @Override
//    public List<Announcement> getAllAnnouncements() throws DataRetrievalFailureException {
//        Gson gson = new GsonBuilder()
//                .registerTypeAdapter(Announcement.class, new AnnouncementSerializer())
//                .create();
//        try {
//            return gson.fromJson(readCache(CACHE_FILE_NAME),
//                    new TypeToken<List<Announcement>>() {
//                    }.getType());
//        } catch (IOException e) {
//            throw new DataRetrievalFailureException(e);
//        }
//    }
//
//    @Override
//    public Observable<List<Announcement>> getAllAnnouncementsObservable() {
//        return Observable.create(new Observable.OnSubscribe<List<Announcement>>() {
//            @Override
//            public void call(Subscriber<? super List<Announcement>> subscriber) {
//                try {
//                    List<Announcement> announcements = getAllAnnouncements();
//                    subscriber.onNext(announcements);
//                    subscriber.onCompleted();
//                } catch (DataRetrievalFailureException e) {
//                    subscriber.onError(e);
//                }
//            }
//        });
//    }
//
//    @Override
//    public void saveAnnouncements(List<Announcement> announcements) throws NotConnectedException,
//            IOException {
//        storeCache(CACHE_FILE_NAME, GSON.toJson(announcements, new TypeToken<List<Announcement>>() {
//        }.getType()));
//    }
}
