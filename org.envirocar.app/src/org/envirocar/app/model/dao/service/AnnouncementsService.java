package org.envirocar.app.model.dao.service;

import org.envirocar.app.model.Announcement;

import java.util.List;

import retrofit.Call;
import retrofit.http.GET;
import rx.Observable;

/**
 *
 * Retrofit service interface that describes the access to the fuelings endpoints of the envirocar
 * service.
 *
 * @author dewall
 */
public interface AnnouncementsService {
    String KEY_ANNOUNCEMENTS = "announcements";
    String KEY_ANNOUNCEMENTS_ID = "id";
    String KEY_ANNOUNCEMENTS_VERSIONS = "versions";
    String KEY_ANNOUNCEMENTS_CATEGORY = "category";
    String KEY_ANNOUNCEMENTS_CATEGORY_APP = "app";
    String KEY_ANNOUNCEMENTS_CATEGORY_GENERAL = "general";
    String KEY_ANNOUNCEMENTS_CONTENT = "content";
    String KEY_ANNOUNCEMENTS_PRIO = "priority";

    @GET("announcements")
    Call<List<Announcement>> getAllAnnouncements();

    @GET("announcements")
    Observable<List<Announcement>> getAllAnnouncementsObservable();
}
