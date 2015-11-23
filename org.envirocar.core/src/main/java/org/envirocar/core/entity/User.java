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
package org.envirocar.core.entity;

import org.envirocar.core.util.VersionRange;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface User extends BaseEntity<User> {
    String KEY_USER_NAME = "name";
    String KEY_USER_MAIL = "mail";
    String KEY_USER_TOKEN = "token";
    String KEY_USER_TOU_VERSION = "touVersion";
    String KEY_USER_TOU_ACCEPTED = "acceptedTermsOfUseVersion";

    String KEY_STATISTICS = "statistics";
    String KEY_STATISTICS_MAX = "max";
    String KEY_STATISTICS_MIN = "min";
    String KEY_STATISTICS_AVG = "avg";
    String KEY_STATISTICS_MEASUREMENTS = "measurements";
    String KEY_STATISTICS_TRACKS = "tracks";
    String KEY_STATISTICS_SENSORS = "sensors";
    String KEY_STATISTICS_PHENOMENON = "phenomenon";
    String KEY_STATISTICS_PHENOMENON_NAME = "name";
    String KEY_STATISTICS_PHENOMENON_UNIT = "unit";

    String getUsername();

    void setUsername(String username);

    String getMail();

    void setMail(String mail);

    String getToken();

    void setToken(String token);

    String getTermsOfUseVersion();

    void setTermsOfUseVersion(String termsOfUseVersion);

    VersionRange getVersionRange();

    void setVersionRange();

    User carbonCopy();
}
