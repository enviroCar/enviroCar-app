package org.envirocar.app.model;

import java.util.ArrayList;

public class modelinformation {
    ArrayList<Object> links = new ArrayList<Object>();
    private String tsn;
    private String commercialName;
    private String allotmentDate;
    private String category;
    private String bodywork;
    private float power;
    private String engineCapacity;
    private float axles;
    private float poweredAxles;
    private float seats;
    private float maximumMass;


    // Getter Methods

    public String getTsn() {
        return tsn;
    }

    public String getCommercialName() {
        return commercialName;
    }

    public String getAllotmentDate() {
        return allotmentDate;
    }

    public String getCategory() {
        return category;
    }

    public String getBodywork() {
        return bodywork;
    }

    public float getPower() {
        return power;
    }

    public String getEngineCapacity() {
        return engineCapacity;
    }

    public float getAxles() {
        return axles;
    }

    public float getPoweredAxles() {
        return poweredAxles;
    }

    public float getSeats() {
        return seats;
    }

    public float getMaximumMass() {
        return maximumMass;
    }

    // Setter Methods

    public void setTsn( String tsn ) {
        this.tsn = tsn;
    }

    public void setCommercialName( String commercialName ) {
        this.commercialName = commercialName;
    }

    public void setAllotmentDate( String allotmentDate ) {
        this.allotmentDate = allotmentDate;
    }

    public void setCategory( String category ) {
        this.category = category;
    }

    public void setBodywork( String bodywork ) {
        this.bodywork = bodywork;
    }

    public void setPower( float power ) {
        this.power = power;
    }

    public void setEngineCapacity( String engineCapacity ) {
        this.engineCapacity = engineCapacity;
    }

    public void setAxles( float axles ) {
        this.axles = axles;
    }

    public void setPoweredAxles( float poweredAxles ) {
        this.poweredAxles = poweredAxles;
    }

    public void setSeats( float seats ) {
        this.seats = seats;
    }

    public void setMaximumMass( float maximumMass ) {
        this.maximumMass = maximumMass;
    }
}

