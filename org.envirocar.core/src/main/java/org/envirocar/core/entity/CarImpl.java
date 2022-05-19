/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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

import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class CarImpl implements Car {
    private static final long serialVersionUID = 2533968678052900431L;

    protected String id;
    protected String model;
    protected String manufacturer;
    protected FuelType fuelType;
    protected int constructionYear;
    protected int engineDisplacement;
    protected int weight;
    protected String vehicleType;

    @Deprecated
    public static double ccmToLiter(int ccm) {
        float result = ccm / 1000.0f;
        return result;
    }

    /**
     * Constructor.
     */
    public CarImpl() {
    }

    /**
     * Constructor.
     *
     * @param manufacturer       the manufacturer
     * @param model              the model
     * @param fuelType           the fueltype
     * @param constructionYear   the year of construction
     */
    public CarImpl(String manufacturer, String model, FuelType fuelType, int constructionYear) {
        this(manufacturer, model, fuelType, constructionYear, -1);
    }

    /**
     * Constructor.
     *
     * @param manufacturer       the manufacturer
     * @param model              the model
     * @param fuelType           the fueltype as String
     * @param constructionYear   the year of construction
     * @param engineDisplacement the engine displacement
     */
    public CarImpl(String manufacturer, String model, String fuelType, int constructionYear, int
            engineDisplacement) {
        this(manufacturer, model, FuelType.resolveFuelType(fuelType), constructionYear,
                engineDisplacement);
    }

    /**
     * Constructor.
     *
     * @param manufacturer       the manufacturer
     * @param model              the model
     * @param fuelType           the fueltype
     * @param constructionYear   the year of construction
     * @param engineDisplacement the engine displacement
     */
    public CarImpl(String manufacturer, String model, FuelType fuelType, int constructionYear, int
            engineDisplacement) {
        this(null, manufacturer, model, fuelType, constructionYear, engineDisplacement);
    }

    /**
     * Constructor.
     *
     * @param id                 the identifier
     * @param manufacturer       the manufacturer
     * @param model              the model
     * @param fuelType           the fueltype
     * @param constructionYear   the year of construction
     * @param engineDisplacement the engine displacement
     */
    public CarImpl(String id, String manufacturer, String model, FuelType fuelType, int
            constructionYear, int engineDisplacement) {
        this.id = id;
        this.manufacturer = manufacturer;
        this.model = model;
        this.fuelType = fuelType;
        this.constructionYear = constructionYear;
        this.engineDisplacement = engineDisplacement;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public String getManufacturer() {
        return manufacturer;
    }

    @Override
    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    @Override
    public FuelType getFuelType() {
        return fuelType;
    }

    @Override
    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }

    @Override
    public void setFuelType(String fuelType) {
        this.fuelType = FuelType.resolveFuelType(fuelType);
    }

    @Override
    public int getConstructionYear() {
        return constructionYear;
    }

    @Override
    public void setConstructionYear(int constructionYear) {
        this.constructionYear = constructionYear;
    }

    @Override
    public boolean hasEngineDispalcement() {
        return this.fuelType != FuelType.ELECTRIC && engineDisplacement != -1;
    }

    @Override
    public int getEngineDisplacement() {
        return engineDisplacement;
    }

    @Override
    public void setEngineDisplacement(int engineDisplacement) {
        this.engineDisplacement = engineDisplacement;
    }

    @Override
    public boolean hasWeight() {
        return this.weight != 0;
    }

    @Override
    public int getWeight() {
        return this.weight;
    }

    @Override
    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public String getVehicleType() {
        return this.vehicleType;
    }

    @Override
    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }


    @Override
    public String toString() {
        return String.format("%s %s %d (%s / %dcc; %dkg; type: %s)", manufacturer, model, constructionYear, fuelType, engineDisplacement, weight, vehicleType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CarImpl car = (CarImpl) o;
        return constructionYear == car.constructionYear &&
                engineDisplacement == car.engineDisplacement &&
                Objects.equals(id, car.id) &&
                Objects.equals(model, car.model) &&
                Objects.equals(manufacturer, car.manufacturer) &&
                weight == car.weight &&
                Objects.equals(vehicleType, car.vehicleType) &&
                fuelType == car.fuelType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, model, manufacturer, fuelType, constructionYear, engineDisplacement, weight, vehicleType);
    }

    @Override
    public Car carbonCopy() {
        CarImpl res = new CarImpl();
        res.id = id;
        res.model = model;
        res.manufacturer = manufacturer;
        res.fuelType = fuelType;
        res.constructionYear = constructionYear;
        res.engineDisplacement = engineDisplacement;
        res.weight = weight;
        res.vehicleType = vehicleType;
        return res;
    }
}
