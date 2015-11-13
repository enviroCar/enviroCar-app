package org.envirocar.remote.service;


import javax.inject.Inject;

/**
 * @author dewall
 */
public class EnviroCarService {
    public static final String BASE_URL = "https://envirocar.org/api/dev/";

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
