package org.envirocar.app.view.preferences;

import android.content.Context;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class EditPreferenceWithSummary extends EditTextPreference {

    private static final String NAMESPACE = "http://envirocar.org";

    public EditPreferenceWithSummary(Context context) {
        super(context);
    }

    public EditPreferenceWithSummary(Context context, AttributeSet attrs) {
        super(context, attrs);
        initParameters(attrs);
    }

    public EditPreferenceWithSummary(Context context, AttributeSet attrs,
                                     int defStyle) {
        super(context, attrs, defStyle);
        initParameters(attrs);
    }

    private void initParameters(AttributeSet attrs) {
        int minValue = Integer.MIN_VALUE;
        int maxValue = Integer.MAX_VALUE;
        try {
            minValue = Math.max(Integer.MIN_VALUE, Integer.parseInt(attrs.getAttributeValue(NAMESPACE, "min")));
            maxValue = Math.min(Integer.MAX_VALUE, Integer.parseInt(attrs.getAttributeValue(NAMESPACE, "max")));
        }
        catch (NumberFormatException e) {
        }

        this.setOnPreferenceChangeListener(new MinMaxValueChangeListener(minValue, maxValue));
    }


    @Override
    public CharSequence getSummary() {
        CharSequence originalSummary = super.getSummary();
        if (originalSummary != null && originalSummary.length() > 0) {
            return String.format(originalSummary.toString(), getPersistedString(getKey()));
        }
        return getPersistedString(getKey());
    }

    public static class MinMaxValueChangeListener implements OnPreferenceChangeListener {

        private int minValue = Integer.MIN_VALUE;
        private int maxValue = Integer.MAX_VALUE;


        public MinMaxValueChangeListener(int minValue, int maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String textValue;
            try {
                int val = Integer.parseInt(newValue.toString());
                val = Math.min(Math.max(minValue, val), maxValue);
                textValue = Integer.toString(val);
            }
            catch (NumberFormatException e) {
                textValue = newValue.toString();
            }

            PreferenceManager.getDefaultSharedPreferences(preference.getContext()).edit().
                    putString(preference.getKey(), textValue).commit();
            return true;
        }

    }


}
