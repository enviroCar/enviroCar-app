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

import com.google.common.base.MoreObjects;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class PhenomenonImpl implements Phenomenon {
    public String phenomenonName;
    public String phenomenonUnit;
    public double max;
    public double avg;
    public double min;

    @Override
    public String getPhenomenonName() {
        return phenomenonName;
    }

    @Override
    public void setPhenomenonName(String phenomenonName) {
        this.phenomenonName = phenomenonName;
    }

    @Override
    public String getPhenomenonUnit() {
        return phenomenonUnit;
    }

    @Override
    public void setPhenomenonUnit(String phenomenonUnit) {
        this.phenomenonUnit = phenomenonUnit;
    }

    @Override
    public double getMaxValue() {
        return max;
    }

    @Override
    public void setMaxValue(double max) {
        this.max = max;
    }

    @Override
    public double getAvgValue() {
        return avg;
    }

    @Override
    public void setAvgValue(double avg) {
        this.avg = avg;
    }

    @Override
    public double getMinValue() {
        return min;
    }

    @Override
    public void setMinValue(double min) {
        this.min = min;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this.getClass())
                .add("phenomenonName", phenomenonName)
                .add("phenomenonUnit", phenomenonUnit)
                .toString();
    }

    @Override
    public Phenomenon carbonCopy() {
        PhenomenonImpl res = new PhenomenonImpl();
        res.phenomenonName = phenomenonName;
        res.phenomenonUnit = phenomenonUnit;
        res.max = max;
        res.avg = avg;
        res.min = min;
        return res;
    }
}
