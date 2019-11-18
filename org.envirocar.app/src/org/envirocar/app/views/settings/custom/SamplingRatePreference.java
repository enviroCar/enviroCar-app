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
package org.envirocar.app.views.settings.custom;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import org.envirocar.app.R;
import org.envirocar.app.handler.ApplicationSettings;
import org.envirocar.core.logging.Logger;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author dewall
 */
public class SamplingRatePreference extends DialogPreference implements TimePickerPreferenceDialog.TimePickerPreference {
    private static final Logger LOG = Logger.getLogger(SamplingRatePreference.class);

    public SamplingRatePreference(Context context) {
        super(context);
    }

    public SamplingRatePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.preference_sampling_rate_dialog;
    }

    @Override
    public int getTime() {
        return getPersistedInt(ApplicationSettings.DEFAULT_SAMPLING_RATE);
    }

    @Override
    public void setTime(int time) {
        persistInt(time);
    }

    public static class Dialog extends PreferenceDialogFragmentCompat {

        private static final int MIN_VALUE = 2;
        private static final int MAX_VALUE = 30;

        public static Dialog newInstance(String key) {
            final Dialog fragment = new Dialog();
            final Bundle b = new Bundle(1);
            b.putString(ARG_KEY, key);
            fragment.setArguments(b);
            return fragment;
        }

        @BindView(R.id.preference_sampling_rate_dialog_text)
        protected TextView textView;
        @BindView(R.id.preference_sampling_rate_dialog_picker)
        protected NumberPicker numberPicker;


        @Override
        protected void onBindDialogView(View view) {
            super.onBindDialogView(view);

            // inject views
            ButterKnife.bind(this, view);

            // set min/max values
            this.numberPicker.setMinValue(MIN_VALUE);
            this.numberPicker.setMaxValue(MAX_VALUE);

            DialogPreference preference = getPreference();
            if (preference instanceof SamplingRatePreference){
                int value = ((SamplingRatePreference) preference).getTime();
                numberPicker.setValue(value);
            }
        }

        @Override
        public void onDialogClosed(boolean positiveResult) {
            if (positiveResult) {
                int currentSeconds = numberPicker.getValue();

                DialogPreference preference = getPreference();
                if (preference instanceof SamplingRatePreference) {
                    ((SamplingRatePreference) preference).setTime(currentSeconds);
                }

                ApplicationSettings.setSamplingRate(getActivity(), currentSeconds);
            }
        }
    }
}
