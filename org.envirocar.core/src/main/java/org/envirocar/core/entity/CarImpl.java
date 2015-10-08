package org.envirocar.core.entity;

import java.io.Serializable;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class CarImpl implements Car {
    protected String id;
    protected String model;
    protected String manufacturer;
    protected FuelType fuelType;
    protected int constructionYear;
    protected int engineDisplacement;

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
    public int getEngineDisplacement() {
        return engineDisplacement;
    }

    @Override
    public void setEngineDisplacement(int engineDisplacement) {
        this.engineDisplacement = engineDisplacement;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(manufacturer);
        sb.append(" ");
        sb.append(model);
        sb.append(" ");
        sb.append(constructionYear);
        sb.append(" (");
        sb.append(fuelType);
        sb.append(" / ");
        sb.append(engineDisplacement);
        sb.append("cc)");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        boolean result = false;
        if (o instanceof CarImpl) {
            CarImpl c = (CarImpl) o;
            result = this.fuelType == c.fuelType
                    && this.manufacturer.equals(c.manufacturer)
                    && this.model.equals(c.model)
                    //                    && this.id.equals(c.id)
                    && this.constructionYear == c.constructionYear
                    && this.engineDisplacement == c.engineDisplacement;
        }
        return result;
    }

    @Override
    public int hashCode() {
        int result = fuelType.hashCode();
        result = 31 * result + manufacturer.hashCode();
        result = 31 * result + model.hashCode();
        //        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + constructionYear;
        result = 31 * result + engineDisplacement;
        return result;
    }
}
