package org.envirocar.core.entity;

import java.util.Map;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface UserStatistics extends BaseEntity {

    Phenomenon getStatistic(String phenomenon);

    void setStatistic(Phenomenon phenomenon);

    Map<String, Phenomenon> getStatistics();

    void setStatistics(Map<String, Phenomenon> statistics);
}
