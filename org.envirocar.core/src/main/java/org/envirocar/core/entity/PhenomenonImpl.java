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
}
