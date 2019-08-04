package org.envirocar.app.views.tracklist;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SortViewModel extends ViewModel {

    private final MutableLiveData<Integer> sortChoice = new MutableLiveData<>();
    private final MutableLiveData<Boolean> sortOrder = new MutableLiveData<>();
    private final MutableLiveData<Boolean> sortActive = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mapChoice = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mapActive = new MutableLiveData<>();

    public MutableLiveData<Boolean> getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Boolean bool){
            sortOrder.setValue(bool);
    }

    public MutableLiveData<Boolean> getSortActive() {
        return sortActive;
    }

    public void setSortActive(Boolean bool){
        sortActive.setValue(bool);
    }

    public MutableLiveData<Integer> getSortChoice() {
        return sortChoice;
    }

    public void setSortChoice(Integer integer) {
        sortChoice.setValue(integer);
    }

    public MutableLiveData<Boolean> getMapChoice() {
        return mapChoice;
    }

    public void setMapChoice(Boolean bool) {
        mapChoice.setValue(bool);
    }

    public MutableLiveData<Boolean> getMapActive() {
        return mapActive;
    }

    public void setMapActive(Boolean bool) {
        mapActive.setValue(bool);
    }
}
