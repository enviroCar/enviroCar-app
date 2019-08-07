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
package org.envirocar.remote.serializer;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.envirocar.core.entity.Car;
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
public class RemoteTrackListDeserializer implements JsonDeserializer<List<Track>> {
    private static final Logger LOG = Logger.getLogger(RemoteTrackListDeserializer.class);

    @Override
    public List<Track> deserialize(
            JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        JsonArray trackArray = json.getAsJsonObject().get("tracks").getAsJsonArray();

        List<Track> result = new ArrayList<>();
        for (int i = 0; i < trackArray.size(); i++) {
            JsonObject trackObject = trackArray.get(i).getAsJsonObject();

            // Get the delivered properties of a track.
            String id = trackObject.get(Track.KEY_TRACK_PROPERTIES_ID).getAsString();
            String modified = trackObject.get(Track
                    .KEY_TRACK_PROPERTIES_MODIFIED).getAsString();
            String name = trackObject.get(Track.KEY_TRACK_PROPERTIES_NAME).getAsString();
            String begin = trackObject.get(Track.KEY_TRACK_PROPERTIES_BEGIN).getAsString();
            String end = trackObject.get(Track.KEY_TRACK_PROPERTIES_END).getAsString();
            JsonObject carObject = trackObject.get(Track.KEY_TRACK_PROPERTIES_SENSOR)
                    .getAsJsonObject();
            Car car = context.deserialize(carObject, Car.class);
            // Create a new remote track.
            Track remoteTrack = new TrackImpl(Track.DownloadState.REMOTE);
            remoteTrack.setRemoteID(id);
            remoteTrack.setName(name);
            remoteTrack.setBegin(begin);
            remoteTrack.setEnd(end);
            remoteTrack.setCar(car);

            if (trackObject.has(Track.KEY_TRACK_PROPERTIES_LENGTH)) {
                double length = trackObject.get(Track.KEY_TRACK_PROPERTIES_LENGTH).getAsDouble();
                remoteTrack.setLength(length);
            }


            try {
                remoteTrack.setLastModified(Util.isoDateToLong(modified));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            // add it to result list.
            result.add(remoteTrack);
        }

        return result;
    }
}
