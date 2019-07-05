package org.envirocar.app.views.tracklist;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Date;
import java.util.List;

public class FilterViewModel extends ViewModel {

    private final MutableLiveData<Boolean> filterDate = new MutableLiveData<>();
    private final MutableLiveData<Date> filterDateAfter = new MutableLiveData<>();
    private final MutableLiveData<Date> filterDateBefore = new MutableLiveData<>();
    private final MutableLiveData<Boolean> filterCar = new MutableLiveData<>();
    private final MutableLiveData<String> filterCarName = new MutableLiveData<>();

    public MutableLiveData<Boolean> getFilterDate() {
        return filterDate;
    }

    public MutableLiveData<Date> getFilterDateAfter() {
        return filterDateAfter;
    }

    public MutableLiveData<Date> getFilterDateBefore() {
        return filterDateBefore;
    }

    public MutableLiveData<Boolean> getFilterCar() {
        return filterCar;
    }

    public MutableLiveData<String> getFilterCarName() {
        return filterCarName;
    }

    public void setFilterDate(Boolean bool){
        filterDate.setValue(bool);
    }

    public void setFilterDates(Date after, Date before){
        filterDateAfter.setValue(after);
        filterDateBefore.setValue(before);
    }

    public void setFilterCar(Boolean bool){
        filterCar.setValue(bool);
    }

    public void setFilterCarName(String name){
        filterCarName.setValue(name);
    }
}
