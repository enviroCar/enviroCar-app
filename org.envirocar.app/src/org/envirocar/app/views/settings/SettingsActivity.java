/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.settings;

import android.app.Fragment;
import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class SettingsActivity extends BaseInjectorActivity {

    @BindView(R.id.fragment_settings_main_toolbar)
    protected Toolbar mToolbar;

    @BindView(R.id.fragment_settings_main_general_settings)
    protected View mGeneralSettingsLayout;
    @BindView(R.id.fragment_settings_main_obd_settings)
    protected View mOBDSettingsLayout;
    @BindView(R.id.fragment_settings_main_car_settings)
    protected View mCarSettingsLayout;
    @BindView(R.id.fragment_settings_main_optional_settings)
    protected View mOptionalSettingsLayout;
    @BindView(R.id.fragment_settings_main_other_settings)
    protected View mOtherSettingsLayout;

    private Fragment mCurrentVisibleFragment;

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_settings_main);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        // Enables the home button.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle(R.string.settings);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // just do the same as on back pressed.
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mCurrentVisibleFragment != null) {
            getFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_right)
                    .remove(mCurrentVisibleFragment)
                    .commit();
            mCurrentVisibleFragment = null;
            getSupportActionBar().setTitle(R.string.settings);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Called when the general settings layout is clicked. It creates and opens the general
     * settings fragment.
     */
    @OnClick(R.id.fragment_settings_main_general_settings)
    protected void onClickGeneralSettings() {
        getSupportActionBar().setTitle(R.string.pref_general_settings);
        createAndShowSettingsFragment(R.xml.preferences_general);
    }

    /**
     * Called when the OBD settings layout is clicked. It creates a new {@link
     * AutoConnectSettingsFragment} and opens this in the settings container.
     */
    @OnClick(R.id.fragment_settings_main_obd_settings)
    protected void onClickOBDSettings() {
        getSupportActionBar().setTitle(R.string.autoconnect_settings);
        showFragment(new AutoConnectSettingsFragment());
    }

    @OnClick(R.id.fragment_settings_main_car_settings)
    protected void onClickCarSettings() {
        Snackbar.make(mToolbar, "Not implemented yet!", Snackbar.LENGTH_LONG).show();
    }

    @OnClick(R.id.fragment_settings_main_optional_settings)
    protected void onClickOptionalSettings() {
        getSupportActionBar().setTitle(R.string.settings_optional);
        createAndShowSettingsFragment(R.xml.preferences_optional);
    }

    @OnClick(R.id.fragment_settings_main_other_settings)
    protected void onClickOtherSettings() {
        getSupportActionBar().setTitle(R.string.settings_other);
        showFragment(new OtherSettingsFragment());
    }

    /**
     * @param resource
     */
    private void createAndShowSettingsFragment(int resource) {
        Fragment settings = new GeneralSettingsFragment();

        // Set the preferences xml in the generated bundle.
        Bundle bundle = new Bundle();
        bundle.putInt(GeneralSettingsFragment.KEY_PREFERENCE, resource);
        settings.setArguments(bundle);

        // show the Settings fragment.
        showFragment(settings);
    }

    /**
     * @param fragment
     */
    private void showFragment(Fragment fragment) {
        // Set the fragment into the main container of the view.
        getFragmentManager()
                .beginTransaction()
                        // Set some animations.
                .setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_right)
                .replace(R.id.fragment_settings_main_container, fragment)
                .commit();
        mCurrentVisibleFragment = fragment;
    }
}
