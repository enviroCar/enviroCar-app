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

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.TrackImpl;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.Util;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class RemoteTrackListSerde extends AbstractJsonSerde implements JsonDeserializer<List<Track>> {
    private static final Logger LOG = Logger.getLogger(RemoteTrackListSerde.class);

    @Override
    public List<Track> deserialize(
            JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        JsonArray trackArray = json.getAsJsonObject().get("tracks").getAsJsonArray();

        List<Track> result = new ArrayList<>();
        for (int i = 0; i < trackArray.size(); i++) {
            JsonObject o = trackArray.get(i).getAsJsonObject();

            // Create a new remote track.
            Track remoteTrack = new TrackImpl(Track.DownloadState.REMOTE);
            remoteTrack.setRemoteID(o.get(Track.KEY_TRACK_ID).getAsString());
            remoteTrack.setName(o.get(Track.KEY_TRACK_NAME).getAsString());
            remoteTrack.setStartTime(parseStringAsTime(Track.KEY_TRACK_BEGIN, o));
            remoteTrack.setEndTime(parseStringAsTime(Track.KEY_TRACK_END, o));

            if (o.has(Track.KEY_TRACK_LENGTH)) {
                double length = o.get(Track.KEY_TRACK_LENGTH).getAsDouble();
                remoteTrack.setLength(length);
            }

            try {
                remoteTrack.setLastModified(Util.isoDateToLong(o.get(Track
                        .KEY_TRACK_MODIFIED).getAsString()));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            // add it to result list.
            result.add(remoteTrack);
        }

        return result;
    }
}
