package org.envirocar.core.entity;

import org.envirocar.core.util.VersionRange;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface User extends BaseEntity {
    String KEY_USER_NAME = "name";
    String KEY_USER_MAIL = "mail";
    String KEY_USER_TOKEN = "token";
    String KEY_USER_TOU_VERSION = "touVersion";

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
}
