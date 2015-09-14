package org.envirocar.app.view.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;

import org.envirocar.app.R;
import org.envirocar.app.util.Util;

/**
 * @author dewall
 */
public class OtherSettingsFragment extends PreferenceFragment {
    private static final String KEY_PREF_WEBSITE = "preference_other_website";
    private static final String KEY_PREF_VERSION = "preference_other_version";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the preference layout.
        addPreferencesFromResource(R.xml.preferences_other);

        // Set an onClick listener on the preference that opens the envirocar website in a webview.
        Preference website = findPreference(KEY_PREF_WEBSITE);
        website.setOnPreferenceClickListener(preference -> {
            Intent i = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(getResources().getString(R.string.envirocar_org)));
            startActivity(i);
            return true;
        });

        // Set the version of the application as
        Preference version = findPreference(KEY_PREF_VERSION);
        version.setSummary(Util.getVersionString(getActivity()));
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set the background color from transparent to white.
        getView().setBackgroundColor(getResources().getColor(R.color.white_cario));
    }
}
