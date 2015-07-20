package org.envirocar.app.view.trackdetails;

import android.content.Context;
import android.graphics.Rect;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

import com.github.clans.fab.FloatingActionMenu;

/**
 * @author dewall
 */
public class FloatingActionMenuBehavior extends CoordinatorLayout.Behavior {

    private float mTranslationY;
    Rect mTmpRect;

    public FloatingActionMenuBehavior(Context context, AttributeSet attrs) {
        super();
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return dependency instanceof AppBarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {

        if (child instanceof FloatingActionMenu && dependency instanceof AppBarLayout) {
            FloatingActionMenu menu = (FloatingActionMenu) child;

            ViewCompat.setTranslationY(child, child.getHeight() / 2 - menu.getMenuIconView()
                    .getHeight()*2);
        }


        return false;
    }
}
