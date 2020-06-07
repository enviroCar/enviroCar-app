package org.envirocar.core.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;



@Entity(tableName = "vehicles",
primaryKeys = {"manufacturer_id","id"},
foreignKeys ={ @ForeignKey(entity = Manufacturers.class,
parentColumns = "id",
childColumns = "manufacturer_id"),
        @ForeignKey(entity = PowerSource.class,
parentColumns = "id",
childColumns = "power_source_id"),
})
public class Vehicles {


    @NonNull
    @ColumnInfo(name = "manufacturer_id")
    String manufacturer_id;

    @NonNull
    @ColumnInfo(name = "id")
    String id;

    @ColumnInfo(name = "trade_name")
    String trade;

    @ColumnInfo(name = "commerical_name")
    String commerical_name;

    @ColumnInfo(name = "allotment_date")
    String allotment_date;

    @ColumnInfo(name = "category")
    String category;

    @ColumnInfo(name = "bodywork")
    String bodywork;

    @ColumnInfo(name = "power_source_id")
    String power_source_id;

    @ColumnInfo(name = "power")
    String power;

    @ColumnInfo(name = "engine_capacity")
    String engine_capacity;

    @ColumnInfo(name = "axles")
    String axles;

    @ColumnInfo(name = "powered_axles")
    String powered_axles;

    @ColumnInfo(name = "seats")
    String seats;

    @ColumnInfo(name = "maximum_mass")
    String maximum_mass;

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

    public String getAllotment_date() {
        return allotment_date;
    }

    public void setAllotment_date(String allotment_date) {
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

    public String getPower_source_id() {
        return power_source_id;
    }

    public void setPower_source_id(String power_source_id) {
        this.power_source_id = power_source_id;
    }

    public String getPower() {
        return power;
    }

    public void setPower(String power) {
        this.power = power;
    }

    public String getEngine_capacity() {
        return engine_capacity;
    }

    public void setEngine_capacity(String engine_capacity) {
        this.engine_capacity = engine_capacity;
    }

    public String getAxles() {
        return axles;
    }

    public void setAxles(String axles) {
        this.axles = axles;
    }

    public String getPowered_axles() {
        return powered_axles;
    }

    public void setPowered_axles(String powered_axles) {
        this.powered_axles = powered_axles;
    }

    public String getSeats() {
        return seats;
    }

    public void setSeats(String seats) {
        this.seats = seats;
    }

    public String getMaximum_mass() {
        return maximum_mass;
    }

    public void setMaximum_mass(String maximum_mass) {
        this.maximum_mass = maximum_mass;
    }
}
