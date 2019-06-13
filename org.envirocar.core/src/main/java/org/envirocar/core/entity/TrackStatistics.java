package org.envirocar.core.entity;

import java.util.Map;

public interface TrackStatistics extends BaseEntity<TrackStatistics>{

    String KEY_USER_STAT_CONSUMPTION = "Consumption";
    String KEY_USER_STAT_SPEED = "Speed";

    Phenomenon getStatistic(String phenomenon);

    void setStatistic(Phenomenon phenomenon);

    Map<String, Phenomenon> getStatistics();

    void setStatistics(Map<String, Phenomenon> statistics);

}
