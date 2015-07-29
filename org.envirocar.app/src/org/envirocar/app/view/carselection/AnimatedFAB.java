package org.envirocar.app.view.carselection;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;

import com.gordonwong.materialsheetfab.AnimatedFab;

import org.envirocar.app.R;

/**
 * @author dewall
 */
public class AnimatedFAB extends FloatingActionButton implements AnimatedFab {

    private Interpolator mAnimationInterpolator;

    public AnimatedFAB(Context context) {
        super(context);
    }

    public AnimatedFAB(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AnimatedFAB(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void setupView() {
        mAnimationInterpolator = AnimationUtils.loadInterpolator(getContext(), R.interpolator
                .msf_interpolator);
    }

    @Override
    public void show(float v, float v1) {

    }
//    @Override
//    public void show() {
//        show(0, 0);
//    }
//
//    @Override
//    public void show(float x, float y) {
//        animate().setInterpolator(mAnimationInterpolator)
//                .setDuration(400)
//                .translationX(x)
//                .translationY(y);
//
//        // Only use scale animation if FAB is hidden
//        if (getVisibility() != View.VISIBLE) {
//            // Pivots indicate where the animation begins from
//            float pivotX = getPivotX() + x;
//            float pivotY = getPivotY() + y;
//
//            ScaleAnimation anim;
//            // If pivots are 0, that means the FAB hasn't been drawn yet so just use the
//            // center of the FAB
//            if (pivotX == 0 || pivotY == 0) {
//                anim = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f,
//                        Animation.RELATIVE_TO_SELF, 0.5f);
//            } else {
//                anim = new ScaleAnimation(0, 1, 0, 1, pivotX, pivotY);
//            }
//
//            // Animate FAB expanding
//            anim.setDuration(400);
//            anim.setInterpolator(mAnimationInterpolator);
//            startAnimation(anim);
//        }
//        setVisibility(View.VISIBLE);
//    }
//
//    @Override
//    public void hide() {
//
//        if (getVisibility() == View.VISIBLE) {
//            float pivotX = getPivotX() + getTranslationX();
//            float pivotY = getPivotY() + getTranslationY();
//
//            ScaleAnimation animation = new ScaleAnimation(1, 0, 1, 0, pivotX, pivotY);
//            animation.setDuration(400);
//            animation.setInterpolator(mAnimationInterpolator);
//            startAnimation(animation);
//        }
//        setVisibility(View.INVISIBLE);
//    }


}
