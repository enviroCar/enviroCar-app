package org.envirocar.app.views.settings.custom;

import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import org.envirocar.app.R;
import org.envirocar.app.handler.ApplicationSettings;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author dewall
 */
public class AutoConnectIntervalPreferenceDialog extends PreferenceDialogFragmentCompat {

    public static AutoConnectIntervalPreferenceDialog newInstance(String key) {
        final AutoConnectIntervalPreferenceDialog fragment = new AutoConnectIntervalPreferenceDialog();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    private static final String[] DISPLAY_SECONDS = {"00", "10", "20", "30", "40", "50"};

    private static final NumberPicker.Formatter FORMATTER_TWO_DIGITS = new NumberPicker.Formatter() {
        final StringBuilder mBuilder = new StringBuilder();
        final java.util.Formatter mFormatter = new java.util.Formatter(mBuilder, java.util.Locale
                .US);
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

    @BindView(R.id.bluetooth_discovery_interval_preference_numberpicker_text)
    protected TextView mText;
    @BindView(R.id.bluetooth_discovery_interval_preference_numberpicker_min)
    protected NumberPicker minutePicker;
    @BindView(R.id.bluetooth_discovery_interval_preference_numberpicker_sec)
    protected NumberPicker secondsPicker;

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        // inject views
        ButterKnife.bind(this, view);

        // Set the textview text
        mText.setText(R.string.pref_bt_discovery_interval_explanation);

        // set the settings for the minute NumberPicker.
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
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
        secondsPicker.setDisplayedValues(DISPLAY_SECONDS);
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

        // setting the initial value
        DialogPreference preference = getPreference();
        if (preference instanceof AutoconnectIntervalPreference) {
            int time = ((AutoconnectIntervalPreference) preference).getInterval();

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
            if (preference instanceof AutoconnectIntervalPreference) {
                AutoconnectIntervalPreference intervalPref = ((AutoconnectIntervalPreference) preference);
                intervalPref.setInterval(time);
            }

            // set the discovery interval
            ApplicationSettings.setDiscoveryInterval(getActivity(), time);
        }
    }
}
