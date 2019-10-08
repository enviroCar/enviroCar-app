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
