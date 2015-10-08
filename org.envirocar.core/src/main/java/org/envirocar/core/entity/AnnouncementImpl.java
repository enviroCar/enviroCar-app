package org.envirocar.core.entity;

import com.google.gson.JsonObject;

import org.envirocar.core.util.VersionRange;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class AnnouncementImpl implements Announcement {
    private String id;
    private VersionRange versionRange;
    private Priority priority;
    private String category;
    private JsonObject contents;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public VersionRange getVersionRange() {
        return versionRange;
    }

    @Override
    public void setVersionRange(VersionRange versionRange) {
        this.versionRange = versionRange;
    }

    @Override
    public JsonObject getContent() {
        return contents;
    }

    @Override
    public void setContent(JsonObject content) {
        this.contents = content;
    }

}
