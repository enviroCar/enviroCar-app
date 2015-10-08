package org.envirocar.core.entity;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface Phenomenon extends BaseEntity {

    String getPhenomenonName();

    void setPhenomenonName(String phenomenonName);

    String getPhenomenonUnit();

    void setPhenomenonUnit(String phenomenonUnit);

    double getMaxValue();

    void setMaxValue(double maxValue);

    double getAvgValue();

    void setAvgValue(double avgValue);

    double getMinValue();

    void setMinValue(double minValue);
}
