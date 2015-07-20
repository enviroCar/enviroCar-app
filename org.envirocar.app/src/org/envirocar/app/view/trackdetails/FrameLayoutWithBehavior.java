//package android.support.design.widget;
//
//import android.annotation.TargetApi;
//import android.content.Context;
//import android.graphics.Rect;
//import android.os.Build;
//import android.support.design.widget.AppBarLayout;
//import android.support.design.widget.CoordinatorLayout;
//import android.support.design.widget.Snackbar;
//import android.support.v4.view.ViewCompat;
//import android.support.v4.view.ViewPropertyAnimatorListener;
//import android.util.AttributeSet;
//import android.view.View;
//import android.view.animation.Animation;
//import android.view.animation.AnimationUtils;
//import android.widget.FrameLayout;
//
//import org.envirocar.app.R;
//
//import java.lang.reflect.Method;
//import java.util.List;
//
///**
// * Created by Peter on 20.07.2015.
// */
//@CoordinatorLayout.DefaultBehavior(FrameLayoutWithBehavior.Behavior.class)
//public class FrameLayoutWithBehavior extends FrameLayout {
//    public FrameLayoutWithBehavior(final Context context) {
//        super(context);
//    }
//
//    public FrameLayoutWithBehavior(final Context context, final AttributeSet attrs) {
//        super(context, attrs);
//    }
//
//    public FrameLayoutWithBehavior(final Context context, final AttributeSet attrs, final int defStyleAttr) {
//        super(context, attrs, defStyleAttr);
//    }
//
//    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//    public FrameLayoutWithBehavior(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
//        super(context, attrs, defStyleAttr, defStyleRes);
//    }
//
//    public static class Behavior extends android.support.design.widget.CoordinatorLayout.Behavior<FrameLayoutWithBehavior> {
//        private static final boolean SNACKBAR_BEHAVIOR_ENABLED;
//        private Rect mTmpRect;
//        private boolean mIsAnimatingOut;
//        private float mTranslationY;
//
//        public Behavior() {
//        }
//
//        @Override
//        public boolean layoutDependsOn(CoordinatorLayout parent, FrameLayoutWithBehavior child, View dependency) {
//            return SNACKBAR_BEHAVIOR_ENABLED && dependency instanceof Snackbar.SnackbarLayout;
//        }
//
//        @Override
//        public boolean onDependentViewChanged(CoordinatorLayout parent, FrameLayoutWithBehavior child, View dependency) {
//            if (dependency instanceof Snackbar.SnackbarLayout) {
//                this.updateFabTranslationForSnackbar(parent, child, dependency);
//            } else if (dependency instanceof AppBarLayout) {
//                AppBarLayout appBarLayout = (AppBarLayout) dependency;
//                if (this.mTmpRect == null) {
//                    this.mTmpRect = new Rect();
//                }
//
//                Rect rect = this.mTmpRect;
//                ViewGroupUtils.getDescendantRect(parent, dependency, rect);
//
//                try {
//                    Method method = appBarLayout.getClass().getMethod("getMinimumHeightForVisibleOverlappingContent", null);
//                    method.setAccessible(true);
//                    method.invoke(appBarLayout)
//                } catch (NoSuchMethodException e) {
//                    e.printStackTrace();
//                }
//
//                if (rect.bottom <= appBarLayout.getMinimumHeightForVisibleOverlappingContent()) {
//                    if (!this.mIsAnimatingOut && child.getVisibility() == VISIBLE) {
//                        this.animateOut(child);
//                    }
//                } else if (child.getVisibility() != VISIBLE) {
//                    this.animateIn(child);
//                }
//            }
//
//            return false;
//        }
//
//        private void updateFabTranslationForSnackbar(CoordinatorLayout parent, FrameLayoutWithBehavior fab, View snackbar) {
//            float translationY = this.getFabTranslationYForSnackbar(parent, fab);
//            if (translationY != this.mTranslationY) {
//                ViewCompat.animate(fab)
//                        .cancel();
//                if (Math.abs(translationY - this.mTranslationY) == (float) snackbar.getHeight()) {
//                    ViewCompat.animate(fab)
//                            .translationY(translationY)
//                            .setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR)
//                            .setListener((ViewPropertyAnimatorListener) null);
//                } else {
//                    ViewCompat.setTranslationY(fab, translationY);
//                }
//
//                this.mTranslationY = translationY;
//            }
//
//        }
//
//        private float getFabTranslationYForSnackbar(CoordinatorLayout parent, FrameLayoutWithBehavior fab) {
//            float minOffset = 0.0F;
//            List dependencies = parent.getDependencies(fab);
//            int i = 0;
//
//            for (int z = dependencies.size(); i < z; ++i) {
//                View view = (View) dependencies.get(i);
//                if (view instanceof Snackbar.SnackbarLayout && parent.doViewsOverlap(fab, view)) {
//                    minOffset = Math.min(minOffset, ViewCompat.getTranslationY(view) - (float) view.getHeight());
//                }
//            }
//
//            return minOffset;
//        }
//
//        private void animateIn(FrameLayoutWithBehavior button) {
//            button.setVisibility(View.VISIBLE);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
//                ViewCompat.animate(button)
//                        .scaleX(1.0F)
//                        .scaleY(1.0F)
//                        .alpha(1.0F)
//                        .setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR)
//                        .withLayer()
//                        .setListener((ViewPropertyAnimatorListener) null)
//                        .start();
//            } else {
//                Animation anim = android.view.animation.AnimationUtils.loadAnimation(button.getContext(), R.anim.fab_in);
//                anim.setDuration(200L);
//                anim.setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR);
//                button.startAnimation(anim);
//            }
//
//        }
//
//        private void animateOut(final FrameLayoutWithBehavior button) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
//                ViewCompat.animate(button)
//                        .scaleX(0.0F)
//                        .scaleY(0.0F)
//                        .alpha(0.0F)
//                        .setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR)
//                        .withLayer()
//                        .setListener(new ViewPropertyAnimatorListener() {
//                            public void onAnimationStart(View view) {
//                                Behavior.this.mIsAnimatingOut = true;
//                            }
//
//                            public void onAnimationCancel(View view) {
//                                Behavior.this.mIsAnimatingOut = false;
//                            }
//
//                            public void onAnimationEnd(View view) {
//                                Behavior.this.mIsAnimatingOut = false;
//                                view.setVisibility(View.GONE);
//                            }
//                        })
//                        .start();
//            } else {
//                Animation anim = android.view.animation.AnimationUtils.loadAnimation(button.getContext(), R.anim.fab_out);
//                anim.setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR);
//                anim.setDuration(200L);
//                anim.setAnimationListener(new AnimationUtils.AnimationListenerAdapter() {
//                    public void onAnimationStart(Animation animation) {
//                        Behavior.this.mIsAnimatingOut = true;
//                    }
//
//                    public void onAnimationEnd(Animation animation) {
//                        Behavior.this.mIsAnimatingOut = false;
//                        button.setVisibility(View.GONE);
//                    }
//                });
//                button.startAnimation(anim);
//            }
//
//        }
//
//        static {
//            SNACKBAR_BEHAVIOR_ENABLED = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
//        }
//    }
//}
