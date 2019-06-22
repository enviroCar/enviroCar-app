package org.envirocar.app.views.statistics;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ChoiceViewModel extends ViewModel {

    private final MutableLiveData<Integer> selectedOption = new MutableLiveData<>();

    public MutableLiveData<Integer> getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(Integer choice){
        selectedOption.setValue(choice);
    }
}
