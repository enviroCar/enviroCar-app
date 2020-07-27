package org.envirocar.app.views.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.envirocar.app.R;
import org.envirocar.app.views.others.OthersFragment;

public class ObdHelpFragment extends DialogFragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View contentView = inflater.inflate(R.layout.obd_help_dialog,container,false);
        ViewPager viewPager = contentView.findViewById(R.id.obd_help_viewpager);

        FragmentStatePagerAdapter fragmentStatePagerAdapter;
        fragmentStatePagerAdapter = new OBDPager(getChildFragmentManager());
        viewPager.setAdapter(fragmentStatePagerAdapter);
        return contentView;
    }

    private class OBDPager extends FragmentStatePagerAdapter {

        private Fragment[] fragments;
        ObdContent1Fragment obdContent1Fragment = new ObdContent1Fragment();
        ObdContent2Fragment obdContent2Fragment = new ObdContent2Fragment();
        ObdContent3Fragment obdContent3Fragment = new ObdContent3Fragment();
        public OBDPager(@NonNull FragmentManager fm) {
            super(fm);
            fragments = new Fragment[] {
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
