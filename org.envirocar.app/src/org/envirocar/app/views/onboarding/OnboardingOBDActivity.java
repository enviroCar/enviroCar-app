package org.envirocar.app.views.onboarding;

import android.content.Intent;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.transition.TransitionManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.envirocar.app.R;
import org.envirocar.core.logging.Logger;

import butterknife.BindView;
import butterknife.ButterKnife;

public class OnboardingOBDActivity extends AppCompatActivity {
    private static final Logger LOG = Logger.getLogger(OnboardingOBDActivity.class);

    @BindView(R.id.donePage1)
    protected TextView doneP1;
    @BindView(R.id.donePage2)
    protected TextView doneP2;
    @BindView(R.id.skipPage1)
    protected TextView skipP1;
    @BindView(R.id.skipPage2)
    protected TextView skipP2;
    @BindView(R.id.obdTroubleshoot)
    protected TextView obdTroubleshoot;
    @BindView(R.id.obdLink)
    protected TextView obdLink;
    @BindView(R.id.page1)
    protected ConstraintLayout page1;
    @BindView(R.id.page2)
    protected ConstraintLayout page2;
    @BindView(R.id.page3)
    protected ConstraintLayout page3;
    @BindView(R.id.obd_pages)
    protected ConstraintLayout pages;
    @BindView(R.id.continueButton)
    protected Button continueButton;
    @BindView(R.id.logo)
    protected ImageView animatedcheck;

    protected int whichPage = 0;
    AnimatedVectorDrawable frameAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.onboarding_obd);
        ButterKnife.bind(this);

        animatedcheck.setBackgroundResource(R.drawable.animated_check);
        frameAnimation = (AnimatedVectorDrawable) animatedcheck.getBackground();

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        obdLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("https://www.amazon.de/Cartrend-80290-Bluetooth-Controller-Smartphones/dp/B00CCZJPCU/ref=sr_1_2?ie=UTF8&qid=1513252484&sr=8-2&keywords=cartrend+obd2");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                }
        });

        obdTroubleshoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("https://www.hum.com/port/");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        skipP1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        skipP2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        doneP1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (whichPage == 0) {
                    whichPage = 1;
                    TransitionManager.beginDelayedTransition(pages);
                    page1.setVisibility(View.GONE);
                    page2.setVisibility(View.VISIBLE);
                    page3.setVisibility(View.GONE);
                }
            }
        });

        doneP2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (whichPage == 1) {
                    whichPage = 2;
                    TransitionManager.beginDelayedTransition(page2);
                    page1.setVisibility(View.GONE);
                    page2.setVisibility(View.GONE);
                    page3.setVisibility(View.VISIBLE);
                    frameAnimation.start();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        LOG.info("whichPage: " + whichPage);

        if (frameAnimation.isRunning()) {
            frameAnimation.stop();
        }
        if (whichPage == 0) {
            super.onBackPressed();
        } else if (whichPage == 1) {
            whichPage =0;
            TransitionManager.beginDelayedTransition(pages);
            page1.setVisibility(View.VISIBLE);
            page2.setVisibility(View.GONE);
            page3.setVisibility(View.GONE);
        } else if (whichPage == 2) {
            whichPage =1;
            TransitionManager.beginDelayedTransition(pages);
            page1.setVisibility(View.GONE);
            page2.setVisibility(View.VISIBLE);
            page3.setVisibility(View.GONE);
        }
    }
}
