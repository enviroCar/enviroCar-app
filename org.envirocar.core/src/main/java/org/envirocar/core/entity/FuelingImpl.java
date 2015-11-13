package org.envirocar.core.entity;

/**
 * TODO JavaDoc
 *
 * @uthor dewall
 */
public class FuelingImpl implements Fueling {
    private Car car;
    private String comment;
    private long time;
    private boolean missedFuelStop;

    private double milage;
    private MilageUnit milageUnit;
    private double cost;
    private CostUnit costUnit;
    private double volume;
    private VolumeUnit volumeUnit;

    @Override
    public Fueling carbonCopy() {
        return null;
    }

    @Override
    public Car getCar() {
        return car;
    }

    @Override
    public void setCar(Car car) {
        this.car = car;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public void setComment(String comment) {
        this.comment = comment;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public boolean isMissedFuelStop() {
        return missedFuelStop;
    }

    public void setMissedFuelStop(boolean missedFuelStop) {
        this.missedFuelStop = missedFuelStop;
    }

    public double getMilage() {
        return milage;
    }

    public void setMilage(double milage, MilageUnit milageUnit) {
        this.milage = milage;
        this.milageUnit = milageUnit;
    }

    public MilageUnit getMilageUnit() {
        return milageUnit;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost, CostUnit costUnit) {
        this.cost = cost;
        this.costUnit = costUnit;
    }

    public CostUnit getCostUnit() {
        return costUnit;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume, VolumeUnit volumeUnit) {
        this.volume = volume;
        this.volumeUnit = volumeUnit;
    }

    public VolumeUnit getVolumeUnit() {
        return volumeUnit;
    }

    @Override
    public int compareTo(Fueling another) {
        if(another.getTime() == time){
            return 0;
        } else if(another.getTime() > time){
            return 1;
        } else {
            return -1;
        }
    }
}
