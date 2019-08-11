package org.envirocar.app.views.trackdetails;

public class TrackStatisticsDataHolder {

    private String phenomena;
    private String unit;
    private int resImg;
    private Float trackMax;
    private Float trackAvg;
    private Float userAvg;
    private Float globalAvg;
    private Boolean displayUserAndGlobalAvg;

    public TrackStatisticsDataHolder(String phenomena, int resImg, Float trackMax, Float trackAvg,
                                     Float userAvg, Float globalAvg, Boolean displayUserAndGlobalAvg, String unit) {
        this.phenomena = phenomena;
        this.unit = unit;
        this.resImg = resImg;
        this.trackMax = trackMax;
        this.trackAvg = trackAvg;
        this.userAvg = userAvg;
        this.globalAvg = globalAvg;
        this.displayUserAndGlobalAvg = displayUserAndGlobalAvg;
    }

    public TrackStatisticsDataHolder() {
    }

    public String getPhenomena() {
        return phenomena;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setPhenomena(String phenomena) {
        this.phenomena = phenomena;
    }

    public int getResImg() {
        return resImg;
    }

    public void setResImg(int resImg) {
        this.resImg = resImg;
    }

    public Float getTrackMax() {
        return trackMax;
    }

    public void setTrackMax(Float trackMax) {
        this.trackMax = trackMax;
    }

    public Float getTrackAvg() {
        return trackAvg;
    }

    public void setTrackAvg(Float trackAvg) {
        this.trackAvg = trackAvg;
    }

    public Float getUserAvg() {
        return userAvg;
    }

    public void setUserAvg(Float userAvg) {
        if (displayUserAndGlobalAvg == Boolean.TRUE)
            this.userAvg = userAvg;
        else
            this.userAvg = null;
    }

    public Float getGlobalAvg() {
        return globalAvg;
    }

    public void setGlobalAvg(Float globalAvg) {
        if (displayUserAndGlobalAvg == Boolean.TRUE)
            this.globalAvg = globalAvg;
        else
            this.globalAvg = null;
    }

    public Boolean getDisplayUserAndGlobalAvg() {
        return displayUserAndGlobalAvg;
    }

    public void setDisplayUserAndGlobalAvg(Boolean displayUserAndGlobalAvg) {
        this.displayUserAndGlobalAvg = displayUserAndGlobalAvg;
        if (displayUserAndGlobalAvg == Boolean.FALSE) {
            setGlobalAvg(null);
            setUserAvg(null);
        }
    }
}
