package org.envirocar.app.views.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.envirocar.app.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class ObdHelpFragment extends DialogFragment {
    @BindView(R.id.obd_help_viewpager)
    ViewPager viewPager;
    @BindView(R.id.obd_help_cancel)
    TextView helpCancel;
    @BindView(R.id.obd_help_next)
    TextView helpNext;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View contentView = inflater.inflate(R.layout.obd_help_dialog, container, false);

        ButterKnife.bind(this, contentView);

        FragmentStatePagerAdapter fragmentStatePagerAdapter;
        fragmentStatePagerAdapter = new OBDPager(getChildFragmentManager());
        viewPager.setAdapter(fragmentStatePagerAdapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 2) {
                    helpNext.setVisibility(View.GONE);
                } else {
                    helpNext.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        return contentView;
    }

    @OnClick(R.id.obd_help_next)
    void nextClick() {
        viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
    }

    @OnClick(R.id.obd_help_cancel)
    void cancelClick() {
        dismiss();
    }

    private class OBDPager extends FragmentStatePagerAdapter {

        private Fragment[] fragments;
        ObdContent1Fragment obdContent1Fragment = new ObdContent1Fragment();
        ObdContent2Fragment obdContent2Fragment = new ObdContent2Fragment();
        ObdContent3Fragment obdContent3Fragment = new ObdContent3Fragment();

        public OBDPager(@NonNull FragmentManager fm) {
            super(fm);
            fragments = new Fragment[]{
                    obdContent1Fragment,
                    obdContent2Fragment,
                    obdContent3Fragment
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
