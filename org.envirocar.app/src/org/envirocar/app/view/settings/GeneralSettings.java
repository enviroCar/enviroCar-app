package org.envirocar.app.view.settings;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.view.View;

import org.envirocar.app.R;

/**
 * @author dewall
 */
public class GeneralSettings extends PreferenceFragment {
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
