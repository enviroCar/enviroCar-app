package org.envirocar.app.views.trackdetails;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class ViewPagerAdapter extends FragmentPagerAdapter {

    private final Bundle bundle;

    public ViewPagerAdapter(FragmentManager fm, Bundle data) {
        super(fm);
        bundle = data;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                final TrackInfoFragment f1 = new TrackInfoFragment();
                f1.setArguments(this.bundle);
                return f1;
            case 1:
                final TrackStatisticsFragment f2 = new TrackStatisticsFragment();
                f2.setArguments(this.bundle);
                return f2;
        }
        return null;
    }

    @Override
    public int getCount() {
        return 2;
    }
}
