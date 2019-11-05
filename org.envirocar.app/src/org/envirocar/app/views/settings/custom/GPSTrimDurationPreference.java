package org.envirocar.app.views.settings.custom;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import org.envirocar.app.R;
import org.envirocar.app.handler.ApplicationSettings;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author dewall
 */
public class GPSTrimDurationPreference extends DialogPreference {

    public GPSTrimDurationPreference(Context context) {
        super(context);
    }

    public GPSTrimDurationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public int getTrimDuration() {
        return getPersistedInt(ApplicationSettings.DEFAULT_TRACK_TRIM_DURATION);
    }

    public void setTrimDuration(int duration) {
        getPersistedInt(duration);
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.preference_trim_duration_dialog;
    }

    public static class GPSTrimDurationPreferenceDialog extends PreferenceDialogFragmentCompat {

        private static final int SECONDS_MIN_VALUE = 0;
        private static final int SECONDS_MAX_VALUE = 59;
        private static final int MINUTES_MIN_VALUE = 0;
        private static final int MINUTES_MAX_VALUE = 5;

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

        public static GPSTrimDurationPreferenceDialog newInstance(String key) {
            final GPSTrimDurationPreferenceDialog fragment = new GPSTrimDurationPreferenceDialog();
            final Bundle b = new Bundle(1);
            b.putString(ARG_KEY, key);
            fragment.setArguments(b);
            return fragment;
        }

        @BindView(R.id.preference_trim_duration_picker_minutes)
        protected NumberPicker minutePicker;
        @BindView(R.id.preference_trim_duration_picker_seconds)
        protected NumberPicker secondsPicker;

        private int currentSeconds;
        private int currentMinutes;

        @Override
        protected void onBindDialogView(View view) {
            super.onBindDialogView(view);

            // inject views
            ButterKnife.bind(this, view);

            // set min/max values
            this.secondsPicker.setMinValue(0);
            this.secondsPicker.setMaxValue(59);
            this.secondsPicker.setOnLongPressUpdateInterval(100);
            this.secondsPicker.setFormatter(FORMATTER_TWO_DIGITS);
            this.secondsPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
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

            this.minutePicker.setMinValue(0);
            this.minutePicker.setMaxValue(59);
            this.minutePicker.setOnLongPressUpdateInterval(100);
            this.minutePicker.setFormatter(FORMATTER_TWO_DIGITS);

            // setting the initial value
            androidx.preference.DialogPreference preference = getPreference();
            if (preference instanceof GPSTrimDurationPreference) {
                int time = ((GPSTrimDurationPreference) preference).getTrimDuration();

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
                if (preference instanceof GPSTrimDurationPreference) {
                    GPSTrimDurationPreference intervalPref = ((GPSTrimDurationPreference) preference);
                    intervalPref.setTrimDuration(time);
                }

                // set the discovery interval
                ApplicationSettings.setDiscoveryInterval(getActivity(), time);
            }
        }
    }
}
