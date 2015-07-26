package org.envirocar.app.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.envirocar.app.R;
import org.envirocar.app.application.CarManager;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.logging.Logger;

import javax.inject.Inject;

import butterknife.ButterKnife;

/**
 * @author dewall
 */
public class StartupFragment extends BaseInjectorFragment {
    private static final Logger LOGGER = Logger.getLogger(StartupFragment.class);


    @Inject
    protected CarManager mCarManager;

//    @Inject
//    protected

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LOGGER.info("onCreateView()");

        // First inflate the general dashboard view.
        View contentView = inflater.inflate(R.layout.fragment_startup, container, false);

        // Inject all dashboard-related views.
        ButterKnife.inject(this, contentView);

        return contentView;
    }
}
