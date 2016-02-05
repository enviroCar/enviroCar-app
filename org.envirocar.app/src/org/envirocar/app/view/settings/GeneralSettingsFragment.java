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
package org.envirocar.app.view.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.View;

import org.envirocar.app.R;

/**
 * @author dewall
 */
public class GeneralSettingsFragment extends PreferenceFragment {
    public static final String KEY_PREFERENCE = "KEY_PREF_RESSOURCE";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the resource id from the attached bundle
        Bundle bundle = this.getArguments();
        int resource = bundle.getInt(KEY_PREFERENCE, -1);

        if(resource != -1) {
            addPreferencesFromResource(resource);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // set a non-transparent white background
        view.setBackgroundColor(getResources().getColor(R.color.white_cario));
    }
}
