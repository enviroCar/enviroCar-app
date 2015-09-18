package org.envirocar.app.view.utils;


import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import rx.functions.Action0;

/**
 * @author dewall
 */
public class ECAnimationUtils {

    public static void animateShowView(Context context, View view, int animResource) {
        Animation animation = AnimationUtils.loadAnimation(context, animResource);
        view.setVisibility(View.VISIBLE);
        view.startAnimation(animation);
    }

    /**
     * Applies an animation on the given view in order hide it.
     *
     * @param context      the context of the current scope
     * @param view         the view to apply the animation on.
     * @param animResource the animation resource.
     */
    public static void animateHideView(Context context, View view, int animResource) {
        animateHideView(context, view, animResource, null);
    }

    /**
     * Applies an animation on the given view in order hide it.
     *
     * @param context      the context of the current scope
     * @param view         the view to apply the animation on.
     * @param animResource the animation resource.
     * @param action       the action that should happen when the animation is finished.
     */
    public static void animateHideView(Context context, View view, int animResource, Action0
            action) {
        Animation animation = AnimationUtils.loadAnimation(context, animResource);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // nothing to do..
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
                if (action != null) {
                    action.call();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // nothing to do..
            }
        });
        view.startAnimation(animation);
    }

    /**
     * Expands the view to a specific height.
     *
     * @param view  the view to expand to a given height.
     * @param height
     */
    public static void expandView(View view, int height) {
        ValueAnimator animator = ValueAnimator.ofInt(view.getLayoutParams().height, height);

        animator.addUpdateListener(animation -> {
            int value = (Integer) animation.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = value;
            view.setLayoutParams(layoutParams);
        });

        animator.setDuration(600);
        animator.start();
    }

    /**
     * Compresses the view down to a specific height.
     *
     * @param view   the view to compress to a different height.
     * @param height the height to the view should have after compression.
     */
    public static void compressView(View view, int height) {
        // Create an ValueAnimator for the height-based animation
        ValueAnimator animator = ValueAnimator.ofInt(view.getLayoutParams().height, height);

        animator.addUpdateListener(animation -> {
            int value = (Integer) animation.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = value;
            view.setLayoutParams(layoutParams);
        });

        animator.setDuration(600);
        animator.start();
    }
}
