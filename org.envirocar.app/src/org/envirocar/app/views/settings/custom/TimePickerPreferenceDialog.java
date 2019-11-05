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
import org.envirocar.app.handler.ApplicationSettings;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TimePickerPreferenceDialog extends PreferenceDialogFragmentCompat {

    public interface TimePickerPreference {
        int getTime();

        void setTime(int time);
    }

    public static TimePickerPreferenceDialog newInstance(String key) {
        return newInstance(key, null);
    }

    public static TimePickerPreferenceDialog newInstance(String key, String[] displaySeconds) {
        final TimePickerPreferenceDialog dialog = new TimePickerPreferenceDialog(displaySeconds);
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

    private String[] displaySeconds;

    @BindView(R.id.bluetooth_discovery_interval_preference_numberpicker_text)
    protected TextView mText;
    @BindView(R.id.bluetooth_discovery_interval_preference_numberpicker_min)
    protected NumberPicker minutePicker;
    @BindView(R.id.bluetooth_discovery_interval_preference_numberpicker_sec)
    protected NumberPicker secondsPicker;

    public TimePickerPreferenceDialog() {
        this(null);
    }

    public TimePickerPreferenceDialog(String[] displaySeconds) {
        this.displaySeconds = displaySeconds;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        // inject views
        ButterKnife.bind(this, view);

        // Set the textview text
        mText.setText(R.string.pref_bt_discovery_interval_explanation);

        // set the settings for the minute NumberPicker.
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(5);
        minutePicker.setOnLongPressUpdateInterval(100);
        minutePicker.setFormatter(FORMATTER_TWO_DIGITS);
        minutePicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            int minValue = minutePicker.getMinValue();
            int maxValue = minutePicker.getMaxValue();

            if (oldVal == maxValue && newVal == minValue) {
            }
        });

        // set the settings for the seconds number picker.
        secondsPicker.setMinValue(0);
        secondsPicker.setMaxValue(5);
        if (displaySeconds != null)
            secondsPicker.setDisplayedValues(displaySeconds);
        secondsPicker.setOnLongPressUpdateInterval(100);
        secondsPicker.setFormatter(FORMATTER_TWO_DIGITS);
        secondsPicker.setOnValueChangedListener((spinner, oldVal, newVal) -> {
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
            currentSeconds = seconds / 10;

            secondsPicker.setValue(currentSeconds);
            minutePicker.setValue(currentMinutes);
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            currentMinutes = minutePicker.getValue();
            currentSeconds = secondsPicker.getValue();

            int time = minutePicker.getValue() * 60 + secondsPicker.getValue() * 10;

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
