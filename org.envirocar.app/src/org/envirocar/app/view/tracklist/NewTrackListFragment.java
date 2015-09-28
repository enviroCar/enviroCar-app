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
import org.envirocar.app.injection.BaseInjectorFragment;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * @author dewall
 */
public class NewTrackListFragment extends BaseInjectorFragment {


    @InjectView(R.id.fragment_tracklist_layout_tablayout)
    protected TabLayout mTabLayout;
    @InjectView(R.id.fragment_tracklist_layout_viewpager)
    protected ViewPager mViewPager;

    private TrackListPagerAdapter mTrackListPageAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        View content = inflater.inflate(R.layout.fragment_tracklist_layout, container, false);

        ButterKnife.inject(this, content);

        mTrackListPageAdapter = new TrackListPagerAdapter(getFragmentManager());
        mViewPager.setAdapter(mTrackListPageAdapter);
        mTabLayout.setupWithViewPager(mViewPager);

        return content;
    }

    /**
     * @author dewall
     */
    static class TrackListPagerAdapter extends FragmentStatePagerAdapter {

        private static final int NUM_PAGES = 2;

        /**
         * Constructor.
         *
         * @param fm the fragment manager of the application's current scope.
         */
        public TrackListPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return new TrackListLocalCardFragment();
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0) {
                return "Local Tracks";
            } else {
                return "Remote Tracks";
            }
        }
    }
}
