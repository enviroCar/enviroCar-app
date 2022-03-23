package org.envirocar.app.views.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

public class NestedScrollableHost extends FrameLayout {

    private int touchSlop = 0;
    private float initialX = 0;
    private float initialY = 0;
    private ViewPager2 parentPager;
    private ViewPager2 childPager;

    public NestedScrollableHost(@NonNull Context context) {
        super(context);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public NestedScrollableHost(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public NestedScrollableHost(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public NestedScrollableHost(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        View parentView = (View) getParent();
        while (parentView!=null && !(parentView instanceof ViewPager2))
        {
            parentView = (View) parentView.getParent();
        }
        parentPager = (ViewPager2) parentView;

        if(getChildCount()>0) childPager = (ViewPager2) getChildAt(0);
    }

    private boolean canChildScroll(int orientation, float delta) {
        int direction = (int) -Math.signum(delta);
        switch (orientation)
        {
            case 0:
                return childPager.canScrollHorizontally(direction);
            case 1:
                return childPager.canScrollVertically(direction);
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(parentPager!=null && childPager!=null)
        {
            int orientation = parentPager.getOrientation();
            boolean isHorizontal = orientation==ViewPager2.ORIENTATION_HORIZONTAL;
            if(canChildScroll(orientation,-1f) || canChildScroll(orientation,1f))
            {
                if(ev.getAction()==MotionEvent.ACTION_DOWN)
                {
                    initialX = ev.getX();
                    initialY = ev.getY();
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                else if(ev.getAction()==MotionEvent.ACTION_MOVE)
                {
                    float dx = ev.getX() - initialX;
                    float dy = ev.getY() - initialY;
                    float scaleDx = Math.abs(dx) * ( isHorizontal ? 0.5f : 1f);
                    float scaleDy = Math.abs(dy) * ( isHorizontal ? 1f : 0.5f);
                    if(scaleDx>touchSlop || scaleDy>touchSlop)
                    {
                        if( isHorizontal == (scaleDy>scaleDx) )
                        {
                            getParent().requestDisallowInterceptTouchEvent(false);
                        }
                        else
                        {
                            getParent().requestDisallowInterceptTouchEvent(canChildScroll(orientation,isHorizontal ? dx : dy));
                        }
                    }
                }
            }
        }
        return super.onInterceptTouchEvent(ev);
    }
}
