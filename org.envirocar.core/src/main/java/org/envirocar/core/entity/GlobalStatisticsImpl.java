package org.envirocar.core.entity;

import android.provider.Settings;

import java.util.HashMap;
import java.util.Map;

public class GlobalStatisticsImpl implements GlobalStatistics {
    
    protected Map<String, Phenomenon> phenomenonMap = new HashMap<>();

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
    public GlobalStatistics carbonCopy() {
        GlobalStatisticsImpl globalStatistics = new GlobalStatisticsImpl();
        globalStatistics.phenomenonMap = new HashMap<>(phenomenonMap);
        return globalStatistics;
    }
}
