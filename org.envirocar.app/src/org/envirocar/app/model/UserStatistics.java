package org.envirocar.app.model;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;

import org.envirocar.app.model.dao.service.UserService;
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
        @SerializedName(UserService.KEY_STATISTICS_PHENOMENON_NAME)
        public String phenomenon;
        @SerializedName(UserService.KEY_STATISTICS_PHENOMENON_UNIT)
        public String phenomenonUnit;
        @SerializedName(UserService.KEY_STATISTICS_MAX)
        public double max;
        @SerializedName(UserService.KEY_STATISTICS_AVG)
        public double avg;
        @SerializedName(UserService.KEY_STATISTICS_MIN)
        public double min;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this.getClass())
                    .add(UserService.KEY_STATISTICS_PHENOMENON, phenomenon)
                    .add(UserService.KEY_STATISTICS_PHENOMENON_UNIT, phenomenonUnit)
                    .toString();
        }
    }

    private final Map<String, PhenomenonStatisticHolder> mStatHolderMap = Maps.newConcurrentMap();

    /**
     * Default constructor.
     */
    public UserStatistics() {}


    /**
     * Constructor.
     *
     * @param statisticsMap a hashmap holding the {@link PhenomenonStatisticHolder}
     */
    public UserStatistics(Map<String, PhenomenonStatisticHolder> statisticsMap) {
        mStatHolderMap.putAll(statisticsMap);
    }

    /**
     * @return
     */
    public Map<String, PhenomenonStatisticHolder> getStatistics() {
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
