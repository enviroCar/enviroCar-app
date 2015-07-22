package org.envirocar.app.view.trackdetails;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * @author dewall
 */
public class ScrollAwareBehavior extends FloatingActionButton.Behavior {

    public ScrollAwareBehavior(Context context, AttributeSet attrs) {
        super();
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, FloatingActionButton child, View
            dependency) {
        Log.d("layoutDependsOn,", "es reagiert " + (dependency instanceof NestedScrollView));
        return dependency instanceof NestedScrollView || super.layoutDependsOn(parent, child,
                dependency);
    }

    @Override
    public boolean onStartNestedScroll(final CoordinatorLayout coordinatorLayout, final
    FloatingActionButton child, final View directTargetChild, final View target, final int
                                               nestedScrollAxes) {
        Log.d("onStartNestedScroll", "es wird was machen " + (target instanceof NestedScrollView));

        // Ensure we react to vertical scrolling
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL
                || super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target,
                nestedScrollAxes);
    }

    @Override
    public void onNestedScrollAccepted(CoordinatorLayout coordinatorLayout, FloatingActionButton child, View directTargetChild, View target, int nestedScrollAxes) {
        Log.d("onNEstedScrollAccepted", "accepted");
        super.onNestedScrollAccepted(coordinatorLayout, child, directTargetChild, target,
                nestedScrollAxes);
    }

    @Override
    public boolean blocksInteractionBelow(CoordinatorLayout parent, FloatingActionButton child) {
        Log.d("blocksInteractionsBelow", "accepted");
        return false;
    }

    @Override
    public void onNestedScroll(final CoordinatorLayout coordinatorLayout, final
    FloatingActionButton child,
                               final View target, final int dxConsumed, final int dyConsumed,
                               final int dxUnconsumed, final int dyUnconsumed) {
        Log.d("es tut was", "es wird was machen " + (target instanceof NestedScrollView));
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed);
        if (dyConsumed > 0 && child.getVisibility() == View.VISIBLE) {
            // User scrolled down and the FAB is currently visible -> hide the FAB
            child.hide();
        } else if (dyConsumed < 0 && child.getVisibility() != View.VISIBLE) {
            // User scrolled up and the FAB is currently not visible -> show the FAB
            child.show();
        }
    }

}
