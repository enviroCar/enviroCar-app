package org.envirocar.app.views.settings.custom;

import android.content.Context;
import android.util.AttributeSet;
import androidx.preference.DialogPreference;

public class TrackchunkUploadPreference extends DialogPreference implements TimePickerPreferenceDialog.TimePickerPreference {

    public TrackchunkUploadPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public int getTime() {
        return 0;
    }

    @Override
    public void setTime(int time) {

    }
}
