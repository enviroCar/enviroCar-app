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
package org.envirocar.remote.service;


/**
 * @author dewall
 */
public class EnviroCarService {
    public static final String BASE_URL = "https://envirocar.org/api/dev/";

    protected static UserService userService;

    protected static CarService carService;

    protected static TrackService trackService;

    protected static TermsOfUseService termsOfUseService;

    protected static FuelingService fuelingService;

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

    public static void setUserService(UserService userService) {
        EnviroCarService.userService = userService;
    }

    public static void setCarService(CarService carService) {
        EnviroCarService.carService = carService;
    }

    public static void setTrackService(TrackService trackService) {
        EnviroCarService.trackService = trackService;
    }

    public static void setTermsOfUseService(TermsOfUseService termsOfUseService) {
        EnviroCarService.termsOfUseService = termsOfUseService;
    }

    public static void setFuelingService(FuelingService fuelingService) {
        EnviroCarService.fuelingService = fuelingService;
    }

    public static void setAnnouncementsService(AnnouncementsService announcementsService) {
        EnviroCarService.announcementsService = announcementsService;
    }
}
