package org.envirocar.app.view.dashboard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.logging.Logger;

import butterknife.ButterKnife;

/**
 * @author dewall
 */
public class DashboardTempomatFragment extends BaseInjectorFragment {
    private static final Logger LOGGER = Logger.getLogger(DashboardTempomatFragment.class);


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        LOGGER.info("onCreateView()");

        setHasOptionsMenu(true);

        // First inflate the general dashboard view.
        View contentView = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Inject all dashboard-related views.
        ButterKnife.inject(this, contentView);

        // return the inflated content view.
        return contentView;
    }

    @Override
    public void onResume() {
        LOGGER.info("onResume()");
        super.onResume();
//        getFragmentManager().beginTransaction()
//                .setCustomAnimations(R.anim.slide_in_top, R.anim.slide_out_top)
//                .show(mDashboardHeaderFragment)
//                .commit();
    }

    //    @Override
    //    public void onViewCreated(View view, Bundle savedInstanceState) {
    //        LOGGER.info("onViewCreated()");
    //
    //        mTimerText.setBase(SystemClock.elapsedRealtime()-10000);
    //        mTimerText.start();
    //
    //        super.onViewCreated(view, savedInstanceState);
    //    }


    @Override
    public void onDestroyView() {
        LOGGER.info("onDestroyView()");
//        if (!getActivity().isFinishing() && mDashboardHeaderFragment != null) {
//            getFragmentManager().beginTransaction()
//                    .remove(mDashboardHeaderFragment)
//                    .commit();
//        }
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_dashboard_tempomat, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
}
