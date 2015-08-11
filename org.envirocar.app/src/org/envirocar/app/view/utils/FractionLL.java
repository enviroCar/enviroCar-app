package org.envirocar.app.view.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.WindowManager;
import android.widget.LinearLayout;

/**
 * @author dewall
 */
public class FractionLL extends LinearLayout {

    public FractionLL(Context context) {
        super(context);
    }

    public FractionLL(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public float getYFraction(){
        final WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        int height = wm.getDefaultDisplay().getHeight();
        return (height == 0) ? 0 : getY() / (float) height;
    }

    public void setYFraction(float yFraction){
        final WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        int height = wm.getDefaultDisplay().getHeight();
        setY((height > 0) ? (yFraction * height) : 0);
    }

    public float getXFraction() {
        final int width = getWidth();
        if (width != 0) return getX() / getWidth();
        else return getX();
    }

    public void setXFraction(float xFraction) {
        final int width = getWidth();
        float newWidth = (width > 0) ? (xFraction * width) : -9999;
        setX(newWidth);
    }
}