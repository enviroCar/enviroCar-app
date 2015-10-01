package org.envirocar.app.model.dao.service.serializer;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.envirocar.app.model.dao.service.TrackService;
import org.envirocar.app.storage.RemoteTrack;

import java.lang.reflect.Type;
import java.util.List;

/**
 * @author dewall
 */
public class RemoteTrackListDeserializer implements JsonDeserializer<List<RemoteTrack>> {

    @Override
    public List<RemoteTrack> deserialize(
            JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        JsonArray trackArray = json.getAsJsonObject().get("tracks").getAsJsonArray();

        List<RemoteTrack> result = Lists.newArrayList();
        for (int i = 0; i < trackArray.size(); i++) {
            JsonObject trackObject = trackArray.get(i).getAsJsonObject();

            // Get the delivered properties of a track.
            String id = trackObject.get(TrackService.KEY_TRACK_PROPERTIES_ID).getAsString();
            //            String modified = trackObject.get(TrackService
            // .KEY_TRACK_PROPERTIES_MODIFIED)
            //                    .getAsString();
            String name = trackObject.get(TrackService.KEY_TRACK_PROPERTIES_NAME).getAsString();

            // Create a new remote track.
            RemoteTrack remoteTrack = RemoteTrack.createRemoteTrack(id);
            remoteTrack.setName(name);

            // add it to result list.
            result.add(remoteTrack);
        }

        return result;
    }
}
