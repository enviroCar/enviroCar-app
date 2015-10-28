package org.envirocar.app.view.preferences.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import org.envirocar.app.R;
import org.envirocar.core.logging.Logger;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by dewall
 */
public class TimerInputView extends LinearLayout {
    private static final Logger LOGGER = Logger.getLogger(TimerInputView.class);

    private static final int KEY_NUMBERS = 19;

    private final OnClickListener mOnClickListener = v -> {

    };

    @InjectView(R.id.preference_layout_timer_input_view_firstrow)
    protected View mButtonRow1;
    @InjectView(R.id.preference_layout_timer_input_view_secondrow)
    protected View mButtonRow2;
    @InjectView(R.id.preference_layout_timer_input_view_thirdrow)
    protected View mButtonRow3;
    @InjectView(R.id.preference_layout_timer_input_view_fourthrow)
    protected View mButtonRow4;

    protected final Button[] mNumberButtons = new Button[10];

    /**
     * Constructor.
     *
     * @param context
     */
    public TimerInputView(Context context) {
        super(context, null);
    }

    /**
     * Constructor.
     *
     * @param context
     * @param attrs
     */
    public TimerInputView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.preference_layout_timer_input_view, this);

        // Inject all views
        ButterKnife.inject(this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        ((Button) mButtonRow4.findViewById(R.id
                .preference_layout_timer_input_view_keyrow_left)).setEnabled(false);
        mNumberButtons[0] = (Button) mButtonRow4.findViewById(R.id
                .preference_layout_timer_input_view_keyrow_mid);
        ((Button) mButtonRow4.findViewById(R.id
                .preference_layout_timer_input_view_keyrow_right)).setEnabled(false);

        mNumberButtons[1] = (Button) mButtonRow1.findViewById(R.id
                .preference_layout_timer_input_view_keyrow_left);
        mNumberButtons[2] = (Button) mButtonRow1.findViewById(R.id
                .preference_layout_timer_input_view_keyrow_mid);
        mNumberButtons[3] = (Button) mButtonRow1.findViewById(R.id
                .preference_layout_timer_input_view_keyrow_right);

        mNumberButtons[4] = (Button) mButtonRow2.findViewById(R.id
                .preference_layout_timer_input_view_keyrow_left);
        mNumberButtons[5] = (Button) mButtonRow2.findViewById(R.id
                .preference_layout_timer_input_view_keyrow_mid);
        mNumberButtons[6] = (Button) mButtonRow2.findViewById(R.id
                .preference_layout_timer_input_view_keyrow_right);

        mNumberButtons[7] = (Button) mButtonRow3.findViewById(R.id
                .preference_layout_timer_input_view_keyrow_left);
        mNumberButtons[8] = (Button) mButtonRow3.findViewById(R.id
                .preference_layout_timer_input_view_keyrow_mid);
        mNumberButtons[9] = (Button) mButtonRow3.findViewById(R.id
                .preference_layout_timer_input_view_keyrow_right);

        for (int i = 0; i < 10; i++) {
            mNumberButtons[i].setOnClickListener(mOnClickListener);
            mNumberButtons[i].setText("" + i);
            mNumberButtons[i].setTextColor(getResources().getColor(R.color.blue_light_cario));
//            mNumberButtons[i].setTag(KEY_NUMBERS, i);
        }
    }
}
