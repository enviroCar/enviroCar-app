package org.envirocar.remote.serializer;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.envirocar.core.entity.Phenomenon;
import org.envirocar.core.entity.PhenomenonImpl;
import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.TrackStatistics;
import org.envirocar.core.entity.TrackStatisticsImpl;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class TrackStatisticsDeserializer implements JsonDeserializer<TrackStatistics> {

    @Override
    public TrackStatistics deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext
            context) throws JsonParseException {

        Map<String, Phenomenon> statisticMap = new HashMap<>();
        // If we are getting the statistics for all the phenomena, we get a JSONArray
        // Instead if we query :trackid/statistics/:phenomenon , in order to get the statistics
        // for only a specific phenomenon, we get a single JSON object only
        if (json.getAsJsonObject().has(Track.KEY_STATISTICS)) {
            final JsonArray statistics = json.getAsJsonObject()
                    .get(Track.KEY_STATISTICS)
                    .getAsJsonArray();

            // Iterate through the array of phenomenons and its values.
            for (int i = 0; i < statistics.size(); i++) {
                // Create a new phenomenon holder.
                Phenomenon holder = new PhenomenonImpl();

                // Get the values of the phenomenon.
                JsonObject statisticObject = statistics.get(i).getAsJsonObject();
                holder.setMaxValue(statisticObject.get(Track.KEY_STATISTICS_MAX).getAsDouble());
                holder.setMinValue(statisticObject.get(Track.KEY_STATISTICS_MIN).getAsDouble());
                holder.setAvgValue(statisticObject.get(Track.KEY_STATISTICS_AVG).getAsDouble());

                // Get the phenomenon name and unit.
                JsonObject phenomenonObject = statisticObject.get(Track
                        .KEY_STATISTICS_PHENOMENON).getAsJsonObject();
                holder.setPhenomenonName(phenomenonObject.get(Track
                        .KEY_STATISTICS_PHENOMENON_NAME).getAsString());
                holder.setPhenomenonUnit(phenomenonObject.get(Track
                        .KEY_STATISTICS_PHENOMENON_UNIT).getAsString());

                // Add the phenomenon to the map.
                statisticMap.put(holder.getPhenomenonName(), holder);
            }
        } else {
            final JsonObject object = json.getAsJsonObject();
            Phenomenon holder = new PhenomenonImpl();

            // Get the values of the phenomenon.
            holder.setMaxValue(object.get(Track.KEY_STATISTICS_MAX).getAsDouble());
            holder.setMinValue(object.get(Track.KEY_STATISTICS_MIN).getAsDouble());
            holder.setAvgValue(object.get(Track.KEY_STATISTICS_AVG).getAsDouble());

            // Get the phenomenon name and unit.
            JsonObject phenomenonObject = object.get(Track
                    .KEY_STATISTICS_PHENOMENON).getAsJsonObject();
            holder.setPhenomenonName(phenomenonObject.get(Track
                    .KEY_STATISTICS_PHENOMENON_NAME).getAsString());
            holder.setPhenomenonUnit(phenomenonObject.get(Track
                    .KEY_STATISTICS_PHENOMENON_UNIT).getAsString());

            // Add the phenomenon to the map.
            statisticMap.put(holder.getPhenomenonName(), holder);
        }

        TrackStatistics trackStatistics = new TrackStatisticsImpl();
        trackStatistics.setStatistics(statisticMap);

        return trackStatistics;
    }
}
