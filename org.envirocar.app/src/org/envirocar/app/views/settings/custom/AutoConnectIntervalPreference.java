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

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;

import org.envirocar.app.R;

/**
 * @author dewall
 */
public class AutoConnectIntervalPreference extends DialogPreference implements TimePickerPreferenceDialog.TimePickerPreference {

    public AutoConnectIntervalPreference(Context context) {
        this(context, null);
    }

    public AutoConnectIntervalPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        super.onSetInitialValue(defaultValue);
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.preference_timepicker_dialog;
    }

    @Override
    public int getTime() {
        return getPersistedInt(10);
    }

    @Override
    public void setTime(int time) {
        persistInt(time);
    }
}
