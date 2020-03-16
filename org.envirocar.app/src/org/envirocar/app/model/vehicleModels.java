package org.envirocar.app.model;

import java.util.ArrayList;

public class vehicleModels {
    ArrayList<Object> links = new ArrayList<Object>();
    String tsn;
    String commercialName;
    String allotmentDate;
    public vehicleModels(String tsn, String commercialName, String allotmentDate) {
        this.tsn = tsn;
        this.commercialName = commercialName;
        this.allotmentDate = allotmentDate;
    }

    public String getTsn() {
        return tsn;
    }

    public void setTsn(String tsn) {
        this.tsn = tsn;
    }

    public String getCommercialName() {
        return commercialName;
    }

    public void setCommercialName(String commercialName) {
        this.commercialName = commercialName;
    }

    public String getAllotmentDate() {
        return allotmentDate;
    }

    public void setAllotmentDate(String allotmentDate) {
        this.allotmentDate = allotmentDate;
    }
}
