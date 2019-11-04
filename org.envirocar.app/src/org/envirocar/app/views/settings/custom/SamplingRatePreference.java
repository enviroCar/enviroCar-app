package org.envirocar.app.views.settings.custom;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import org.envirocar.app.R;
import org.envirocar.app.handler.ApplicationSettings;

/**
 * @author dewall
 */
public class SamplingRatePreference extends DialogPreference {

    public SamplingRatePreference(Context context) {
        super(context);
    }

    public SamplingRatePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public int getSamplingRate() {
        return getPersistedInt(ApplicationSettings.DEFAULT_SAMPLING_RATE);
    }

    public void setSamplingRate(int samplingRate) {
        persistInt(samplingRate);
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.preference_sampling_rate_dialog;
    }
}
