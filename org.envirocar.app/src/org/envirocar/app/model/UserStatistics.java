package org.envirocar.app.model;

import com.google.common.collect.Maps;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * @author dewall
 */
public class UserStatistics {
    private static final String STATISTICS = "statistics";
    private static final String PHENOMENON = "phenomenon";
    private static final String PHENOMENON_NAME = "name";
    private static final String PHENOMENON_UNIT = "unit";
    private static final String STATISTIC_MAX = "max";
    private static final String STATISTIC_MIN = "min";
    private static final String STATISTIC_AVG = "avg";

    /**
     * Holder class that holds the most common statistics of a specific phenomenon.
     */
    public static class PhenomenonStatisticHolder {
        public String phenomenon;
        public String phenomenonUnit;
        public double max, avg, min;

        @Override
        public String toString() {
            return phenomenon + " " + phenomenonUnit;
        }
    }

    private final Map<String, PhenomenonStatisticHolder> mStatHolderMap = Maps.newConcurrentMap();

    /**
     *
     * @return
     */
    public Map<String, PhenomenonStatisticHolder> getStatistics(){
        return mStatHolderMap;
    }

    /**
     * @param json the parsed JSONObject holding the user statistics.
     * @return the parsed object as UserStatistics instance holding the user statistics.
     * @throws JSONException
     */
    public static UserStatistics fromJson(JSONObject json) throws JSONException {
        UserStatistics result = new UserStatistics();

        JSONArray statArray = json.getJSONArray(STATISTICS);
        for (int i = 0; i < statArray.length(); i++) {
            JSONObject obj = statArray.getJSONObject(i);

            PhenomenonStatisticHolder holder = new PhenomenonStatisticHolder();
            JSONObject phenomenonObj = obj.getJSONObject(PHENOMENON);
            holder.phenomenon = phenomenonObj.getString(PHENOMENON_NAME);
            holder.phenomenonUnit = phenomenonObj.getString(PHENOMENON_UNIT);

            holder.avg = obj.getDouble(STATISTIC_AVG);
            holder.max = obj.getDouble(STATISTIC_MAX);
            holder.min = obj.getDouble(STATISTIC_MIN);

            result.mStatHolderMap.put(holder.phenomenon, holder);
        }

        return result;
    }

}
