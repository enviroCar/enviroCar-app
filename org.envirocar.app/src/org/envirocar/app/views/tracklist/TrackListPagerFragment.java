/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.tracklist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.envirocar.app.BaseApplication;
import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.injection.modules.MainActivityModule;
import org.envirocar.core.logging.Logger;

import butterknife.BindView;
import butterknife.ButterKnife;
import info.hoang8f.android.segmented.SegmentedGroup;

/**
 * @author dewall
 */
public class TrackListPagerFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(TrackListPagerFragment.class);

    @BindView(R.id.trackListSegmentedGroup)
    protected SegmentedGroup trackListSegmentedGroup;
    @BindView(R.id.fragment_tracklist_layout_viewpager)
    protected ViewPager mViewPager;

    private TrackListPagerAdapter trackListPageAdapter;

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        BaseApplication.get(getActivity())
                .getBaseApplicationComponent()
                .plus(new MainActivityModule(getActivity()))
                .inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LOG.info("onCreateView()");
        View content = inflater.inflate(R.layout.fragment_tracklist_layout, container, false);
        ButterKnife.bind(this, content);

        trackListPageAdapter = new TrackListPagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(trackListPageAdapter);
        mViewPager.setSaveFromParentEnabled(false);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    trackListSegmentedGroup.check(R.id.localSegmentedButton);
                } else {
                    trackListSegmentedGroup.check(R.id.uploadedSegmentedButton);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        trackListSegmentedGroup.check(R.id.localSegmentedButton);

        trackListSegmentedGroup.setOnCheckedChangeListener((radioGroup, i) -> {
            switch (i) {
                case R.id.localSegmentedButton:
                    mViewPager.setCurrentItem(0);
                    break;
                case R.id.uploadedSegmentedButton:
                    mViewPager.setCurrentItem(1);
                    break;
                default:
                    break;
            }
        });

        return content;
    }

    @Override
    public void onResume() {
        LOG.info("onResume()");
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        LOG.info("onDestroyView()");
        super.onDestroyView();
    }


    /**
     * @author dewall
     */
    class TrackListPagerAdapter extends FragmentStatePagerAdapter {
        private static final int NUM_PAGES = 2;

        private TrackListLocalCardFragment localCardFragment;
        private TrackListRemoteCardFragment remoteCardFragment;

        /**
         * Constructor.
         *
         * @param fm the fragment manager of the application's current scope.
         */
        public TrackListPagerAdapter(FragmentManager fm) {
            super(fm);

            remoteCardFragment = new TrackListRemoteCardFragment();
            localCardFragment = new TrackListLocalCardFragment();
            localCardFragment.setOnTrackUploadedListener(remoteCardFragment);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return localCardFragment;
            } else {
                return remoteCardFragment;
            }
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

    }
}
