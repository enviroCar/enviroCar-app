/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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

    @Override
    public Announcement carbonCopy() {
        AnnouncementImpl result = new AnnouncementImpl();
        result.id = id;
        result.versionRange = versionRange;
        result.priority = priority;
        result.category = category;
        result.contents = contents;
        return result;
    }
}
