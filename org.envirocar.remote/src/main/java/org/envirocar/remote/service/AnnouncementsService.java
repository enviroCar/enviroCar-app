package org.envirocar.remote.service;


import org.envirocar.core.entity.Announcement;

import java.util.List;

import retrofit.Call;
import retrofit.http.GET;
import rx.Observable;

/**
 *
 * Retrofit remoteService interface that describes the access to the fuelings endpoints of the envirocar
 * remoteService.
 *
 * @author dewall
 */
public interface AnnouncementsService {

    @GET("announcements")
    Call<List<Announcement>> getAllAnnouncements();

    @GET("announcements")
    Observable<List<Announcement>> getAllAnnouncementsObservable();
}
