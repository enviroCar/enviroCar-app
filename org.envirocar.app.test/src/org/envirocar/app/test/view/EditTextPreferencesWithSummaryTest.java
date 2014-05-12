/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.app.test.view;

import org.envirocar.app.R;
import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.activity.preference.EditPreferenceWithSummary;
import org.envirocar.app.activity.preference.EditPreferenceWithSummary.MinMaxValueChangeListener;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.test.ActivityUnitTestCase;

public class EditTextPreferencesWithSummaryTest
		extends ActivityUnitTestCase<SettingsActivity> {

	private Intent intent;
	private SettingsActivity activity;


	public EditTextPreferencesWithSummaryTest() {
		super(SettingsActivity.class);
	}

    @Override
    protected void setUp() throws Exception {
        Context targetContext = getInstrumentation().getTargetContext();
        intent = new Intent(targetContext, SettingsActivity.class);
        super.setUp();
    }

    private void assembleActivity() {
        startActivity(intent, null, null);
        activity = getActivity();
    }

    private void assemblePreferences() {
        Context targetContext = getInstrumentation().getTargetContext();        
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(targetContext);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString("ec_sampling_rate", "5");
        editor.commit();
    }

    @SuppressWarnings("deprecation")
	public void testSummary() {
        assemblePreferences(); 
        assembleActivity();
        EditTextPreference samplingRateEdit = (EditTextPreference) activity.findPreference("ec_sampling_rate");
        assertEquals(String.format(getActivity().getResources().getString(R.string.sampling_rate_summary), "5"), samplingRateEdit.getSummary());
    }

	public void testMinMaxChangeListener() {
		assembleActivity();
		MinMaxValueChangeListener l = new EditPreferenceWithSummary.MinMaxValueChangeListener(0, 23);
		
		String key = "test_keyyyy";
		Preference pref = new Preference(getActivity());
		pref.setKey(key);
		
		l.onPreferenceChange(pref, "24");
		
		String val = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(key, "");
		
		assertEquals(val, "23");
		
		l.onPreferenceChange(pref, "-324");
		
		val = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(key, "");
		
		assertEquals(val, "0");
	}
	
}
