package org.envirocar.core.entity;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface Fueling extends BaseEntity<Fueling> {

    Car getCar();

    void setCar(Car car);

    String getComment();

    void setComment(String comment);
}