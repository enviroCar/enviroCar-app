package org.envirocar.core.entity;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface Fueling extends BaseEntity<Fueling>, Comparable<Fueling> {
    String KEY_REMOTE_ID = "id";
    String KEY_FUELINGS = "fuelings";
    String KEY_COMMENT = "comment";
    String KEY_MISSED_FUEL_STOP = "missedFuelStop";
    String KEY_CAR = "car";
    String KEY_FUEL_TYPE = "fuelType";
    String KEY_COST = "cost";
    String KEY_MILEAGE = "mileage";
    String KEY_VOLUME = "volume";
    String KEY_TIME = "time";

    String KEY_VALUE = "value";
    String KEY_UNIT = "unit";

    interface Unit {
        String getUnit();

        String getName();
    }

    enum MilageUnit implements Unit {
        KILOMETRES("Kilometres", "km"),
        MILES("Miles", "mile");

        private final String name;
        private final String unit;

        /**
         * Constructor.
         *
         * @param name full name of the unit
         * @param unit short name of the unit
         */
        MilageUnit(String name, String unit) {
            this.name = name;
            this.unit = unit;
        }

        @Override
        public String toString() {
            return getName() + " (" + getUnit() + ")";
        }

        @Override
        public String getUnit() {
            return unit;
        }

        @Override
        public String getName() {
            return name;
        }

        public static MilageUnit fromString(String unit) {
            if (unit != null) {
                for (MilageUnit u : MilageUnit.values()) {
                    if (unit.equalsIgnoreCase(u.toString())) {
                        return u;
                    }
                }
            }
            return null;
        }
    }

    enum CostUnit implements Unit {
        DOLLAR("Dollar", "$"),
        EURO("Euro", "€");

        private final String name;
        private final String unit;

        /**
         * Constructor.
         *
         * @param name full name of the unit
         * @param unit short name of the unit
         */
        CostUnit(String name, String unit) {
            this.name = name;
            this.unit = unit;
        }

        @Override
        public String toString() {
            return getName() + " (" + getUnit() + ")";
        }

        @Override
        public String getUnit() {
            return unit;
        }

        @Override
        public String getName() {
            return name;
        }

        public static CostUnit fromString(String unit) {
            if (unit != null) {
                for (CostUnit u : CostUnit.values()) {
                    if (unit.equalsIgnoreCase(u.toString())) {
                        return u;
                    }
                }
            }
            return null;
        }
    }

    enum VolumeUnit implements Unit {
        LITRES("Litre", "l"),
        GALLON_IMP("Gallon", "Imp.gal."),
        GALLON_US("Gallon", "Us.liq.gal.");

        private final String name;
        private final String unit;

        /**
         * Constructor.
         *
         * @param name full unit name.
         * @param unit unit short.
         */
        VolumeUnit(String name, String unit) {
            this.name = name;
            this.unit = unit;
        }

        @Override
        public String toString() {
            return getName() + " (" + getUnit() + ")";
        }

        @Override
        public String getUnit() {
            return unit;
        }

        @Override
        public String getName() {
            return name;
        }

        public static VolumeUnit fromString(String unit) {
            if (unit != null) {
                for (VolumeUnit u : VolumeUnit.values()) {
                    if (unit.equalsIgnoreCase(u.toString())) {
                        return u;
                    }
                }
            }
            return null;
        }
    }

    String getRemoteID();

    void setRemoteID(String remoteID);

    Car getCar();

    void setCar(Car car);

    String getComment();

    void setComment(String comment);

    long getTime();

    void setTime(long time);

    boolean isMissedFuelStop();

    void setMissedFuelStop(boolean missedFuelStop);

    double getMilage();

    void setMilage(double milage, MilageUnit milageUnit);

    MilageUnit getMilageUnit();

    double getCost();

    void setCost(double cost, CostUnit costUnit);

    CostUnit getCostUnit();

    double getVolume();

    void setVolume(double volume, VolumeUnit volumeUnit);

    VolumeUnit getVolumeUnit();
}
