package org.envirocar.app.view.settings;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorFragment;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * @author dewall
 */
public class SettingsFragment2 extends BaseInjectorFragment {



    @InjectView(R.id.fragment_settings_main_general_settings)
    protected View mGeneralSettingsLayout;
    @InjectView(R.id.fragment_settings_main_obd_settings)
    protected View mOBDSettingsLayout;
    @InjectView(R.id.fragment_settings_main_car_settings)
    protected View mCarSettingsLayout;
    @InjectView(R.id.fragment_settings_main_optional_settings)
    protected View mOptionalSettingsLayout;
    @InjectView(R.id.fragment_settings_main_other_settings)
    protected View mOtherSettingsLayout;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        // Inflate the view.
        View view = inflater.inflate(R.layout.fragment_settings_main, container, false);

        // Inject the annotated subviews nad set the onClick listener.
        ButterKnife.inject(this, view);

        // Return the view.
        return view;
    }

    @OnClick(R.id.fragment_settings_main_general_settings)
    protected void onClickGeneralSettings(){

    }

    @OnClick(R.id.fragment_settings_main_obd_settings)
    protected void onClickOBDSettings(){

    }

    @OnClick(R.id.fragment_settings_main_car_settings)
    protected void onClickCarSettings(){

    }

    @OnClick(R.id.fragment_settings_main_optional_settings)
    protected void onClickOptionalSettings(){

    }

    @OnClick(R.id.fragment_settings_main_other_settings)
    protected void onClickOtherSettings(){

    }
}