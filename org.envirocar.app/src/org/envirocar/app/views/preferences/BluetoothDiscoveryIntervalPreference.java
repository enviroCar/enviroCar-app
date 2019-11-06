/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.preferences;


import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import org.envirocar.app.BaseApplication;
import org.envirocar.app.R;
import org.envirocar.app.handler.ApplicationSettings;
import org.envirocar.core.logging.Logger;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author dewall
 */
public class BluetoothDiscoveryIntervalPreference extends DialogPreference {
    private static final Logger LOGGER = Logger.getLogger(BluetoothDiscoveryIntervalPreference.class);

    private static final String[] DISPLAY_SECONDS = {"00", "10", "20", "30", "40", "50"};

    /**
     *
     */
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

    /**
     * Constructor.
     *
     * @param context the context of the current scope.
     * @param attrs   the attriute set
     */
    public BluetoothDiscoveryIntervalPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public int getInterval(){
        return getPersistedInt(10);
    }

    public void setInterval(int interval){
        persistInt(interval);
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.bluetooth_discovery_interval_preference;
    }

    public final class IntervalPreferenceFragment extends PreferenceDialogFragmentCompat {

        // The seconds and minutes the timepicker have to show
        private int mCurrentSeconds;
        private int mCurrentMinutes;

        @BindView(R.id.default_header_toolbar)
        protected Toolbar mToolbar;
        @BindView(R.id.bluetooth_discovery_interval_preference_numberpicker_text)
        protected TextView mText;
        @BindView(R.id.bluetooth_discovery_interval_preference_numberpicker_min)
        protected NumberPicker mMinutePicker;
        @BindView(R.id.bluetooth_discovery_interval_preference_numberpicker_sec)
        protected NumberPicker mSecondsPicker;

        @Override
        protected void onBindDialogView(View view) {
            LOGGER.info("onBindDialogView()");
            super.onBindDialogView(view);

            // Inject all views
            ButterKnife.bind(this, view);

            // Toolbar settings.
            mToolbar.setTitle(R.string.pref_bt_discovery_interval_title);
            mToolbar.setNavigationIcon(R.drawable.ic_timer_white_24dp);
            mToolbar.setTitleTextColor(getActivity().getResources().getColor(R.color.white_cario));

            // Set the textview text
            mText.setText(R.string.pref_bt_discovery_interval_explanation);

            onSetInitialValue(true, null);

            // set the settings for the minute NumberPicker.
            mMinutePicker.setMinValue(0);
            mMinutePicker.setMaxValue(59);
            mMinutePicker.setOnLongPressUpdateInterval(100);
            mMinutePicker.setFormatter(FORMATTER_TWO_DIGITS);
            mMinutePicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
                int minValue = mMinutePicker.getMinValue();
                int maxValue = mMinutePicker.getMaxValue();

                if (oldVal == maxValue && newVal == minValue) {
                }
            });

            // set the settings for the seconds number picker.
            mSecondsPicker.setMinValue(0);
            mSecondsPicker.setMaxValue(5);
            mSecondsPicker.setDisplayedValues(DISPLAY_SECONDS);
            mSecondsPicker.setOnLongPressUpdateInterval(100);
            mSecondsPicker.setFormatter(FORMATTER_TWO_DIGITS);
            mSecondsPicker.setOnValueChangedListener((spinner, oldVal, newVal) -> {
                int minValue = mSecondsPicker.getMinValue();
                int maxValue = mSecondsPicker.getMaxValue();
                if (oldVal == maxValue && newVal == minValue) {
                    int newMinute = mMinutePicker.getValue() + 1;
                    mMinutePicker.setValue(newMinute);
                } else if (oldVal == minValue && newVal == maxValue) {
                    int newMinute = mMinutePicker.getValue() - 1;
                    mMinutePicker.setValue(newMinute);
                }
            });

            mSecondsPicker.setValue(mCurrentSeconds);
            mMinutePicker.setValue(mCurrentMinutes);
        }

        protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
            LOGGER.info("onSetInitialValue");

            int time = -1;
            // First either get the persisted value or the value stored in the shared preferences.
            if (restorePersistedValue) {

                time = getPersistedInt(-1);
            }

            if (time == -1) {
                time = ApplicationSettings.getDiscoveryIntervalObservable(this.getActivity()).blockingFirst();
            }

            if (time != -1) {
                int seconds = time % 60;
                int minutes = (time - seconds) / 60;

                mCurrentMinutes = minutes;
                mCurrentSeconds = seconds / 10;
            }
        }


        @Override
        protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
            super.onPrepareDialogBuilder(builder);
            // Remove default preference title.
            builder.setTitle(null);
        }

        @Override
        public void onDialogClosed(boolean positiveResult) {

            if (positiveResult) {
                mCurrentMinutes = mMinutePicker.getValue();
                mCurrentSeconds = mSecondsPicker.getValue();

                int time = mMinutePicker.getValue() * 60 + mSecondsPicker.getValue() * 10;
                persistInt(time);

                // set the discovery interval
                ApplicationSettings.setDiscoveryInterval(getActivity(), time);
            }
        }

        private BluetoothDiscoveryIntervalPreference getCustomizablePreference() {
            return (BluetoothDiscoveryIntervalPreference) getPreference();
        }
    }
}
