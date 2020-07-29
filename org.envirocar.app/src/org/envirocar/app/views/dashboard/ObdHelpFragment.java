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
import me.relex.circleindicator.CircleIndicator;


public class ObdHelpFragment extends DialogFragment {
    @BindView(R.id.obd_help_viewpager)
    ViewPager viewPager;
    @BindView(R.id.dialogCircleIndicator)
    CircleIndicator circleIndicator;
    @BindView(R.id.contentChange)
    TextView nextText;
    @BindView(R.id.contentChangePrev)
    TextView prevText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View contentView = inflater.inflate(R.layout.obd_help_dialog, container, false);

        ButterKnife.bind(this, contentView);


        FragmentStatePagerAdapter fragmentStatePagerAdapter;
        fragmentStatePagerAdapter = new OBDPager(getChildFragmentManager());
        viewPager.setAdapter(fragmentStatePagerAdapter);
        circleIndicator.setViewPager(viewPager);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 2) {
                    nextText.setText("FINISH");
                } else {
                    nextText.setText("NEXT");
                }

                if (position == 0) {
                    prevText.setVisibility(View.GONE);
                } else {
                    prevText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        return contentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @OnClick(R.id.obdDialogClose)
    void closeDialog() {
        dismiss();
    }

    @OnClick(R.id.contentChange)
    void changeContent() {
        if (viewPager.getCurrentItem() != 2) {
            viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
        } else {
            dismiss();
        }
    }

    @OnClick(R.id.contentChangePrev)
    void prevContent() {
        viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
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
