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
package org.envirocar.remote.serde;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.envirocar.core.entity.Announcement;
import org.envirocar.core.entity.AnnouncementImpl;
import org.envirocar.core.util.VersionRange;

import java.lang.reflect.Type;

/**
 * @author dewall
 */
public class AnnouncementSerde implements JsonSerializer<Announcement>, JsonDeserializer<Announcement> {

    @Override
    public JsonElement serialize(Announcement src, Type typeOfSrc, JsonSerializationContext
            context) {

        JsonObject result = new JsonObject();
        result.addProperty(Announcement.KEY_ANNOUNCEMENTS_ID, src.getId());
        result.addProperty(Announcement.KEY_ANNOUNCEMENTS_VERSIONS, src.getVersionRange()
                .toString());
        result.addProperty(Announcement.KEY_ANNOUNCEMENTS_PRIO, src.getPriority().toString());
        result.add(Announcement.KEY_ANNOUNCEMENTS_CONTENT, src.getContent());

        String category = src.getCategory();
        if (category != null) {
            result.addProperty(Announcement.KEY_ANNOUNCEMENTS_CATEGORY, category);
        }

        return result;
    }

    @Override
    public Announcement deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext
            context) throws JsonParseException {

        // Parse all attributes of the announcements json element.
        JsonObject announcementObject = json.getAsJsonObject();
        String id = announcementObject.get(Announcement.KEY_ANNOUNCEMENTS_ID)
                .getAsString();
        String versionRange = announcementObject.get(Announcement
                .KEY_ANNOUNCEMENTS_VERSIONS).getAsString();
        String category = announcementObject.get(Announcement.KEY_ANNOUNCEMENTS_CATEGORY)
                .getAsString();
        String priority = announcementObject.get(Announcement.KEY_ANNOUNCEMENTS_PRIO)
                .getAsString();
        String contents = announcementObject.get(Announcement.KEY_ANNOUNCEMENTS_CONTENT)
                .getAsString();

        // If it is not a valid category that belongs to a smartphone, then ignore it.
        if (category == null ||
                !(category.equalsIgnoreCase(
                        Announcement.KEY_ANNOUNCEMENTS_CATEGORY_APP)) ||
                category.equalsIgnoreCase(
                        Announcement.KEY_ANNOUNCEMENTS_CATEGORY_GENERAL)) {
            return null;
        }

        Announcement announcement = new AnnouncementImpl();
        announcement.setId(id);
        announcement.setVersionRange(VersionRange.fromString(versionRange));
        announcement.setPriority(Announcement.Priority.fromString(priority));
        announcement.setCategory(category);
        announcement.setContent(new JsonParser().parse(contents).getAsJsonObject());

        return announcement;
    }


}
