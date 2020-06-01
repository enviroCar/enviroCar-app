package org.envirocar.core.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "vehicles",
foreignKeys ={ @ForeignKey(entity = Manufacturers.class,
parentColumns = "id",
childColumns = "manufacturer_id"),
        @ForeignKey(entity = PowerSource.class,
parentColumns = "id",
childColumns = "power_source_id")
})
public class Vehicles {

    @PrimaryKey
    @ColumnInfo(name = "manufacturer_id")
    String manufacturer_id;

    @PrimaryKey
    @ColumnInfo(name = "id")
    String id;

    @ColumnInfo(name = "trade_name")
    String trade;

    @ColumnInfo(name = "commerical_name")
    String commerical_name;

    @ColumnInfo(name = "allotment_date")
    Date allotment_date;

    @ColumnInfo(name = "category")
    String category;

    @ColumnInfo(name = "bodywork")
    String bodywork;

    @ColumnInfo(name = "power_source_id")
    int power_source_id;

    @ColumnInfo(name = "power")
    int power;

    @ColumnInfo(name = "engine_capacity")
    String engine_capacity;

    @ColumnInfo(name = "axles")
    int axles;

    @ColumnInfo(name = "powered_axles")
    int powered_axles;

    @ColumnInfo(name = "seats")
    int seats;

    @ColumnInfo(name = "maximum_mass")
    int maximum_mass;

    public String getManufacturer_id() {
        return manufacturer_id;
    }

    public void setManufacturer_id(String manufacturer_id) {
        this.manufacturer_id = manufacturer_id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTrade() {
        return trade;
    }

    public void setTrade(String trade) {
        this.trade = trade;
    }

    public String getCommerical_name() {
        return commerical_name;
    }

    public void setCommerical_name(String commerical_name) {
        this.commerical_name = commerical_name;
    }

    public Date getAllotment_date() {
        return allotment_date;
    }

    public void setAllotment_date(Date allotment_date) {
        this.allotment_date = allotment_date;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBodywork() {
        return bodywork;
    }

    public void setBodywork(String bodywork) {
        this.bodywork = bodywork;
    }

    public int getPower_source_id() {
        return power_source_id;
    }

    public void setPower_source_id(int power_source_id) {
        this.power_source_id = power_source_id;
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public String getEngine_capacity() {
        return engine_capacity;
    }

    public void setEngine_capacity(String engine_capacity) {
        this.engine_capacity = engine_capacity;
    }

    public int getAxles() {
        return axles;
    }

    public void setAxles(int axles) {
        this.axles = axles;
    }

    public int getPowered_axles() {
        return powered_axles;
    }

    public void setPowered_axles(int powered_axles) {
        this.powered_axles = powered_axles;
    }

    public int getSeats() {
        return seats;
    }

    public void setSeats(int seats) {
        this.seats = seats;
    }

    public int getMaximum_mass() {
        return maximum_mass;
    }

    public void setMaximum_mass(int maximum_mass) {
        this.maximum_mass = maximum_mass;
    }
}
