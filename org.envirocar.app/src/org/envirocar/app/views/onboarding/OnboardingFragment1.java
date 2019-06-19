package org.envirocar.app.views.onboarding;


import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.envirocar.app.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class OnboardingFragment1 extends Fragment {


    public OnboardingFragment1() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.onboarding_1,container,false);
    }

}
