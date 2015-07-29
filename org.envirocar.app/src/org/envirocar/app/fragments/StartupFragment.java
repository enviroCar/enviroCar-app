package org.envirocar.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.envirocar.app.R;
import org.envirocar.app.application.CarPreferenceHandler;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.view.carselection.CarSelectionActivity;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * @author dewall
 */
public class StartupFragment extends BaseInjectorFragment {
    private static final Logger LOGGER = Logger.getLogger(StartupFragment.class);


    @Inject
    protected CarPreferenceHandler mCarManager;


    @InjectView(R.id.fragment_startup_car_selection)
    protected View mCarTypeView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        LOGGER.info("onCreateView()");

        // First inflate the general dashboard view.
        View contentView = inflater.inflate(R.layout.fragment_startup, container, false);

        // Inject all dashboard-related views.
        ButterKnife.inject(this, contentView);

        mCarTypeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CarSelectionActivity.class);
                getActivity().startActivity(intent);
            }
        });

        return contentView;
    }
}
