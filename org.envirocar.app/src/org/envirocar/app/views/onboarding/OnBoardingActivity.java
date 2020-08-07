package org.envirocar.app.views.onboarding;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.button.MaterialButton;

import org.envirocar.app.R;
import org.envirocar.app.views.BaseMainActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.relex.circleindicator.CircleIndicator;

public class OnBoardingActivity extends AppCompatActivity {

    @BindView(R.id.onboardingViewpager)
    ViewPager viewPager;
    @BindView(R.id.onboardingIndicator)
    CircleIndicator circleIndicator;
    @BindView(R.id.onBoardingButton)
    MaterialButton button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_boarding);
        ButterKnife.bind(this);

        FragmentStatePagerAdapter adapter = new OnBoardingViewPager(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        circleIndicator.setViewPager(viewPager);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if(position == 2) {
                    button.setText("FINISH");
                } else {
                    button.setText("NEXT");
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }

    @OnClick(R.id.onBoardingButton)
    void onBoardingButtonClick() {
        if(viewPager.getCurrentItem()==2) {
            getSharedPreferences("OnBoarding",MODE_PRIVATE).edit().putBoolean("HasSeen",true).commit();
            Intent intent = new Intent(this, BaseMainActivity.class);
            startActivity(intent);
        } else {
            viewPager.setCurrentItem(viewPager.getCurrentItem()+1);
        }
    }

    private static class OnBoardingViewPager extends FragmentStatePagerAdapter {

        OnBoardingFragment onBoardingFragment = new OnBoardingFragment();
        OnBoardingFragmentSecond onBoardingFragmentSecond = new OnBoardingFragmentSecond();
        OnBoardingFragmentThird onBoardingFragmentThird = new OnBoardingFragmentThird();

        Fragment fragments[];
        public OnBoardingViewPager(@NonNull FragmentManager fm) {
            super(fm);
            fragments = new Fragment[] {
                    onBoardingFragment,
                    onBoardingFragmentSecond,
                    onBoardingFragmentThird
            };
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments[position];
        }

        @Override
        public int getCount() {
            return 3;
        }
    }
}