package org.envirocar.core.entity;

import com.google.common.collect.Maps;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class UserStatisticsImpl implements UserStatistics {
    protected Map<String, Phenomenon> phenomenonMap = Maps.newHashMap();

    @Override
    public Phenomenon getStatistic(String phenomenon) {
        return this.phenomenonMap.get(phenomenon);
    }

    @Override
    public void setStatistic(Phenomenon phenomenon) {
        this.phenomenonMap.put(phenomenon.getPhenomenonName(), phenomenon);
    }

    @Override
    public Map<String, Phenomenon> getStatistics() {
        return this.phenomenonMap;
    }

    @Override
    public void setStatistics(Map<String, Phenomenon> statistics) {
        this.phenomenonMap = statistics;
    }

    @Override
    public UserStatistics carbonCopy() {
        UserStatisticsImpl userStatistics = new UserStatisticsImpl();
        userStatistics.phenomenonMap = new HashMap<>(phenomenonMap);
        return userStatistics;
    }
}