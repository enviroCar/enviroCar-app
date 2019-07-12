package org.envirocar.app.views.tracklist;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SortViewModel extends ViewModel {

    private final MutableLiveData<Integer> sortChoice = new MutableLiveData<>();
    private final MutableLiveData<Boolean> sortOrder = new MutableLiveData<>();
    private final MutableLiveData<Boolean> sortActive = new MutableLiveData<>();

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
}
