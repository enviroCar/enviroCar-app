package org.envirocar.app.views.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.envirocar.app.R;
import org.envirocar.app.views.settings.custom.AutoConnectIntervalPreference;
import org.envirocar.app.views.settings.custom.GPSTrimDurationPreference;
import org.envirocar.app.views.settings.custom.SamplingRatePreference;
import org.envirocar.app.views.settings.custom.TimePickerPreferenceDialog;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author dewall
 */
public class SettingsActivity2 extends AppCompatActivity {

    @BindView(R.id.activity_settings_toolbar)
    protected Toolbar toolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        // set toolbar
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.settings);

        // add the settingsfragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.activity_settings_content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.settings);
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            DialogFragment fragment = null;
            if (preference instanceof AutoConnectIntervalPreference) {
                fragment = TimePickerPreferenceDialog.newInstance(preference.getKey());
            } else if (preference instanceof SamplingRatePreference) {
                fragment = SamplingRatePreference.Dialog.newInstance(preference.getKey());
            } else if (preference instanceof GPSTrimDurationPreference) {
                fragment = TimePickerPreferenceDialog.newInstance(preference.getKey());
            }

            if (fragment != null) {
                fragment.setTargetFragment(this, 0);
                fragment.show(this.getFragmentManager(), "android.support.v7.preference.PreferenceFragment.DIALOG");
            } else {
                super.onDisplayPreferenceDialog(preference);
            }
        }
    }
}
