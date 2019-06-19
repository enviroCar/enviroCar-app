package org.envirocar.app.views.onboarding;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Constraints;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.animation.ArgbEvaluator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.ogaclejapan.smarttablayout.SmartTabLayout;

import org.envirocar.app.R;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.main.BaseMainActivityBottomBar;
import org.envirocar.app.views.dashboard.DashBoardFragment;

import butterknife.BindView;
import butterknife.ButterKnife;

public class OnboardingActivity extends AppCompatActivity {

    public static final String ONBOARDING_COMPLETE = "Onboarding_Complete";

    @BindView(R.id.onboarding_viewpager)
    protected ViewPager viewPager;

    @BindView(R.id.onboarding_viewpagertab)
    protected SmartTabLayout smartTabLayout;

    @BindView(R.id.onboarding_next)
    protected Button nextButton;

    @BindView(R.id.onboarding_skip)
    protected Button skipButton;

    @BindView(R.id.onboarding_layout_basic)
    protected ConstraintLayout onBoardingLayout;

    protected int[] colorList;
    protected OBPageAdapter obPageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.onboarding_basic);
        ButterKnife.bind(this);
        int color0 = Color.WHITE;
        int color1 = Color.parseColor("#EFF9FD");
        int color2 = Color.parseColor("#E8F2F9");
        int color3 = Color.WHITE;

        colorList = new int[]{color0, color1, color2, color3};
        obPageAdapter = new OBPageAdapter(getSupportFragmentManager());

        viewPager.setOffscreenPageLimit(3);
        viewPager.setAdapter(obPageAdapter);
        smartTabLayout.setViewPager(viewPager);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(viewPager.getCurrentItem() == 2){
                    //finishOnboarding();
                }
                else{
                    viewPager.setCurrentItem(viewPager.getCurrentItem()+1,true);
                }
            }
        });

        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishOnboarding();
            }
        });

        smartTabLayout.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                ArgbEvaluator evaluator = new ArgbEvaluator();
                int colorUpdate;
                if(position!=3){
                    colorUpdate = (Integer) evaluator.evaluate(positionOffset, colorList[position], colorList[position + 1]);
                }
                else
                {
                    colorUpdate = (Integer) evaluator.evaluate(positionOffset, colorList[position], colorList[position]);
                }
                viewPager.setBackgroundColor(colorUpdate);
            }

            @Override
            public void onPageSelected(int position){
                if(position == 3){
                    skipButton.setVisibility(View.GONE);
                    nextButton.setVisibility(View.GONE);
                }
                else
                {
                    skipButton.setVisibility(View.VISIBLE);
                    nextButton.setVisibility(View.VISIBLE);
                }

                switch (position) {
                    case 0:
                        viewPager.setBackgroundColor(color0);
                        break;
                    case 1:
                        viewPager.setBackgroundColor(color1);
                        break;
                    case 2:
                        viewPager.setBackgroundColor(color2);
                        break;
                    case 3:
                        viewPager.setBackgroundColor(color3);
                        break;
                }

            }
        });
    }

    protected void finishOnboarding(){
        //PreferencesHandler.getSharedPreferences(getApplicationContext()).edit().putBoolean(ONBOARDING_COMPLETE,true).apply();
        Intent main = new Intent(this, BaseMainActivityBottomBar.class);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        startActivity(main);
        finish();
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    @Override
    public void onBackPressed()
    {
        if(viewPager.getCurrentItem()!=0){
            viewPager.setCurrentItem(viewPager.getCurrentItem()-1);
        }
    }
}
