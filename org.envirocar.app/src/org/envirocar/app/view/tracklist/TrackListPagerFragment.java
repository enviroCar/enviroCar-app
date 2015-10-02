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

import com.google.common.collect.Lists;

import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.storage.RemoteTrack;
import org.envirocar.app.storage.Track;

import java.util.Collections;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * @author dewall
 */
public class TrackListPagerFragment extends BaseInjectorFragment {

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

        mTabLayout.setSelectedTabIndicatorColor(getResources().getColor(R.color.green_dark_cario));
        return content;
    }

    /**
     * @author dewall
     */
    class TrackListPagerAdapter extends FragmentStatePagerAdapter {

        private static final int NUM_PAGES = 2;

        private final List<RemoteTrack> mRemoteTrackList = Collections.synchronizedList(Lists
                .newArrayList());
        private final List<Track> mLocalTrackList = Collections.synchronizedList(Lists
                .newArrayList());

        private TrackListLocalCardFragment localCardFragment = new TrackListLocalCardFragment();
        private TrackListRemoteCardFragment remoteCardFragment = new TrackListRemoteCardFragment();

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
                return new TrackListLocalCardFragment();
            } else {
                return new TrackListRemoteCardFragment();
            }
        }


        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0) {
                return "Local";
            } else {
                return "Uploaded";
            }
        }
    }
}
