/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
