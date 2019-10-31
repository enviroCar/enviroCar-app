package org.envirocar.app.views.settings.custom;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceFragmentCompat;

import org.envirocar.app.R;

/**
 * @author dewall
 */
public class AutoconnectIntervalPreference extends DialogPreference {

    public AutoconnectIntervalPreference(Context context) {
        this(context, null);
    }

    public AutoconnectIntervalPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public int getInterval(){
        return getPersistedInt(10);
    }

    public void setInterval(int interval){
        persistInt(interval);
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        super.onSetInitialValue(defaultValue);
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.bluetooth_discovery_interval_preference;
    }
}
