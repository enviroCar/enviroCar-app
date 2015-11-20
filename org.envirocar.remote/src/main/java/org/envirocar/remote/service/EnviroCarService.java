/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
