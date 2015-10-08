package org.envirocar.core.entity;

import com.google.gson.JsonObject;

import org.envirocar.core.util.VersionRange;
import org.json.JSONObject;

/**
 * @author dewall
 */
public interface Announcement extends BaseEntity {
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
