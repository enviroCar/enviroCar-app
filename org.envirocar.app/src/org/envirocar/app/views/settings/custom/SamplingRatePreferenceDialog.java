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
public class SamplingRatePreferenceDialog extends PreferenceDialogFragmentCompat {

    private static final int MIN_VALUE = 2;
    private static final int MAX_VALUE = 30;

    public static SamplingRatePreferenceDialog newInstance(String key) {
        final SamplingRatePreferenceDialog fragment = new SamplingRatePreferenceDialog();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @BindView(R.id.preference_sampling_rate_dialog_text)
    protected TextView textView;
    @BindView(R.id.preference_sampling_rate_dialog_picker)
    protected NumberPicker numberPicker;


    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        // inject views
        ButterKnife.bind(this, view);

        // set min/max values
        this.numberPicker.setMinValue(MIN_VALUE);
        this.numberPicker.setMaxValue(MAX_VALUE);

        DialogPreference preference = getPreference();
        if (preference instanceof SamplingRatePreference){
            int value = ((SamplingRatePreference) preference).getSamplingRate();
            numberPicker.setValue(value);
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            int currentSeconds = numberPicker.getValue();

            DialogPreference preference = getPreference();
            if (preference instanceof SamplingRatePreference) {
                ((SamplingRatePreference) preference).setSamplingRate(currentSeconds);
            }

            ApplicationSettings.setSamplingRate(getActivity(), currentSeconds);
        }
    }
}
