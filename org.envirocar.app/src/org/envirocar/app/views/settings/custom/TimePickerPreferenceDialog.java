/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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

import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import org.envirocar.app.R;
import org.envirocar.app.databinding.PreferenceTimepickerDialogBinding;
import org.envirocar.app.handler.ApplicationSettings;

import java.lang.reflect.Field;




public class TimePickerPreferenceDialog extends PreferenceDialogFragmentCompat {

    public interface TimePickerPreference {
        int getTime();

        void setTime(int time);
    }

    public static TimePickerPreferenceDialog newInstance(String key) {
        final TimePickerPreferenceDialog dialog = new TimePickerPreferenceDialog();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        dialog.setArguments(b);
        return dialog;
    }

    private static final NumberPicker.Formatter FORMATTER_TWO_DIGITS = new NumberPicker.Formatter() {
        final StringBuilder mBuilder = new StringBuilder();
        final java.util.Formatter mFormatter = new java.util.Formatter(mBuilder, java.util.Locale.US);
        final Object[] mArgs = new Object[1];

        public String format(int value) {
            mArgs[0] = value;
            mBuilder.delete(0, mBuilder.length());
            mFormatter.format("%02d", mArgs);
            return mFormatter.toString();
        }
    };

    // The seconds and minutes the timepicker have to show
    private int currentSeconds;
    private int currentMinutes;



    protected TextView text;

    protected NumberPicker minutePicker;

    protected NumberPicker secondsPicker;

    protected PreferenceTimepickerDialogBinding binding;
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        binding = PreferenceTimepickerDialogBinding.bind(view);
        text = binding.preferenceTimepickerText;
        minutePicker = binding.preferenceTimepickerMinutesPicker;
        secondsPicker = binding.preferenceTimepickerSecondsPicker;

        // inject view

        // Set the textview text
        this.text.setText(R.string.pref_bt_discovery_interval_explanation);

        // set the settings for the minute NumberPicker.
        this.minutePicker.setMinValue(0);
        this.minutePicker.setMaxValue(10);
        this.minutePicker.setOnLongPressUpdateInterval(100);
        this.minutePicker.setFormatter(FORMATTER_TWO_DIGITS);
        this.minutePicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            int minValue = minutePicker.getMinValue();
            int maxValue = minutePicker.getMaxValue();

            if (oldVal == maxValue && newVal == minValue) {
            }
        });

        // set the settings for the seconds number picker.
        this.secondsPicker.setMinValue(0);
        this.secondsPicker.setMaxValue(59);
//        if (this.displaySeconds != null)
//            this.secondsPicker.setDisplayedValues(displaySeconds);
        this.secondsPicker.setOnLongPressUpdateInterval(100);
        this.secondsPicker.setFormatter(FORMATTER_TWO_DIGITS);
        this.secondsPicker.setOnValueChangedListener((spinner, oldVal, newVal) -> {
            int minValue = secondsPicker.getMinValue();
            int maxValue = secondsPicker.getMaxValue();
            if (oldVal == maxValue && newVal == minValue) {
                int newMinute = minutePicker.getValue() + 1;
                minutePicker.setValue(newMinute);
            } else if (oldVal == minValue && newVal == maxValue) {
                int newMinute = minutePicker.getValue() - 1;
                minutePicker.setValue(newMinute);
            }
        });

        // hack for showing the initial value
        try {
            Field field = NumberPicker.class.getDeclaredField("mInputText");
            field.setAccessible(true);
            EditText numberPickerText = (EditText) field.get(minutePicker);
            numberPickerText.setFilters(new InputFilter[0]);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        // setting the initial value
        DialogPreference preference = getPreference();
        if (preference instanceof TimePickerPreference) {
            int time = ((TimePickerPreference) preference).getTime();

            int seconds = time % 60;
            int minutes = (time - seconds) / 60;

            currentMinutes = minutes;
            currentSeconds = seconds;

            secondsPicker.setValue(currentSeconds);
            minutePicker.setValue(currentMinutes);
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            currentMinutes = minutePicker.getValue();
            currentSeconds = secondsPicker.getValue();

            int time = minutePicker.getValue() * 60 + secondsPicker.getValue();

            // save the result
            DialogPreference preference = getPreference();
            if (preference instanceof TimePickerPreference) {
                TimePickerPreference intervalPref = ((TimePickerPreference) preference);
                intervalPref.setTime(time);
            }

            // set the discovery interval
            ApplicationSettings.setDiscoveryInterval(getActivity(), time);
        }
    }
}