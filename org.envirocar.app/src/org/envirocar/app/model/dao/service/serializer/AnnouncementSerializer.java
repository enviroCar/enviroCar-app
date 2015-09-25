package org.envirocar.app.model.dao.service.serializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.envirocar.app.model.Announcement;
import org.envirocar.app.model.dao.service.AnnouncementsService;

import java.lang.reflect.Type;

/**
 * @author dewall
 */
public class AnnouncementSerializer implements JsonDeserializer<Announcement> {
    @Override
    public Announcement deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext
            context) throws JsonParseException {

        // Parse all attributes of the announcements json element.
        JsonObject announcementObject = json.getAsJsonObject();
        String id = announcementObject.get(AnnouncementsService.KEY_ANNOUNCEMENTS_ID)
                .getAsString();
        String versionRange = announcementObject.get(AnnouncementsService
                .KEY_ANNOUNCEMENTS_VERSIONS).getAsString();
        String category = announcementObject.get(AnnouncementsService.KEY_ANNOUNCEMENTS_CATEGORY)
                .getAsString();
        String priority = announcementObject.get(AnnouncementsService.KEY_ANNOUNCEMENTS_PRIO)
                .getAsString();
        String contents = announcementObject.get(AnnouncementsService.KEY_ANNOUNCEMENTS_CONTENT)
                .getAsString();

        // If it is not a valid category that belongs to a smartphone, then ignore it.
        if (category == null ||
                !(category.equalsIgnoreCase(
                        AnnouncementsService.KEY_ANNOUNCEMENTS_CATEGORY_APP)) ||
                category.equalsIgnoreCase(
                        AnnouncementsService.KEY_ANNOUNCEMENTS_CATEGORY_GENERAL)) {
            return null;
        }

        return new Announcement(id, versionRange, priority, category, contents);
    }
}
