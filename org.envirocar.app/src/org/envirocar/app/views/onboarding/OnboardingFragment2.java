package org.envirocar.app.views.onboarding;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import org.envirocar.app.R;
import org.w3c.dom.Text;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 */
public class OnboardingFragment2 extends Fragment {


    public OnboardingFragment2() {
        // Required empty public constructor
    }

    @BindView(R.id.obd_onbaording_start)
    protected TextView obdOBStart;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.onboarding_2,container,false);
        ButterKnife.bind(this, view);
        obdOBStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(),OnboardingOBDActivity.class);
                startActivity(intent);
            }
        });
        return view;
    }

}
