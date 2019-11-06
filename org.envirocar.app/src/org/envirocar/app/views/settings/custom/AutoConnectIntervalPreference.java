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
