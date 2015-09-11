package org.envirocar.app.view.settings;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.google.common.base.Preconditions;

import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorActivity;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * @author dewall
 */
public class NewSettingsActivity extends BaseInjectorActivity {

    @InjectView(R.id.fragment_settings_main_toolbar)
    protected Toolbar mToolbar;

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

    private Fragment mCurrentVisibleFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_settings_main);
        ButterKnife.inject(this);

        setSupportActionBar(mToolbar);
        // Enables the home button.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
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
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right)
                    .remove(mCurrentVisibleFragment)
                    .commit();
            mCurrentVisibleFragment = null;
        } else {
            super.onBackPressed();
        }
    }

    @OnClick(R.id.fragment_settings_main_general_settings)
    protected void onClickGeneralSettings() {
        createAndShowSettingsFragment(R.xml.preferences_general);
    }

    @OnClick(R.id.fragment_settings_main_obd_settings)
    protected void onClickOBDSettings() {

    }

    @OnClick(R.id.fragment_settings_main_car_settings)
    protected void onClickCarSettings() {

    }

    @OnClick(R.id.fragment_settings_main_optional_settings)
    protected void onClickOptionalSettings() {

    }

    @OnClick(R.id.fragment_settings_main_other_settings)
    protected void onClickOtherSettings() {

    }

    /**
     * @param resource
     */
    private void createAndShowSettingsFragment(int resource) {
        Fragment settings = new GeneralSettings();

        // Set the preferences xml in the generated bundle.
        Bundle bundle = new Bundle();
        bundle.putInt(GeneralSettings.KEY_PREFERENCE, resource);
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
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right)
                .replace(R.id.fragment_settings_main_container, fragment)
                .commit();
        mCurrentVisibleFragment = fragment;
    }
}
