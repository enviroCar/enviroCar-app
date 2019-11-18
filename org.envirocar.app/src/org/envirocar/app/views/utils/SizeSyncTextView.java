/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
package org.envirocar.app.views.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * @author dewall
 */
public class SizeSyncTextView extends AppCompatTextView {
    public interface OnTextSizeChangedListener {
        void onTextSizeChanged(SizeSyncTextView view, float size);
    }

    private OnTextSizeChangedListener listener;
    private float lastTextSize;

    public SizeSyncTextView(Context context) {
        super(context);
    }

    public SizeSyncTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SizeSyncTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (lastTextSize != getTextSize() && getTextSize() < lastTextSize) {
            lastTextSize = getTextSize();
            if (listener != null) {
                listener.onTextSizeChanged(this, lastTextSize);
            }
        }
    }

    public void setOnTextSizeChangedListener(OnTextSizeChangedListener onTextSizeChangedListener){
        this.listener = onTextSizeChangedListener;
    }
}
