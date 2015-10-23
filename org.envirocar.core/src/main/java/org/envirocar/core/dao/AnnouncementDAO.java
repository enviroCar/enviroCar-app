package org.envirocar.core.dao;


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
public interface AnnouncementDAO {

    List<Announcement> getAllAnnouncements() throws DataRetrievalFailureException,
            NotConnectedException;

    Observable<List<Announcement>> getAllAnnouncementsObservable();

    void saveAnnouncements(List<Announcement> announcements) throws NotConnectedException, IOException;
}
