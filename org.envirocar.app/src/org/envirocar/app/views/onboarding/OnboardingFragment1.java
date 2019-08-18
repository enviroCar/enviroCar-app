package org.envirocar.app.views.onboarding;


import android.os.Bundle;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.envirocar.app.R;
import org.envirocar.core.logging.Logger;


import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 */
public class OnboardingFragment1 extends Fragment {
    private static final Logger LOG = Logger.getLogger(OnboardingFragment1.class);
    public static final Long animationStart = 1200L, animationDuration = 500L;

    public OnboardingFragment1() {
        // Required empty public constructor
    }

    @BindView(R.id.headerTV)
    protected TextView headerTV;

    @BindView(R.id.header_subTV)
    protected TextView headerSubTV;

    @BindView(R.id.subTV)
    protected TextView subTV;

    @BindView(R.id.ob_img1)
    protected ImageView background;

    @BindView(R.id.logo)
    protected ImageView logo;

    @BindView(R.id.ob_1)
    protected ConstraintLayout layout;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.onboarding_1,container,false);
        ButterKnife.bind(this, view);
        LOG.info("Begin Transitions");
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(layout, new Fade().setDuration(animationDuration));
                background.setVisibility(View.VISIBLE);
                logo.setVisibility(View.GONE);
                subTV.setVisibility(View.INVISIBLE);
            }
        }, animationStart);

        final Handler handler1 = new Handler();
        handler1.postDelayed(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(layout, new Fade().setDuration(500));
                headerTV.setVisibility(View.VISIBLE);
                headerSubTV.setVisibility(View.VISIBLE);
                LOG.info("End Transitions");
            }
        }, animationStart+animationDuration);

        return view;
    }

    public void setBackgroundVisibility(float alpha) {
        background.setAlpha(alpha);
    }
}
