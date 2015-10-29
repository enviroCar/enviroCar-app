package org.envirocar.app.view.tracklist;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.envirocar.app.R;
import org.envirocar.core.injection.BaseInjectorFragment;
import org.envirocar.core.logging.Logger;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * @author dewall
 */
public class TrackListPagerFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(TrackListPagerFragment.class);

    @InjectView(R.id.fragment_tracklist_layout_tablayout)
    protected TabLayout mTabLayout;
    @InjectView(R.id.fragment_tracklist_layout_viewpager)
    protected ViewPager mViewPager;

    private TrackListPagerAdapter trackListPageAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        LOG.info("onCreateView()");
        View content = inflater.inflate(R.layout.fragment_tracklist_layout, container, false);

        ButterKnife.inject(this, content);

        trackListPageAdapter = new TrackListPagerAdapter(getFragmentManager());
        mViewPager.setAdapter(trackListPageAdapter);
        mTabLayout.setupWithViewPager(mViewPager);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int
                    positionOffsetPixels) {
                // Nothing to do..
            }

            @Override
            public void onPageSelected(int position) {
                LOG.info("Page selected=" + position);
                if (position == 0) {
                    trackListPageAdapter.localCardFragment.loadDataset();
                } else if (position == 1) {
                    trackListPageAdapter.remoteCardFragment.loadDataset();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // Nothing to do..
            }
        });

        mTabLayout.setSelectedTabIndicatorColor(getResources()
                .getColor(R.color.green_dark_cario));
        return content;
    }

    @Override
    public void onResume() {
        LOG.info("onResume()");
        super.onResume();
        if (mViewPager.getCurrentItem() == 0) {
            trackListPageAdapter.localCardFragment.loadDataset();
        } else {
            trackListPageAdapter.remoteCardFragment.loadDataset();
        }
    }

    @Override
    public void onDestroyView() {
        LOG.info("onDestroyView()");
        super.onDestroyView();

        trackListPageAdapter.localCardFragment.onDestroyView();
        trackListPageAdapter.remoteCardFragment.onDestroyView();
    }

    /**
     * @author dewall
     */
    class TrackListPagerAdapter extends FragmentStatePagerAdapter {
        private static final int NUM_PAGES = 2;

        private TrackListLocalCardFragment localCardFragment =
                new TrackListLocalCardFragment();
        private TrackListRemoteCardFragment remoteCardFragment =
                new TrackListRemoteCardFragment();

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

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0) {
                return getString(R.string.track_list_local_tracks);
            } else {
                return getString(R.string.track_list_remote_tracks);
            }
        }
    }
}
