package org.envirocar.app.views.onboarding;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class OBPageAdapter extends FragmentStatePagerAdapter {
    private List<Fragment> tabs = new ArrayList<>();

    public OBPageAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);

        initializeTabs();
    }

    private void initializeTabs() {
        tabs.add(new OnboardingFragment1());
        tabs.add(new OnboardingFragment2());
        tabs.add(new OnboardingFragment3());
        tabs.add(new OnboardingFragment4());
    }

    @Override
    public Fragment getItem(int position) {
        return tabs.get(position);
    }

    @Override
    public int getCount() {
        return tabs.size();
    }

    public void setPageBackVisibility(float alpha, int page) {
        if (tabs.get(page) != null) {
            if (page == 0)
                ((OnboardingFragment1)tabs.get(page)).setBackgroundVisibility(alpha);
            else if (page == 3)
                ((OnboardingFragment4)tabs.get(page)).setBackgroundVisibility(alpha);
        }
    }
}
