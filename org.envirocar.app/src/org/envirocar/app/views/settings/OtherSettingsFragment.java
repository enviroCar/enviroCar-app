/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;

import org.envirocar.app.R;
import org.envirocar.core.util.Util;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class OtherSettingsFragment extends PreferenceFragment {
    private static final String KEY_PREF_WEBSITE = "preference_other_website";
    private static final String KEY_PREF_VERSION = "preference_other_version";
    private static final String KEY_PREF_LICENSES = "preferences_other_licenses";

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

        Preference licenses = findPreference(KEY_PREF_LICENSES);
        licenses.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                displayLicenseDialog();
                return true;
            }
        });
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set the background color from transparent to white.
        getView().setBackgroundColor(getResources().getColor(R.color.white_cario));
    }

    private void displayLicenseDialog() {
        WebView webView = (WebView) LayoutInflater.from(getActivity()).inflate(R.layout
                .preference_fragment_licenses, null);
        webView.loadUrl("file:///android_asset/open_source_licenses.html");
        new AlertDialog.Builder(getActivity())
                .setTitle("Open Source Licenses\nNotices for libraries:")
                .setView(webView)
                .setPositiveButton("Ok", null)
                .show();
    }
}
