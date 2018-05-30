/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.app.view.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import org.envirocar.app.R;
import org.envirocar.core.logging.Logger;

import butterknife.ButterKnife;
import butterknife.BindView;

/**
 * Created by dewall
 */
public class TimerInputView extends LinearLayout {
    private static final Logger LOGGER = Logger.getLogger(TimerInputView.class);

    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            String t = v.getTag().toString();
        }
    };

    @BindView(R.id.preference_layout_timer_input_view_firstrow)
    protected View mButtonRow1;
    @BindView(R.id.preference_layout_timer_input_view_secondrow)
    protected View mButtonRow2;
    @BindView(R.id.preference_layout_timer_input_view_thirdrow)
    protected View mButtonRow3;
    @BindView(R.id.preference_layout_timer_input_view_fourthrow)
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
        ButterKnife.bind(this);
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
            mNumberButtons[i].setTag(i);
        }
    }
}
