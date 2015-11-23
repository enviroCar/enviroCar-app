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

import com.google.gson.JsonObject;

import org.envirocar.core.util.VersionRange;

/**
 * @author dewall
 */
public interface Announcement extends BaseEntity<Announcement> {
    String KEY_ANNOUNCEMENTS = "announcements";
    String KEY_ANNOUNCEMENTS_ID = "id";
    String KEY_ANNOUNCEMENTS_VERSIONS = "versions";
    String KEY_ANNOUNCEMENTS_CATEGORY = "category";
    String KEY_ANNOUNCEMENTS_CATEGORY_APP = "app";
    String KEY_ANNOUNCEMENTS_CATEGORY_GENERAL = "general";
    String KEY_ANNOUNCEMENTS_CONTENT = "content";
    String KEY_ANNOUNCEMENTS_PRIO = "priority";

     enum Priority {
        LOW {
            @Override
            public String toString() {
                return "low";
            }
        },
        MEDIUM {
            @Override
            public String toString() {
                return "medium";
            }
        },
        HIGH {
            @Override
            public String toString() {
                return "high";
            }
        };

        public static Priority fromString(String s) {
            for (Priority p : values()) {
                if (p.toString().equals(s)) {
                    return p;
                }
            }
            return LOW;
        }
    }


    String getId();

    void setId(String id);

    Priority getPriority();

    void setPriority(Priority priority);

    String getCategory();

    void setCategory(String category);

    VersionRange getVersionRange();

    void setVersionRange(VersionRange versionRange);

    JsonObject getContent();

    void setContent(JsonObject content);

}
