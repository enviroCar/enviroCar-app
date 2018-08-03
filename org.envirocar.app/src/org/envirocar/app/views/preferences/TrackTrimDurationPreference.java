package org.envirocar.app.views.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.DialogPreference;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

import org.envirocar.app.R;
import org.envirocar.app.handler.PreferenceConstants;
import org.envirocar.app.main.BaseApplication;
import org.envirocar.core.logging.Logger;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TrackTrimDurationPreference extends DialogPreference {
    private static final Logger LOGGER = Logger.getLogger(BluetoothDiscoveryIntervalPreference
            .class);

    /**
     *
     */
    private static final NumberPicker.Formatter FORMATTER_TWO_DIGITS = new NumberPicker.Formatter
            () {
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

    @BindView(R.id.default_header_toolbar)
    protected Toolbar mToolbar;
    @BindView(R.id.bluetooth_discovery_interval_preference_numberpicker_min)
    protected NumberPicker mMinutePicker;
    @BindView(R.id.bluetooth_discovery_interval_preference_numberpicker_sec)
    protected NumberPicker mSecondsPicker;

    // The seconds and minutes the timepicker have to show
    private int mCurrentSeconds;
    private int mCurrentMinutes;

    /**
     * Constructor.
     *
     * @param context the context of the current scope.
     * @param attrs   the attriute set
     */
    public TrackTrimDurationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Inject ourselves.
        BaseApplication.get(context).getBaseApplicationComponent().inject(this);

        // Set the layout of the dialog to show.
        setDialogLayoutResource(R.layout.bluetooth_discovery_interval_preference);
    }

    @Override
    protected void onBindDialogView(View view) {
        LOGGER.info("onBindDialogView()");
        super.onBindDialogView(view);

        // Inject all views
        ButterKnife.bind(this, view);

        // Toolbar settings.
        mToolbar.setTitle(R.string.pref_track_trim_duration_title);
        mToolbar.setNavigationIcon(R.drawable.ic_timer_white_24dp);
        mToolbar.setTitleTextColor(getContext().getResources().getColor(R.color.white_cario));
        

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
        mSecondsPicker.setMaxValue(59);
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

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        LOGGER.info("onSetInitialValue");
        super.onSetInitialValue(restorePersistedValue, defaultValue);

        int time = -1;
        // First either get the persisted value or the value stored in the shared preferences.
        if (restorePersistedValue) {
            time = getPersistedInt(-1);
        }

        if(time == -1){
            time = getSharedPreferences().getInt(PreferenceConstants
                    .PREF_TRACK_CUT_DURATION, PreferenceConstants
                    .DEFAULT_TRACK_TRIM_DURATION);
        }

        if (time != -1){
            int seconds = time % 60;
            int minutes = (time - seconds) / 60;

            mCurrentMinutes = minutes;
            mCurrentSeconds = seconds;
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            mCurrentMinutes = mMinutePicker.getValue();
            mCurrentSeconds = mSecondsPicker.getValue();

            int time = mMinutePicker.getValue() * 60 + mSecondsPicker.getValue();
            persistInt(time);

            getSharedPreferences().edit()
                    .putInt(PreferenceConstants.PREF_TRACK_CUT_DURATION, time)
                    .apply();
        }
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        // Remove default preference title.
        builder.setTitle(null);
    }

}

