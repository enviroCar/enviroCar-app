package org.envirocar.app.views.tracklist;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Date;

public class FilterViewModel extends ViewModel {

    // To notify that a filter has been modified
    private final MutableLiveData<Boolean> filterActive = new MutableLiveData<>();

    // Holds whether the date filter is active or not
    private final MutableLiveData<Boolean> filterDate = new MutableLiveData<>();
    private final MutableLiveData<Date> filterDateStart = new MutableLiveData<>();
    private final MutableLiveData<Date> filterDateEnd = new MutableLiveData<>();

    // Holds whether the car filter is active or not
    private final MutableLiveData<Boolean> filterCar = new MutableLiveData<>();
    private final MutableLiveData<String> filterCarName = new MutableLiveData<>();

    public MutableLiveData<Boolean> getFilterActive() {
        return filterActive;
    }

    public void setFilterActive(Boolean bool) {
        filterActive.setValue(bool);
    }

    public MutableLiveData<Boolean> getFilterDate() {
        return filterDate;
    }

    public MutableLiveData<Date> getFilterDateStart() {
        return filterDateStart;
    }

    public MutableLiveData<Date> getFilterDateEnd() {
        return filterDateEnd;
    }

    public MutableLiveData<Boolean> getFilterCar() {
        return filterCar;
    }

    public MutableLiveData<String> getFilterCarName() {
        return filterCarName;
    }

    public void setFilterDate(Boolean bool) {
        filterDate.setValue(bool);
    }

    public void setFilterDates(Date after, Date before) {
        filterDateStart.setValue(after);
        filterDateEnd.setValue(before);
    }

    public void setFilterCar(Boolean bool) {
        filterCar.setValue(bool);
    }

    public void setFilterCarName(String name) {
        filterCarName.setValue(name);
    }
}
