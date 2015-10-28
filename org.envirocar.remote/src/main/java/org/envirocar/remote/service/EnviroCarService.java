package org.envirocar.remote.service;


import org.envirocar.core.UserManager;
import org.envirocar.remote.dao.RemoteUserDAO;

import javax.inject.Inject;

import retrofit.Retrofit;

/**
 * @author dewall
 */
public class EnviroCarService {
    public static final String BASE_URL = "https://envirocar.org/api/dev/";

    @Inject
    protected static UserManager usermanager;
    @Inject
    protected static Retrofit retrofitClient;
    @Inject
    protected static RemoteUserDAO remoteUserDAO;

    @Inject
    protected static UserService userService;
    @Inject
    protected static CarService carService;
    @Inject
    protected static TrackService trackService;
    @Inject
    protected static TermsOfUseService termsOfUseService;
    @Inject
    protected static FuelingService fuelingService;
    @Inject
    protected static AnnouncementsService announcementsService;

    @Deprecated
    public static UserService getUserService() {
        return userService;
    }

    @Deprecated
    public static CarService getCarService() {
        return carService;
    }

    @Deprecated
    public static TrackService getTrackService() {
        return trackService;
    }

    @Deprecated
    public static TermsOfUseService getTermsOfUseService() {
        return termsOfUseService;
    }

    @Deprecated
    public static FuelingService getFuelingService() {
        return fuelingService;
    }

    @Deprecated
    public static AnnouncementsService getAnnouncementService() {
        return announcementsService;
    }
}
